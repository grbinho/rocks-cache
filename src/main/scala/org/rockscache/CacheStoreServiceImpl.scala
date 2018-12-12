package org.rockscache

import java.util

import com.typesafe.scalalogging.LazyLogging
import org.rockscache.avro.proto.{CacheStore, KeyValuePair, KeyValuePairBatchResponse}
import org.rocksdb.{WriteBatch, WriteOptions}

import scala.collection.JavaConverters._

class CacheStoreServiceImpl(cacheDb: CacheStoreDatabase) extends CacheStore with LazyLogging {
  val db = cacheDb.db

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
    keyValuePair.map(kvp => db.get(kvp.getKey.array) == null)

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
