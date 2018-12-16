package org.rockscache

package object statistics {

  object Metric {
    val all = Seq(BlockCacheHit, BlockCacheMiss)

    def apply(name: String): Metric =
      name.toLowerCase match {
        case "blockcachemiss" => BlockCacheMiss
        case "blockcachehit" => BlockCacheHit
        case _ => BlockCacheMiss
      }

  }

  case class MetricValue(metric: Metric, value: Double)

  sealed trait Metric {
    val name: String
    val description: String
  }

  case object BlockCacheMiss extends Metric {
    override val name: String = "BlockCacheMiss"
    override val description: String = "Total block cache misses: BLOCK_CACHE_MISS == BLOCK_CACHE_INDEX_MISS + BLOCK_CACHE_FILTER_MISS + BLOCK_CACHE_DATA_MISS"
  }

  case object BlockCacheHit extends Metric {
    override val name: String = "BlockCacheHit"
    override val description: String = "Total block cache hit: BLOCK_CACHE_HIT == BLOCK_CACHE_INDEX_HIT + BLOCK_CACHE_FILTER_HIT + BLOCK_CACHE_DATA_HIT"
  }



}
