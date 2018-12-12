package org.rockscache

import java.lang.management.ManagementFactory
import java.nio.file.{Files, Paths}

import com.typesafe.scalalogging.LazyLogging
import org.rocksdb._

import scala.collection.JavaConverters._

class CacheStoreDatabase extends LazyLogging {

    RocksDB.loadLibrary()

    val statisticsObject = new Statistics()

    //TODO: Get available system memory. Give some memory to the JVM for service, assign some percentage to block cache
    //TODO: Use CLOCK cache

    //TODO: Expose statistics
    //TODO: Tune level0 size to be equal to level1 (read tuning guide)
    //TODO: Limit JVM memory usage. Memory is used by the native process.

    //TODO: Expose management features over HTTP endpoint

    val maxMemoryForJVM = Runtime.getRuntime.maxMemory

    import com.sun.management.OperatingSystemMXBean

    val osBean = ManagementFactory.getPlatformMXBean(classOf[OperatingSystemMXBean])
    val totalSystemMemory = osBean.getTotalPhysicalMemorySize
    val cpuCount = Runtime.getRuntime.availableProcessors

    logger.info(s"Total system memory: ${totalSystemMemory} bytes")
    logger.info(s"Memory reserved by the JVM: ${maxMemoryForJVM} bytes")

    val blockBasedTableConfig = new BlockBasedTableConfig()
    val blockCacheSize = totalSystemMemory / 2 - maxMemoryForJVM

    logger.info(s"Block cache size: ${blockCacheSize} bytes")

    val clockCache = new ClockCache(blockCacheSize)
    blockBasedTableConfig.setBlockCache(clockCache)

    //TODO: Use settings file

    val options = new Options()
      .setCreateIfMissing(true)
      .setIncreaseParallelism(cpuCount) //Sets low threads to "totalThreads", highs stays at 1
      .setCompressionPerLevel(List(
      CompressionType.NO_COMPRESSION, //Do not compress first two levels of data
      CompressionType.NO_COMPRESSION,
      CompressionType.LZ4_COMPRESSION,
      CompressionType.LZ4_COMPRESSION,
      CompressionType.LZ4_COMPRESSION,
      CompressionType.LZ4_COMPRESSION,
      CompressionType.LZ4_COMPRESSION).asJava)
      .setCompressionType(CompressionType.LZ4_COMPRESSION)
      .setCompactionStyle(CompactionStyle.LEVEL)
      .setStatistics(statisticsObject)
      .setTableFormatConfig(blockBasedTableConfig)

    val dbPath = "/tmp/rocks-cache/ttldb"

    Files.createDirectories(Paths.get(dbPath))

    val ttl: Int = 7 * 24 * 60 * 60 //7 days of retention
    val readonly = false

    val db = TtlDB.open(options, dbPath, ttl, readonly)
}
