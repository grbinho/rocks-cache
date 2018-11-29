package org.rockscache

import java.util

import com.typesafe.scalalogging.LazyLogging
import org.rockscache.avro.proto.CacheStore
import org.rocksdb._

import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._

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

class CacheStoreImpl extends CacheStore {

  RocksDB.loadLibrary()

  val statisticsObject = new Statistics()

  val options = new Options()
    .setCreateIfMissing(true)
    .setCompressionType(CompressionType.LZ4_COMPRESSION)
    .setCompactionStyle(CompactionStyle.LEVEL)
    .setStatistics(statisticsObject)

  val dbPath = "/tmp/rocks-cache/ttldb"
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

  override def checkAndStore(keyValuePair: KeyValuePair): Boolean = {
    _checkAndStore(keyValuePair)
  }

  override def checkAndStoreBatch(keyValuePairArray: util.List[KeyValuePair]): KeyValuePairBatchResponse = {
    val result = keyValuePairArray.asScala.map(_checkAndStore).map(Boolean.box).toList.asJava
    val response = new KeyValuePairBatchResponse(result)
    response
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
