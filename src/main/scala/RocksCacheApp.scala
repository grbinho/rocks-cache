import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import io.finch._
import cats.effect.IO
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Http, Service}
import com.twitter.util.Await
import io.circe.generic.auto._
import io.finch._
import io.finch.circe._
import org.rocksdb.{Options, RocksDB, TtlDB}

/*
* If we use it both locally and remote, we can use it as a local cache and remote store???
*
* Assumption is that we are running in a Container or some other volatile compute service.
* To have persistence, we need remote service.
* Another way could be by attaching volumes.
*
* TTL in key somehow??
*
* Pluggable compaction filters (TimeToLive) -> Could already exist in RocksDB. Check it out
*
* Primary use case is going to have a lot of reads and a lot of cache misses!
* Meaning checking a lot of files (almost all)
* Very unlikely that data will be cached on read part. Only during write.
* Maybe good optimization is to immediately after write do the read on server to populate read cache
* Sharding with consistent hashing?
*
* TTL -> Start with generic delete after x days
* Redis can cover most of these features, but does it keep everything in memory? No matter the age
* We would like to evict old records to disk. Hopefully that is something that is automatically managed by RocksDB
*
* TTL available also in RocksDB
*
* Use some arbitrary value for values that will end up being dictionary compressed. Keys are what we want in dedup use case
*
* MultiGet could be useful when we need to check keys for the whole package
*
* Column family per data source? vs multiple database instances?
*
* "The FIFOStyle Compaction drops oldest file when obsolete and can be used for cache-like data."
* */


case class KeyValue(key: String, value: String)

object RocksCacheApp extends App with Endpoint.Module[IO] {

  RocksDB.loadLibrary()

  private def getBytesUTF8(value: String): Array[Byte] = StandardCharsets.UTF_8.encode(value).array
  private def getStringUTF8(value: Array[Byte]) = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(value)).toString



  val options = new Options().setCreateIfMissing(true)
  val db = TtlDB.open(options, "/Users/igrbavac/Documents/Development/scala/rocks-cache/temp/rocksdb")

  val sayHello: Endpoint[IO, String] = get("hello") {
    Ok("Hello, World!")
  }

  final val postValue: Endpoint[IO, KeyValue] = post("values" :: jsonBody[KeyValue]) { t: KeyValue =>
    db.put(getBytesUTF8(t.key), getBytesUTF8(t.value))
    println(t)
    Ok(t)
  }

  final val getValue: Endpoint[IO, String] = get("values" :: path[String]) { key: String =>
    val value = db.get(getBytesUTF8(key))
    value match {
      case null => NotFound(new Exception("Key not found"))
      case _ => Ok(getStringUTF8(value))
    }
  }

  //final val getMultiValues: Endpoint[IO, String]

  val service: Service[Request, Response] = Bootstrap
    .serve[Application.Json](sayHello :+: postValue :+: getValue).toService

  Await.ready(Http.server.serve(":8080", service))
}
