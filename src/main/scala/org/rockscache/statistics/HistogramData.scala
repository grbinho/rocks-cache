package org.rockscache.statistics

case class HistogramData(median: Double, percentile95: Double, percentile99: Double, average: Double, standardDeviation: Double)

object HistogramData {
  def apply(data: org.rocksdb.HistogramData): HistogramData = {
    HistogramData(data.getMedian, data.getPercentile95, data.getPercentile99, data.getAverage, data.getStandardDeviation)
  }
}