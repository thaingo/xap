package org.openspaces.persistency.kafka;

import com.gigaspaces.sync.*;
import com.gigaspaces.sync.serializable.EndpointData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static org.openspaces.persistency.kafka.KafkaSpaceSynchronizationEndpoint.KAFKA_TIMEOUT;

public class SpaceSynchronizationEndpointKafkaWriter implements Runnable{
    private static final Log logger = LogFactory.getLog(SpaceSynchronizationEndpointKafkaWriter.class);

    private SpaceSynchronizationEndpoint spaceSynchronizationEndpoint;
    private Consumer<String, EndpointData> kafkaConsumer;
    private String topic;
    private Set<TopicPartition> topicPartitions;
    private Map<TopicPartition, OffsetAndMetadata> startingPoint;
    private boolean firstTime = true;

    public SpaceSynchronizationEndpointKafkaWriter(SpaceSynchronizationEndpoint spaceSynchronizationEndpoint, Properties kafkaProps, String topic , String groupName) {
        this.spaceSynchronizationEndpoint = spaceSynchronizationEndpoint;
        this.topic = topic;
        kafkaProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupName);
        initKafkaConsumer(kafkaProps);
    }

    private void initKafkaConsumer(Properties kafkaProps) {
        this.kafkaConsumer = new KafkaConsumer<>(kafkaProps);
        this.topicPartitions = initTopicPartitions();
        kafkaConsumer.assign(topicPartitions);
        this.startingPoint = kafkaConsumer.committed(topicPartitions);
        if(startingPoint.values().stream().allMatch(Objects::isNull))
            this.startingPoint = null;
    }

    private Set<TopicPartition> initTopicPartitions(){
        List<PartitionInfo> partitionInfos;
        while (true){
            try{
                partitionInfos = kafkaConsumer.partitionsFor(topic);
                if(partitionInfos != null)
                    return partitionInfos.stream().map(p -> new TopicPartition(p.topic(), p.partition())).collect(Collectors.toSet());
            } catch (RuntimeException e){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException interruptedException) {
                    throw new RuntimeException("Interrupted while getting kafka partitions for topic " + topic);
                }
            }
        }
    }

    public Map<TopicPartition, OffsetAndMetadata> getStartingPoint() {
        return startingPoint;
    }

    public void setStartingPoint(Map<TopicPartition, OffsetAndMetadata> startingPoint) {
        this.startingPoint = startingPoint;
    }

    public void run() {
        try{
            while (true) {
                try {
                    Map<TopicPartition, OffsetAndMetadata> map;
                    if(firstTime){
                        map = startingPoint;
                        firstTime = false;
                    }
                    else {
                        map = kafkaConsumer.committed(topicPartitions);
                    }
                    if(map != null) {
                        Collection<TopicPartition> uninitializedPartitions = new HashSet<>();
                        for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : map.entrySet()) {
                            TopicPartition topicPartition = entry.getKey();
                            OffsetAndMetadata offsetAndMetadata = entry.getValue();
                            if (offsetAndMetadata != null) {
                                kafkaConsumer.seek(topicPartition, offsetAndMetadata);
                            } else {
                                uninitializedPartitions.add(topicPartition);
                            }
                        }
                        if (!uninitializedPartitions.isEmpty())
                            kafkaConsumer.seekToBeginning(uninitializedPartitions);
                    }
                    ConsumerRecords<String, EndpointData> records = kafkaConsumer.poll(Duration.ofSeconds(KAFKA_TIMEOUT));
                    for(ConsumerRecord<String, EndpointData> record: records) {
                        EndpointData endpointData = record.value();
                        switch (endpointData.getSyncEndpointMethod()) {
                            case onTransactionConsolidationFailure:
                                spaceSynchronizationEndpoint.onTransactionConsolidationFailure((ConsolidationParticipantData) endpointData);
                                logRecord(ConsolidationParticipantData.class.getSimpleName());
                                kafkaConsumer.commitSync();
                                break;
                            case onTransactionSynchronization:
                                spaceSynchronizationEndpoint.onTransactionSynchronization((TransactionData) endpointData);
                                try{
                                    spaceSynchronizationEndpoint.afterTransactionSynchronization((TransactionData) endpointData);
                                }catch (Exception e){
                                    if(logger.isWarnEnabled()) {
                                        logger.warn("Caught exception while attempting afterTransactionSynchronization: " + e.getMessage());
                                    }
                                }
                                logRecord(TransactionData.class.getSimpleName());
                                kafkaConsumer.commitSync();
                                break;
                            case onOperationsBatchSynchronization:
                                spaceSynchronizationEndpoint.onOperationsBatchSynchronization((OperationsBatchData) endpointData);
                                try{
                                    spaceSynchronizationEndpoint.afterOperationsBatchSynchronization((OperationsBatchData) endpointData);
                                }catch (Exception e){
                                    if(logger.isWarnEnabled()) {
                                        logger.warn("Caught exception while attempting afterOperationsBatchSynchronization: " + e.getMessage());
                                    }
                                }
                                logRecord(OperationsBatchData.class.getSimpleName());
                                kafkaConsumer.commitSync();
                                break;
                            case onAddIndex:
                                spaceSynchronizationEndpoint.onAddIndex((AddIndexData) endpointData);
                                logRecord(AddIndexData.class.getSimpleName());
                                kafkaConsumer.commitSync();
                                break;
                            case onIntroduceType:
                                spaceSynchronizationEndpoint.onIntroduceType((IntroduceTypeData) endpointData);
                                logRecord(IntroduceTypeData.class.getSimpleName());
                                kafkaConsumer.commitSync();
                                break;
                        }
                    }
                }catch (Exception e){
                    if(e instanceof InterruptedException)
                        throw (InterruptedException) e;
                    handleException(e);
                    continue;
                }
            }
        } catch (InterruptedException e){
            if(logger.isInfoEnabled())
                logger.info("Closing kafka consumer of topic " + topic);
            kafkaConsumer.close(Duration.ofSeconds(KAFKA_TIMEOUT));
        }
    }

    private void handleException(Exception e) throws InterruptedException {
        logException(e);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException interruptedException) {
            throw new InterruptedException("Interrupted while recovering from exception thrown during message consumption from kafka topic " + topic);
        }
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