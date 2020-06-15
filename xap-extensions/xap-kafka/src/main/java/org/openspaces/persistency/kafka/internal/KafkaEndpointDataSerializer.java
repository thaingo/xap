package org.openspaces.persistency.kafka.internal;

import com.gigaspaces.api.InternalApi;
import org.apache.commons.lang.SerializationUtils;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

@InternalApi
public class KafkaEndpointDataSerializer implements Serializer<KafkaEndpointData> {
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public byte[] serialize(String topic, KafkaEndpointData data) {
        return SerializationUtils.serialize(data);
    }

    @Override
    public byte[] serialize(String topic, Headers headers, KafkaEndpointData data) {
        return SerializationUtils.serialize(data);
    }

    @Override
    public void close() {

    }
}
