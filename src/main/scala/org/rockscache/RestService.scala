package org.rockscache

import cats.effect.IO
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import io.circe.generic.auto._
import io.finch._
import io.finch.circe._
import org.rocksdb.HistogramType

case class HistogramData(median: Double, percentile95: Double, percentile99: Double, average: Double, standardDeviation: Double)
object HistogramData {
  def apply(data: org.rocksdb.HistogramData): HistogramData = {
    HistogramData(data.getMedian, data.getPercentile95, data.getPercentile99, data.getAverage, data.getStandardDeviation)
  }
}

case class DbStatistics(dbWrite: HistogramData, dbGet: HistogramData)

class RestService(cacheDb: CacheStoreDatabase) extends Endpoint.Module[IO] {
  final val getStats: Endpoint[IO, DbStatistics] = get("stats") {
    val writes = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.DB_WRITE))
    val reads = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.DB_GET))
    val stats = DbStatistics(writes, reads)
    Ok(stats)
  }

  final val httpService: Service[Request, Response] =
    Bootstrap.serve[Application.Json](getStats).toService
}
