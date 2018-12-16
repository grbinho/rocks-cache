package org.rockscache.statistics

import org.rocksdb.{HistogramType, Statistics, StatsLevel, TickerType}




class DbStatistics(statistics: Statistics) {
  def getHistograms() : Histograms = {
    val dbWrites = HistogramData(statistics.getHistogramData(HistogramType.DB_WRITE))
    val dbGet = HistogramData(statistics.getHistogramData(HistogramType.DB_GET))
    val dbMultiGet = HistogramData(statistics.getHistogramData(HistogramType.DB_MULTIGET))
    val dbSeek = HistogramData(statistics.getHistogramData(HistogramType.DB_SEEK))
    val bytesCompressed = HistogramData(statistics.getHistogramData(HistogramType.BYTES_COMPRESSED))
    val bytesDecompressed = HistogramData(statistics.getHistogramData(HistogramType.BYTES_DECOMPRESSED))
    val bytesPerWrite = HistogramData(statistics.getHistogramData(HistogramType.BYTES_PER_WRITE))
    val bytesPerRead = HistogramData(statistics.getHistogramData(HistogramType.BYTES_PER_READ))
    val bytesPerMultiGet = HistogramData(statistics.getHistogramData(HistogramType.BYTES_PER_MULTIGET))
    val compactionOutFileSyncMicros = HistogramData(statistics.getHistogramData(HistogramType.COMPACTION_OUTFILE_SYNC_MICROS))
    val compactionTime = HistogramData(statistics.getHistogramData(HistogramType.COMPACTION_TIME))
    val numFilesInSingleCompaction = HistogramData(statistics.getHistogramData(HistogramType.NUM_FILES_IN_SINGLE_COMPACTION))
    val compressionTimesNanos = HistogramData(statistics.getHistogramData(HistogramType.COMPRESSION_TIMES_NANOS))
    val decompressionTimesNanos = HistogramData(statistics.getHistogramData(HistogramType.DECOMPRESSION_TIMES_NANOS))
    val hardRateLimitDelayCount = HistogramData(statistics.getHistogramData(HistogramType.HARD_RATE_LIMIT_DELAY_COUNT))
    val manifestFileSyncMicros = HistogramData(statistics.getHistogramData(HistogramType.MANIFEST_FILE_SYNC_MICROS))
    val numSubcompactionsScheduled = HistogramData(statistics.getHistogramData(HistogramType.NUM_SUBCOMPACTIONS_SCHEDULED))
    val histogramEnumMax = HistogramData(statistics.getHistogramData(HistogramType.HISTOGRAM_ENUM_MAX))
    val readBlockCompactionMicros = HistogramData(statistics.getHistogramData(HistogramType.READ_BLOCK_COMPACTION_MICROS))
    val readBlockGetMicros = HistogramData(statistics.getHistogramData(HistogramType.READ_BLOCK_GET_MICROS))
    val readNumMergeOperands = HistogramData(statistics.getHistogramData(HistogramType.READ_NUM_MERGE_OPERANDS))
    val softRateLimitDelayCount = HistogramData(statistics.getHistogramData(HistogramType.SOFT_RATE_LIMIT_DELAY_COUNT))
    val sstReadMicros = HistogramData(statistics.getHistogramData(HistogramType.SST_READ_MICROS))
    val stallL0NumFilesCount = HistogramData(statistics.getHistogramData(HistogramType.STALL_L0_NUM_FILES_COUNT))
    val stallL0SlowdownCount = HistogramData(statistics.getHistogramData(HistogramType.STALL_L0_SLOWDOWN_COUNT))
    val stallMemtableCompactionCount = HistogramData(statistics.getHistogramData(HistogramType.STALL_MEMTABLE_COMPACTION_COUNT))
    val subcompactionSetupTime = HistogramData(statistics.getHistogramData(HistogramType.SUBCOMPACTION_SETUP_TIME))
    val tableOpenIOMicros = HistogramData(statistics.getHistogramData(HistogramType.TABLE_OPEN_IO_MICROS))
    val tableSyncMicros = HistogramData(statistics.getHistogramData(HistogramType.TABLE_SYNC_MICROS))
    val walFileSyncMicros = HistogramData(statistics.getHistogramData(HistogramType.WAL_FILE_SYNC_MICROS))
    val writeRawBlockMicros = HistogramData(statistics.getHistogramData(HistogramType.WRITE_RAW_BLOCK_MICROS))
    val writeStall = HistogramData(statistics.getHistogramData(HistogramType.WRITE_STALL))

    Histograms(
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
  }

  def getMetricValue(metric: Metric) = ???
}

case class Histograms(dbWrite: HistogramData,
                        dbGet: HistogramData,
                        dbMultiGet: HistogramData,
                        dbSeek: HistogramData,
                        bytesCompressed: HistogramData,
                        bytesDecompressed: HistogramData,
                        bytesPerWrite: HistogramData,
                        bytesPerRead: HistogramData,
                        bytesPerMultiGet: HistogramData,
                        compactionOutFileSyncMicros: HistogramData,
                        compactionTime: HistogramData,
                        numFilesInSingleCompaction: HistogramData,
                        compressionTimesNanos: HistogramData,
                        decompressionTimesNanos: HistogramData,
                        hardRateLimitDelayCount: HistogramData,
                        manifestFileSyncMicros: HistogramData,
                        numSubcompactionsScheduled: HistogramData,
                        histogramEnumMax: HistogramData,
                        readBlockCompactionMicros: HistogramData,
                        readBlockGetMicros: HistogramData,
                        readNumMergeOperands: HistogramData,
                        softRateLimitDelayCount: HistogramData,
                        sstReadMicros: HistogramData,
                        stallL0NumFilesCount: HistogramData,
                        stallL0SlowdownCount: HistogramData,
                        stallMemtableCompactionCount: HistogramData,
                        subcompactionSetupTime: HistogramData,
                        tableOpenIOMicros: HistogramData,
                        tableSyncMicros: HistogramData,
                        walFileSyncMicros: HistogramData,
                        writeRawBlockMicros: HistogramData,
                        writeStall: HistogramData
                       )
