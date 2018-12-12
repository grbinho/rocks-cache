package org.rockscache

import java.lang.management.ManagementFactory
import java.nio.file.{Files, Paths}
import java.util

import com.typesafe.scalalogging.LazyLogging
import org.rockscache.avro.proto.CacheStore
import org.rocksdb._

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

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
* TTL available also in RocksDB (values in seconds)
*
* Use some arbitrary value for values that will end up being dictionary compressed. Keys are what we want in dedup use case
*
* MultiGet could be useful when we need to check keys for the whole package
*
* Column family per data source? vs multiple database instances?
*
* "The FIFOStyle Compaction drops oldest file when obsolete and can be used for cache-like data."
* */


import java.net.InetSocketAddress

import org.apache.avro.ipc.specific.SpecificResponder
import org.apache.avro.ipc.{NettyServer, Server}
import org.rockscache.avro.proto._

object AvroRpcService {
  def createService(): Server = {
    new NettyServer(new SpecificResponder(classOf[CacheStore], new CacheStoreImpl), new InetSocketAddress(65111))
  }
}

class CacheStoreImpl extends CacheStore with LazyLogging {

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

  private def _checkAndStore(keyValuePair: KeyValuePair): Boolean = {
    val value = db.get(keyValuePair.getKey.array)
    value match {
      case null =>
        db.put(keyValuePair.getKey.array, keyValuePair.getValue.array)
        true
      case _ => false
    }
  }

  private def _checkBatchIfNotExists(keyValuePair: Seq[KeyValuePair]): Seq[Boolean] =
    keyValuePair.map (kvp => db.get(kvp.getKey.array) == null)

  private def _writeBatch(keyValuePair: Seq[KeyValuePair]) = {
    val batch = new WriteBatch()
    val writeOptions = new WriteOptions()
    keyValuePair.foreach(kvp => batch.put(kvp.getKey.array, kvp.getValue.array))
    db.write(writeOptions, batch)
  }

  override def checkAndStore(keyValuePair: KeyValuePair): Boolean = {
    _checkAndStore(keyValuePair)
  }

  override def checkAndStoreBatch(keyValuePairArray: util.List[KeyValuePair]): KeyValuePairBatchResponse = {
    val keyValuePair = keyValuePairArray.asScala
    val keyValuePairsToWrite = keyValuePair.zip(_checkBatchIfNotExists(keyValuePair)).filter(_._2)
    _writeBatch(keyValuePairsToWrite.map(_._1))
    val writtenKeys = keyValuePairsToWrite.map(_._2).map(Boolean.box).toList.asJava
    new KeyValuePairBatchResponse(writtenKeys)
  }
}


object RocksCacheApp extends App with LazyLogging {
  Try {
    logger.info("Starting service")
    val service = AvroRpcService.createService()
    //service.start()
    logger.info("Service started. Running on port 65111")

    service.join()
  } match {
    case Failure(exception) => logger.error("Error occurred", exception)
    case Success(_) => logger.info("Exited")
  }
}
