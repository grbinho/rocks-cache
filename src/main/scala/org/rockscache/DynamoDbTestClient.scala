package org.rockscache

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.{OffsetDateTime, ZoneOffset}
import java.util.Base64
import java.util.concurrent.Executors

import org.rockscache.avro.proto.{KeyValuePair, KeyValuePairBatchResponse}
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.services.dynamodb._
import software.amazon.awssdk.services.dynamodb.model._

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Random, Success, Try}

object DynamoDbTestClient extends App {

  lazy val dynamoDbClient = DynamoDbClient
    .builder
    .credentialsProvider(EnvironmentVariableCredentialsProvider.create)
    .region(software.amazon.awssdk.regions.Region.EU_WEST_1)
    .build

  case class RunResults(requestCount: Long, itemsCount: Long, errorCount: Long, duplicateCount: Long, totalDuration: Long)



  def checkAndStoreBatch(tableName: String, batch: Seq[KeyValuePair]) = {
    //Check if item is already in dynamoDb
    //If it's not write it and return true
    //If it is, return false

    //Max batch size is 100. Split incoming batch into batches of 100
    val dynamoBatches = batch.grouped(100).toList

    val batchResponses = dynamoBatches.map { b =>
      val batch = b.map(kv => (StandardCharsets.UTF_8.decode(Base64.getEncoder.encode(kv.getKey())).toString -> kv.getValue)).toMap
      val timestamp = java.time.Instant.now.toEpochMilli
      val data = KeysAndAttributes.builder.keys(batch.map(kv => Map("Key"-> AttributeValue.builder.s(kv._1).build).asJava).asJavaCollection).build
      val batchData = Map(tableName -> data).asJava
      val batchRequest = BatchGetItemRequest.builder.requestItems(batchData).build
      val response = dynamoDbClient.batchGetItem(batchRequest)
      val responseData = response.responses.get(tableName)
      val responseKeys = responseData.asScala.map(r => r.values.asScala.map(v => v.s)).flatten
      //Translate b -> key -> true/false. False if item was found, meaning write was not done for others, do a write
      val keysToWrite = batch.map(kv => (kv._1, !responseKeys.exists(rk => rk == kv._1)))
      keysToWrite
    }.reduceLeft(_ ++ _)

    //For each true, do a write
    val keysToWrite = batchResponses.filter(kv => kv._2 == true).grouped(25).toList

    keysToWrite.foreach{ k =>
      val wr = k.map(kv => WriteRequest.builder.putRequest(PutRequest.builder.item(Map("Key" -> AttributeValue.builder.s(kv._1).build).asJava).build).build).asJavaCollection
      val batchWriteRequest = BatchWriteItemRequest.builder.requestItems(Map(tableName -> wr).asJava).build
      val writeResponse = dynamoDbClient.batchWriteItem(batchWriteRequest)
    }

    val response = new KeyValuePairBatchResponse(batchResponses.map(b => Boolean.box(b._2)).toList.asJava)
    response
  }


  private def runTest(tableName: String, runLength: Int, batchSize: Int): RunResults = {
    val runId = Random.nextLong()

    println(s"[RunId=${runId}] Running for ${runLength} minutes against ${tableName}")

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
        checkAndStoreBatch(tableName, keyValuePairBatch)
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

    if(args.length < 4)
      println("Usage:\n <tableName> <runLength:minutes> <batchSize> <numberOfClients>")
    else {
      val tableName = args(0)
      val runLength = Integer.parseInt(args(1)) //minutes
      val batchSize = Integer.parseInt(args(2))
      val numberOfClients = Integer.parseInt(args(3))

      val fixedThreadPool = Executors.newFixedThreadPool(numberOfClients)
      implicit val ec = ExecutionContext.fromExecutor(fixedThreadPool)

      val tasks = for {
        i <- 1 to numberOfClients
      } yield Future {
        runTest(tableName, runLength, batchSize)
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

      dynamoDbClient.close()
      fixedThreadPool.shutdown()
    }
  }
}
