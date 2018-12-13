package org.rockscache

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.sql.Timestamp
import java.time.{OffsetDateTime, ZoneOffset}
import java.util.Base64
import java.util.concurrent.Executors

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.rockscache.avro.proto.{KeyValuePair, KeyValuePairBatchResponse}
import org.rockscache.utils.SqlDataAccess._

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Random, Success, Try}


object PostgresTestClient extends App {

  case class RunResults(requestCount: Long, itemsCount: Long, errorCount: Long, duplicateCount: Long, totalDuration: Long)

  private def checkAndStoreBatch(b: Seq[KeyValuePair])(implicit dataSource: HikariDataSource): KeyValuePairBatchResponse = {

    val batch = b.map(kv => (StandardCharsets.UTF_8.decode(Base64.getEncoder.encode(kv.getKey())).toString -> kv.getValue)).toMap

    var existingKeys = Seq.empty[String]
    usingConnection { implicit connection =>

      // First try to write the full batch.
      val insertQueryTemplate = "INSERT INTO public.dedupstore (key, value, \"timestamp\") VALUES (?,?,?)"
      val insertStatementTry = connection.prepareStatement(insertQueryTemplate)
      batch.map { kv =>
        insertStatementTry.setString(1, kv._1)
        insertStatementTry.setBytes(2, kv._2.array)
        insertStatementTry.setTimestamp(3, new Timestamp(java.time.Instant.now().toEpochMilli))
        insertStatementTry.addBatch()
      }

      Try {
        insertStatementTry.executeBatch()
        insertStatementTry.close()
      } match {
        case Failure(t) =>
          // If it fails, check and then insert
          // Check which keys exist
          val inClause = (for (_ <- 0 until batch.size) yield "?").mkString(",")
          withStatement(
            s"""
               |SELECT key FROM public.dedupstore WHERE key IN (${inClause})
        """.stripMargin) { selectStatement =>
            batch.keys.zipWithIndex.foreach(ik => selectStatement.setString(ik._2 + 1, ik._1))
            val results = selectStatement.executeQuery()

            while (results.next()) {
              val key = results.getString("key")
              existingKeys = existingKeys :+ key
            }
          }

          val shouldWriteKeys = batch.map(bi => (bi -> !existingKeys.exists(k => k == bi._1)))


          val insertStatement = connection.prepareStatement(insertQueryTemplate)
          shouldWriteKeys.filter(_._2).map { kv =>
            val kvp = kv._1
            insertStatement.setString(1, kvp._1)
            insertStatement.setBytes(2, kvp._2.array)
            insertStatement.setTimestamp(3, new Timestamp(java.time.Instant.now().toEpochMilli))
            insertStatement.addBatch()
          }

          insertStatement.executeBatch()
          insertStatement.close()

          new KeyValuePairBatchResponse(shouldWriteKeys.map(b => Boolean.box(b._2)).toList.asJava)
        case Success(_) =>
          new KeyValuePairBatchResponse(batch.map(_ => Boolean.box(true)).toList.asJava)
      }
    }
  }

  private def runTest(host: String, runLength: Int, batchSize: Int)(implicit dataSource: HikariDataSource): RunResults = {
    val runId = Random.nextLong()

    println(s"[RunId=${runId}] Running for ${runLength} minutes against ${host}")

    val endTime = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(runLength)
    var requestCount = 0L
    var errorCount = 0L
    var duplicateCount = 0L

    val start = System.currentTimeMillis()

    while (OffsetDateTime.now(ZoneOffset.UTC).isBefore(endTime)) {

      val keyValuePairBatch = for (i <- 1 to batchSize) yield {
        val r = Random.nextLong.toString
        val kv = new KeyValuePair()
        val key = MessageDigest.getInstance("SHA-256").digest(r.getBytes)
        kv.setKey(ByteBuffer.wrap(key))
        kv.setValue(StandardCharsets.UTF_8.encode(r))
        kv
      }

      val response = Try {
        checkAndStoreBatch(keyValuePairBatch)
      }

      response match {
        case Success(result) => result.getPayload.asScala.filter(_ == false).foreach(_ => duplicateCount += 1)
        case Failure(_) => errorCount += 1
      }

      requestCount += 1
    }

    val end = System.currentTimeMillis()


    RunResults(requestCount, requestCount * batchSize, errorCount, duplicateCount, end - start)
  }

  override def main(args: Array[String]) = {

    classOf[org.postgresql.Driver]

    if(args.length < 4)
      println("Usage:\n <host> <runLength:minutes> <batchSize> <numberOfClients>")
    else {
      val host = args(0)
      val runLength = Integer.parseInt(args(1)) //minutes
      val batchSize = Integer.parseInt(args(2))
      val numberOfClients = Integer.parseInt(args(3))

      val hikariConfig = new HikariConfig
      hikariConfig.setJdbcUrl(s"jdbc:postgresql://${host}/pgcache")
      hikariConfig.setUsername("pgcache")
      hikariConfig.setPassword("sU8jCN3yjfe3SwHD3XqkDktd8NAeJ2d9chnk")
      hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
      hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
      hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

      implicit val dataSource = new HikariDataSource(hikariConfig)

      val fixedThreadPool = Executors.newFixedThreadPool(numberOfClients)
      implicit val ec = ExecutionContext.fromExecutor(fixedThreadPool)

      val tasks = for {
        i <- 1 to numberOfClients
      } yield Future {
        runTest(host, runLength, batchSize)
      }

      val aggregated = Future.sequence(tasks)

      val results = Await.result(aggregated, Duration(runLength + 1, MINUTES))

      val maxDuration = results.map(_.totalDuration).max
      val totalDuration = results.map(_.totalDuration).sum
      val totalRequestsCount = results.map(_.requestCount).sum
      val totalRecordCount = totalRequestsCount * batchSize
      val errorCount = results.map(_.errorCount).sum
      val duplicateCount = results.map(_.duplicateCount).sum

      println(s"RunTime: ${runLength} min")
      println(s"Number of clients: ${numberOfClients}")
      println(s"Total requests: ${totalRequestsCount}")
      println(s"Total records: ${totalRecordCount}")
      println(s"Total errors: ${errorCount}")
      println(s"Total duplicates: ${duplicateCount}")
      println(s"Ratio of duplicates: ${duplicateCount / totalRecordCount.toDouble}")
      println(s"Duration: ${maxDuration} ms")
      println(s"Avg request duration: ${ totalDuration / totalRequestsCount.toDouble} ms")
      println(s"Batch size: ${batchSize}")
      println(s"Records/sec: ${(totalRecordCount) / (maxDuration/1000d)}")

      dataSource.close()
      fixedThreadPool.shutdown()
    }
  }
}
