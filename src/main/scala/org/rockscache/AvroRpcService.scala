package org.rockscache

import java.net.InetSocketAddress

import org.apache.avro.ipc.specific.SpecificResponder
import org.apache.avro.ipc.{NettyServer, Server}
import org.rockscache.avro.proto.CacheStore

object AvroRpcService {
  def createService(db: CacheStoreDatabase): Server = {
    new NettyServer(new SpecificResponder(classOf[CacheStore], new CacheStoreServiceImpl(db)), new InetSocketAddress(65111))
  }
}
