/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package org.rockscache.avro.proto;

@SuppressWarnings("all")
@org.apache.avro.specific.AvroGenerated
public interface CacheStore {
  public static final org.apache.avro.Protocol PROTOCOL = org.apache.avro.Protocol.parse("{\"protocol\":\"CacheStore\",\"namespace\":\"org.rockscache.avro.proto\",\"types\":[{\"type\":\"record\",\"name\":\"KeyValuePair\",\"fields\":[{\"name\":\"key\",\"type\":\"bytes\"},{\"name\":\"value\",\"type\":[\"null\",\"bytes\"]}]},{\"type\":\"record\",\"name\":\"KeyValuePairBatchResponse\",\"fields\":[{\"name\":\"payload\",\"type\":{\"type\":\"array\",\"items\":\"boolean\"}}]}],\"messages\":{\"checkAndStore\":{\"request\":[{\"name\":\"keyValuePair\",\"type\":\"KeyValuePair\"}],\"response\":\"boolean\"},\"checkAndStoreBatch\":{\"request\":[{\"name\":\"keyValuePairArray\",\"type\":{\"type\":\"array\",\"items\":\"KeyValuePair\"}}],\"response\":\"KeyValuePairBatchResponse\"}}}");
  /**
   */
  boolean checkAndStore(org.rockscache.avro.proto.KeyValuePair keyValuePair) throws org.apache.avro.AvroRemoteException;
  /**
   */
  org.rockscache.avro.proto.KeyValuePairBatchResponse checkAndStoreBatch(java.util.List<org.rockscache.avro.proto.KeyValuePair> keyValuePairArray) throws org.apache.avro.AvroRemoteException;

  @SuppressWarnings("all")
  public interface Callback extends CacheStore {
    public static final org.apache.avro.Protocol PROTOCOL = org.rockscache.avro.proto.CacheStore.PROTOCOL;
    /**
     * @throws java.io.IOException The async call could not be completed.
     */
    void checkAndStore(org.rockscache.avro.proto.KeyValuePair keyValuePair, org.apache.avro.ipc.Callback<java.lang.Boolean> callback) throws java.io.IOException;
    /**
     * @throws java.io.IOException The async call could not be completed.
     */
    void checkAndStoreBatch(java.util.List<org.rockscache.avro.proto.KeyValuePair> keyValuePairArray, org.apache.avro.ipc.Callback<org.rockscache.avro.proto.KeyValuePairBatchResponse> callback) throws java.io.IOException;
  }
}