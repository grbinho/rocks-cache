package org.rockscache

import cats.effect.IO
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import io.circe.generic.auto._
import io.finch._
import io.finch.circe._
import org.rockscache.statistics.DbStatistics
import org.rocksdb.HistogramType
import org.rockscache.statistics._

class RestService(cacheDb: CacheStoreDatabase) extends Endpoint.Module[IO] {
  final val getStats: Endpoint[IO, DbStatistics] = get("stats") {
    val dbWrites = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.DB_WRITE))
    val dbGet = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.DB_GET))
    val dbMultiGet = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.DB_MULTIGET))
    val dbSeek = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.DB_SEEK))
    val bytesCompressed = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.BYTES_COMPRESSED))
    val bytesDecompressed = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.BYTES_DECOMPRESSED))
    val bytesPerWrite = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.BYTES_PER_WRITE))
    val bytesPerRead = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.BYTES_PER_READ))
    val bytesPerMultiGet = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.BYTES_PER_MULTIGET))
    val compactionOutFileSyncMicros = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.COMPACTION_OUTFILE_SYNC_MICROS))
    val compactionTime = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.COMPACTION_TIME))
    val numFilesInSingleCompaction = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.NUM_FILES_IN_SINGLE_COMPACTION))
    val compressionTimesNanos = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.COMPRESSION_TIMES_NANOS))
    val decompressionTimesNanos = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.DECOMPRESSION_TIMES_NANOS))
    val hardRateLimitDelayCount = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.HARD_RATE_LIMIT_DELAY_COUNT))
    val manifestFileSyncMicros = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.MANIFEST_FILE_SYNC_MICROS))
    val numSubcompactionsScheduled = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.NUM_SUBCOMPACTIONS_SCHEDULED))
    val histogramEnumMax = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.HISTOGRAM_ENUM_MAX))
    val readBlockCompactionMicros = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.READ_BLOCK_COMPACTION_MICROS))
    val readBlockGetMicros = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.READ_BLOCK_GET_MICROS))
    val readNumMergeOperands = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.READ_NUM_MERGE_OPERANDS))
    val softRateLimitDelayCount = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.SOFT_RATE_LIMIT_DELAY_COUNT))
    val sstReadMicros = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.SST_READ_MICROS))
    val stallL0NumFilesCount = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.STALL_L0_NUM_FILES_COUNT))
    val stallL0SlowdownCount = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.STALL_L0_SLOWDOWN_COUNT))
    val stallMemtableCompactionCount = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.STALL_MEMTABLE_COMPACTION_COUNT))
    val subcompactionSetupTime = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.SUBCOMPACTION_SETUP_TIME))
    val tableOpenIOMicros = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.TABLE_OPEN_IO_MICROS))
    val tableSyncMicros = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.TABLE_SYNC_MICROS))
    val walFileSyncMicros = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.WAL_FILE_SYNC_MICROS))
    val writeRawBlockMicros = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.WRITE_RAW_BLOCK_MICROS))
    val writeStall = HistogramData(cacheDb.statisticsObject.getHistogramData(HistogramType.WRITE_STALL))

    val stats = DbStatistics(
      dbWrites,
      dbGet,
      dbMultiGet,
      dbSeek,
      bytesCompressed,
      bytesDecompressed,
      bytesPerWrite,
      bytesPerRead,
      bytesPerMultiGet,
      compactionOutFileSyncMicros,
      compactionTime,
      numFilesInSingleCompaction,
      compressionTimesNanos,
      decompressionTimesNanos,
      hardRateLimitDelayCount,
      manifestFileSyncMicros,
      numSubcompactionsScheduled,
      histogramEnumMax,
      readBlockCompactionMicros,
      readBlockGetMicros,
      readNumMergeOperands,
      softRateLimitDelayCount,
      sstReadMicros,
      stallL0NumFilesCount,
      stallL0SlowdownCount,
      stallMemtableCompactionCount,
      subcompactionSetupTime,
      tableOpenIOMicros,
      tableSyncMicros,
      walFileSyncMicros,
      writeRawBlockMicros,
      writeStall)

    Ok(stats)
  }

  final val httpService: Service[Request, Response] =
    Bootstrap.serve[Application.Json](getStats).toService
}
