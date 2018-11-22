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
import org.rocksdb.RocksDB
import org.rocksdb.Options

case class KeyValue(key: String, value: String)

object RocksCacheApp extends App with Endpoint.Module[IO] {

  RocksDB.loadLibrary()

  private def getBytesUTF8(value: String): Array[Byte] = StandardCharsets.UTF_8.encode(value).array
  private def getStringUTF8(value: Array[Byte]) = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(value)).toString

  val options = new Options().setCreateIfMissing(true)
  val db = RocksDB.open(options, "/Users/igrbavac/Documents/Development/scala/rocks-cache/temp/rocksdb")

  val sayHello: Endpoint[IO, String] = get("hello") {
    Ok("Hello, World!")
  }

  final val postKey: Endpoint[IO, KeyValue] = post("keys" :: jsonBody[KeyValue]) { t: KeyValue =>
    db.put(getBytesUTF8(t.key), getBytesUTF8(t.value))
    println(t)
    Ok(t)
  }

  final val getValue: Endpoint[IO, String] = get("keys" :: path[String]) { key: String =>
    val value = db.get(getBytesUTF8(key))
    value match {
      case null => NotFound(new Exception("Key not found"))
      case _ => Ok(getStringUTF8(value))
    }
  }

  val service: Service[Request, Response] = Bootstrap
    .serve[Application.Json](sayHello :+: postKey :+: getValue).toService

  Await.ready(Http.server.serve(":8080", service))
}
