package org.openspaces.persistency.kafka;

import com.gigaspaces.sync.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.*;
import org.openspaces.persistency.kafka.internal.*;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class KafkaSpaceSynchronizationEndpoint extends SpaceSynchronizationEndpoint {
    private final static Log logger = LogFactory.getLog(KafkaSpaceSynchronizationEndpoint.class);
    private final static long KAFKA_PRODUCE_TIMEOUT = 30;

    private final Producer<String, KafkaEndpointData> kafkaProducer;
    private final KafkaSpaceSynchronizationEndpointWriter kafkaSpaceSynchronizationEndpointWriter;
    private final String topic;

    public KafkaSpaceSynchronizationEndpoint(SpaceSynchronizationEndpoint spaceSynchronizationEndpoint, Properties kafkaProps, String topic) {
        this.topic = topic;
        Properties kafkaProducerProps = (Properties) kafkaProps.clone();
        Properties kafkaConsumerProps = (Properties) kafkaProps.clone();
        kafkaProducerProps.put(ProducerConfig.ACKS_CONFIG, "1");
        kafkaProducerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");
        kafkaProducerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaEndpointDataSerializer.class.getName());
        this.kafkaProducer = new KafkaProducer<>(kafkaProducerProps);
        kafkaConsumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "0");
        kafkaConsumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        kafkaConsumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringDeserializer");
        kafkaConsumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaEndpointDataDeserializer.class.getName());
        kafkaConsumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");
        this.kafkaSpaceSynchronizationEndpointWriter = new KafkaSpaceSynchronizationEndpointWriter(spaceSynchronizationEndpoint, kafkaConsumerProps, topic);
    }

    public void start(){
        Thread thread = new Thread(kafkaSpaceSynchronizationEndpointWriter);
        thread.start();
    }

    @Override
    public void onTransactionConsolidationFailure(ConsolidationParticipantData participantData) {
        sendToKafka(new ProducerRecord<>(topic, new KafkaConsolidationParticipantData(participantData, SpaceSyncEndpointMethod.onTransactionConsolidationFailure)));
    }

    @Override
    public void onTransactionSynchronization(TransactionData transactionData) {
        sendToKafka(new ProducerRecord<>(topic, new KafkaTransactionData(transactionData, SpaceSyncEndpointMethod.onTransactionSynchronization)));
    }

    @Override
    public void onOperationsBatchSynchronization(OperationsBatchData batchData) {
        sendToKafka(new ProducerRecord<>(topic, new KafkaOperationBatchData(batchData, SpaceSyncEndpointMethod.onOperationsBatchSynchronization)));
    }

    @Override
    public void onAddIndex(AddIndexData addIndexData) {
        sendToKafka(new ProducerRecord<>(topic, new KafkaAddIndexData(addIndexData, SpaceSyncEndpointMethod.onAddIndex)));
    }

    @Override
    public void onIntroduceType(IntroduceTypeData introduceTypeData) {
        sendToKafka(new ProducerRecord<>(topic, new KafkaIntroduceTypeData(introduceTypeData, SpaceSyncEndpointMethod.onIntroduceType)));
    }

    private void sendToKafka(ProducerRecord<String, KafkaEndpointData> producerRecord){
        try {
            Future<RecordMetadata> future = kafkaProducer.send(producerRecord);
            RecordMetadata recordMetadata = future.get(KAFKA_PRODUCE_TIMEOUT, TimeUnit.SECONDS);
            if(logger.isDebugEnabled())
                logger.debug("Written message to Kafka: " + producerRecord);
        } catch (Exception e) {
            throw new SpaceKafkaException("Failed to write to kafka", e);
        }
    }
}
