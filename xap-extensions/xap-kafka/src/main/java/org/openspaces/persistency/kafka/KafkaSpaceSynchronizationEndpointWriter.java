package org.openspaces.persistency.kafka;

import com.gigaspaces.sync.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.openspaces.persistency.kafka.internal.KafkaEndpointData;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class KafkaSpaceSynchronizationEndpointWriter implements Runnable{
    private static final Log logger = LogFactory.getLog(KafkaSpaceSynchronizationEndpointWriter.class);
    private final static long KAFKA_CONSUME_TIMEOUT = 30;

    private SpaceSynchronizationEndpoint spaceSynchronizationEndpoint;
    private Consumer<String, KafkaEndpointData> kafkaConsumer;
    private final String topic;

    public KafkaSpaceSynchronizationEndpointWriter(SpaceSynchronizationEndpoint spaceSynchronizationEndpoint, Properties kafkaProps, String topic) {
        this.kafkaConsumer = new KafkaConsumer<>(kafkaProps);
        this.spaceSynchronizationEndpoint = spaceSynchronizationEndpoint;
        this.topic = topic;
    }

    public void run() {
        Set<TopicPartition> topicPartitions = getTopicPartitions();
        if(topicPartitions != null)
            kafkaConsumer.assign(topicPartitions);
        try{
            while (true) {
                try {
                    if(topicPartitions != null) {
                        Map<TopicPartition, OffsetAndMetadata> map = kafkaConsumer.committed(topicPartitions);
                        Collection<TopicPartition> uninitializedPartitions = new HashSet<>();
                        for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : map.entrySet()) {
                            TopicPartition topicPartition = entry.getKey();
                            OffsetAndMetadata offsetAndMetadata = entry.getValue();
                            if(offsetAndMetadata != null) {
                                kafkaConsumer.seek(topicPartition, offsetAndMetadata);
                            }
                            else{
                                uninitializedPartitions.add(topicPartition);
                            }
                        }
                        if(!uninitializedPartitions.isEmpty())
                            kafkaConsumer.seekToBeginning(uninitializedPartitions);
                    }
                    ConsumerRecords<String, KafkaEndpointData> records = kafkaConsumer.poll(Duration.ofSeconds(KAFKA_CONSUME_TIMEOUT));
                    for(ConsumerRecord<String, KafkaEndpointData> record: records) {
                        KafkaEndpointData kafkaEndpointData = record.value();
                        switch (kafkaEndpointData.getSyncEndpointMethod()) {
                            case onTransactionConsolidationFailure:
                                spaceSynchronizationEndpoint.onTransactionConsolidationFailure((ConsolidationParticipantData) kafkaEndpointData);
                                logRecord(ConsolidationParticipantData.class.getSimpleName());
                                kafkaConsumer.commitSync();
                                break;
                            case onTransactionSynchronization:
                                spaceSynchronizationEndpoint.onTransactionSynchronization((TransactionData) kafkaEndpointData);
                                try{
                                    spaceSynchronizationEndpoint.afterTransactionSynchronization((TransactionData) kafkaEndpointData);
                                }catch (Throwable t){
                                    if(logger.isWarnEnabled()) {
                                        logger.warn("Caught exception while attempting afterTransactionSynchronization: " + t.getMessage());
                                    }
                                }
                                logRecord(TransactionData.class.getSimpleName());
                                kafkaConsumer.commitSync();
                                break;
                            case onOperationsBatchSynchronization:
                                spaceSynchronizationEndpoint.onOperationsBatchSynchronization((OperationsBatchData) kafkaEndpointData);
                                try{
                                    spaceSynchronizationEndpoint.afterOperationsBatchSynchronization((OperationsBatchData) kafkaEndpointData);
                                }catch (Throwable t){
                                    if(logger.isWarnEnabled()) {
                                        logger.warn("Caught exception while attempting afterOperationsBatchSynchronization: " + t.getMessage());
                                    }
                                }
                                logRecord(OperationsBatchData.class.getSimpleName());
                                kafkaConsumer.commitSync();
                                break;
                            case onAddIndex:
                                spaceSynchronizationEndpoint.onAddIndex((AddIndexData) kafkaEndpointData);
                                logRecord(AddIndexData.class.getSimpleName());
                                kafkaConsumer.commitSync();
                                break;
                            case onIntroduceType:
                                spaceSynchronizationEndpoint.onIntroduceType((IntroduceTypeData) kafkaEndpointData);
                                logRecord(IntroduceTypeData.class.getSimpleName());
                                kafkaConsumer.commitSync();
                                break;
                        }
                    }
                }catch (Exception e){
                    logException(e);
                    continue;
                }
            }
        } finally {
            kafkaConsumer.close();
        }
    }

    private Set<TopicPartition> getTopicPartitions(){
        List<PartitionInfo> partitionInfos;
        try{
            partitionInfos = kafkaConsumer.partitionsFor(topic);
        } catch (Exception e){
            return null;
        }
        if(partitionInfos == null)
            return null;
        return partitionInfos.stream().map(p -> new TopicPartition(p.topic(), p.partition())).collect(Collectors.toSet());
    }

    private void logException(Exception e) {
        if(logger.isWarnEnabled())
            logger.warn("Caught exception while consuming Kafka records: " + e.getMessage());
    }

    private void logRecord(String endpointType){
        if(logger.isDebugEnabled()) {
            logger.debug("Consumed kafka message of type " + endpointType + " and persisted to " + spaceSynchronizationEndpoint.getClass().getSimpleName());
        }
    }
}
