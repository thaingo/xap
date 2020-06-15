package org.openspaces.persistency.kafka.internal;

import com.gigaspaces.api.InternalApi;
import org.apache.commons.lang.SerializationUtils;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

@InternalApi
public class KafkaEndpointDataDeserializer implements Deserializer<KafkaEndpointData> {
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public KafkaEndpointData deserialize(String topic, byte[] data) {
        return (KafkaEndpointData) SerializationUtils.deserialize(data);
    }

    @Override
    public KafkaEndpointData deserialize(String topic, Headers headers, byte[] data) {
        return (KafkaEndpointData) SerializationUtils.deserialize(data);
    }
}
