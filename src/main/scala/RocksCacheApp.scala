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

case class Key(key: String)

object RocksCacheApp extends App with Endpoint.Module[IO] {

  RocksDB.loadLibrary()

  val options = new Options().setCreateIfMissing(true)
  val db = RocksDB.open(options, "/Users/igrbavac/Documents/Development/scala/rocks-cache/temp/rocksdb")

  val sayHello: Endpoint[IO, String] = get("hello") {
    Ok("Hello, World!")
  }

  final val postKey: Endpoint[IO, Key] = post("keys" :: jsonBody[Key]) { t: Key =>
    db.put(t.key.getBytes, Array[Byte]())
    println(t)
    Ok(t)
  }

  final val getKey: Endpoint[IO, Key] = get("keys" :: path[String]) { key: String =>
    val value = db.get(key.getBytes)
    value match {
      case null => NotFound(new Exception("Key not fount"))
      case _ => Ok(Key(key))
    }
  }

  val service: Service[Request, Response] = Bootstrap
    .serve[Application.Json](sayHello :+: postKey :+: getKey).toService

  Await.ready(Http.server.serve(":8080", service))
}
