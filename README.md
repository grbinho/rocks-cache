# rocks-cache
Durable Ephemeral cache

Cache service backed by rocksdb.

It allows for storing epehmeral key value pairs.
Initial idea is to use it as a second level cache when data need to survive service restarts.
It can then be used to populate in process caches as it's required.

All values in the cache have a TTL which keeps the maintenance low.
