package org.rockscache

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.{OffsetDateTime, ZoneOffset}
import java.util.concurrent.Executors

import org.apache.avro.ipc.specific.SpecificRequestor
import org.rockscache.avro.proto.{CacheStore, KeyValuePair}

import scala.util.{Failure, Random, Success, Try}
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

object RocksCacheTestClient extends App {

  case class KeyValue(key: String, value: String)

  case class RunResults(requestCount: Long, itemsCount: Long, errorCount: Long, duplicateCount: Long, totalDuration: Long)

  private def runTest(host: String, runLength: Int, batchSize: Int): RunResults = {
    val runId = Random.nextLong()

    import java.net.InetSocketAddress

    import org.apache.avro.ipc.NettyTransceiver
    val client = new NettyTransceiver(new InetSocketAddress(host, 65111))
    val cacheStoreProxy = SpecificRequestor.getClient[CacheStore](classOf[CacheStore], client)

    println(s"[RunId=${runId}] Running for ${runLength} minutes against ${host}")

    val endTime = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(runLength)
    var requestCount = 0L
    var errorCount = 0L
    var duplicateCount = 0L

    val start = System.currentTimeMillis()

    while (OffsetDateTime.now(ZoneOffset.UTC).isBefore(endTime)) {

      val keyValuePairBatch = for (i <- 1 to batchSize) yield {
        val r = Random.nextInt().toString
        val kv = new KeyValuePair()
        kv.setKey(ByteBuffer.wrap(r.getBytes))
        kv.setValue(StandardCharsets.UTF_8.encode("x"))
        kv
      }

      val response = Try {
        cacheStoreProxy.checkAndStoreBatch(keyValuePairBatch.toList.asJava)
      }

      response match {
        case Success(result) => result.getPayload.asScala.filter(_ == false).foreach(_ => duplicateCount += 1)
        case Failure(_) => errorCount += 1
      }

      requestCount += 1
    }

    val end = System.currentTimeMillis()

    client.close(true)

    RunResults(requestCount, requestCount * batchSize, errorCount, duplicateCount, end - start)
  }

  override def main(args: Array[String]) = {

    if(args.length < 4)
      println("Usage:\n <host> <runLength:minutes> <batchSize> <numberOfClients>")
    else {
      val host = args(0)
      val runLength = Integer.parseInt(args(1)) //minutes
      val batchSize = Integer.parseInt(args(2))
      val numberOfClients = Integer.parseInt(args(3))

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

      fixedThreadPool.shutdown()
    }
  }
}
