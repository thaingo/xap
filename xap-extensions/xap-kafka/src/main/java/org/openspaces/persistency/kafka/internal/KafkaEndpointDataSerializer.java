package org.openspaces.persistency.kafka.internal;

import com.gigaspaces.api.InternalApi;
import com.gigaspaces.sync.serializable.EndpointData;
import org.apache.commons.lang.SerializationUtils;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

@InternalApi
public class KafkaEndpointDataSerializer implements Serializer<EndpointData> {
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public byte[] serialize(String topic, EndpointData data) {
        return SerializationUtils.serialize(data);
    }

    @Override
    public byte[] serialize(String topic, Headers headers, EndpointData data) {
        return SerializationUtils.serialize(data);
    }

    @Override
    public void close() {

    }
}
