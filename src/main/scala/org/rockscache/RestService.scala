package org.rockscache

import cats.effect.IO
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import io.circe.generic.auto._
import io.finch._
import io.finch.circe._
import org.rockscache.statistics._

class RestService(cacheDb: CacheStoreDatabase) extends Endpoint.Module[IO] {
  val statistics = new DbStatistics(cacheDb.statisticsObject)

  final val getHistograms: Endpoint[IO, Histograms] = get("histograms") {
    Ok(statistics.getHistograms())
  }

  final val getMetrics: Endpoint[IO, Map[String, String]] = get("available-metrics") {
    //Returns a list of available metrics and their description
    val availableMetrics = Metric.all
    Ok(availableMetrics.map(m => (m.name -> m.description)).toMap)
  }

  final val getMetricsValues
    : Endpoint[IO, Seq[MetricValue]] =
    get("metrics" :: params[String]("names")) {
      names: Seq[String] =>
        Ok(names.map(n => MetricValue(Metric(n), 0.0d)))
    }

  final val httpService: Service[Request, Response] =
    Bootstrap
      .serve[Application.Json](
        getHistograms :+: getMetrics :+: getMetricsValues)
      .toService
}
