package org.rockscache

import com.twitter.util.Await
import com.typesafe.scalalogging.LazyLogging
import scala.util.{Failure, Success, Try}
import com.twitter.finagle.Http

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

object RocksCacheApp extends App with LazyLogging {
  Try {
    logger.info("Starting service")
    val db = new CacheStoreDatabase()
    val restService = new RestService(db).httpService
    val avroService = AvroRpcService.createService(db)

    val httpService = Http.server.serve(":8080", restService)
    logger.info("Service started. Running on port 65111")

    // TODO: Make this start/shutdown part better
    avroService.start()
    Await.ready(httpService)
  } match {
    case Failure(exception) => logger.error("Error occurred", exception)
    case Success(_) => logger.info("Exited")
  }
}
