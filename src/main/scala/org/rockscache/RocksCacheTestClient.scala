package org.rockscache

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.{OffsetDateTime, ZoneOffset}

import org.apache.avro.ipc.specific.SpecificRequestor
import org.rockscache.avro.proto.{CacheStore, KeyValuePair}

import scala.util.{Failure, Random, Success, Try}

object RocksCacheTestClient extends App {

  case class KeyValue(key: String, value:String)

  override def main(args: Array[String]) = {
    val host = args(0)
    val runLength = Integer.parseInt(args(1)) //minutes
    val runId = Random.nextLong()

    import java.net.InetSocketAddress

    import org.apache.avro.ipc.NettyTransceiver
    val client = new NettyTransceiver(new InetSocketAddress(host,65111))
    val cacheStoreProxy = SpecificRequestor.getClient[CacheStore](classOf[CacheStore], client)

    println(s"[RunId=${runId}] Running for ${runLength} minutes against ${host}")

    val endTime = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(runLength)
    var requestsSent = 0
    var errorCount = 0
    var duplicateCount = 0

    val start = System.currentTimeMillis()

    while(OffsetDateTime.now(ZoneOffset.UTC).isBefore(endTime)) {
      val r = Random.nextInt().toString
      val keyValuePair = new KeyValuePair()
      keyValuePair.setKey(ByteBuffer.wrap(r.getBytes))
      keyValuePair.setValue(StandardCharsets.UTF_8.encode("x"))

      val response = Try { cacheStoreProxy.checkAndStore(keyValuePair) }

      response match {
        case Success(false) => duplicateCount += 1
        case Success(true) => Unit
        case Failure(_) => errorCount += 1
      }

      requestsSent += 1
    }

    val end = System.currentTimeMillis()

    println(s"[RunId=${runId}] Total requests: ${requestsSent}")
    println(s"[RunId=${runId}] Total errors: ${errorCount}")
    println(s"[RunId=${runId}] Ratio of duplicates: ${duplicateCount/requestsSent.toDouble}")
    println(s"[RunId=${runId}] Total duration: ${end - start} ms")
    println(s"[RunId=${runId}] Avg request duration: ${(end - start)/requestsSent.toDouble}")
  }
}
