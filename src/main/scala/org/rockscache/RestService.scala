package org.rockscache

import cats.effect.IO
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import io.circe.generic.auto._
import io.finch._
import io.finch.circe._
import org.rocksdb.HistogramType

class RestService(cacheDb: CacheStoreDatabase) extends Endpoint.Module[IO] {
  final val getStats: Endpoint[IO, String] = get("stats") {
    val dbGetHistogram = cacheDb.statisticsObject.getHistogramString(HistogramType.DB_GET)
    Ok(dbGetHistogram)
  }

  final val httpService: Service[Request, Response] =
    Bootstrap.serve[Application.Json](getStats).toService
}
