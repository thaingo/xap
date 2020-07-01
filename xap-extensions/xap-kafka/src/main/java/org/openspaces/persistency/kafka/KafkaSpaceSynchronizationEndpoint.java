package org.openspaces.persistency.kafka;

import com.gigaspaces.internal.utils.concurrent.GSThreadFactory;
import com.gigaspaces.sync.*;
import com.gigaspaces.sync.serializable.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.*;
import org.openspaces.persistency.kafka.internal.KafkaEndpointDataDeserializer;
import org.openspaces.persistency.kafka.internal.KafkaEndpointDataSerializer;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class KafkaSpaceSynchronizationEndpoint extends SpaceSynchronizationEndpoint {
    private final static Log logger = LogFactory.getLog(KafkaSpaceSynchronizationEndpoint.class);
    public final static long KAFKA_TIMEOUT = 10;
    private final static String MASTER_THREAD = "master-endpoint";
    private final static String MASTER_GROUP = "master-group";

    private final Producer<String, EndpointData> kafkaProducer;
    private final SpaceSynchronizationEndpointKafkaWriter primaryEndpointKafkaWriter;
    private final Map<String, SpaceSynchronizationEndpointKafkaWriter> secondaryEndpointsKafkaWriters = new HashMap<>();
    private final String topic;
    private final Set<Thread> writerThreads = new HashSet<>();


    public KafkaSpaceSynchronizationEndpoint(SpaceSynchronizationEndpoint primaryEndpoint, Properties kafkaProps, String topic) {
        this.topic = topic;
        this.kafkaProducer = new KafkaProducer<>(initKafkaProducerProperties(kafkaProps));
        Properties consumerProperties = initConsumerProperties(kafkaProps);
        this.primaryEndpointKafkaWriter = new SpaceSynchronizationEndpointKafkaWriter(primaryEndpoint, consumerProperties, topic, MASTER_GROUP);
    }

    public KafkaSpaceSynchronizationEndpoint(SpaceSynchronizationEndpoint primaryEndpoint, Map<String, SpaceSynchronizationEndpoint> secondaryEndpoints, Properties kafkaProps, String topic) {
        this.topic = topic;
        this.kafkaProducer = new KafkaProducer<>(initKafkaProducerProperties(kafkaProps));
        Properties consumerProperties = initConsumerProperties(kafkaProps);
        this.primaryEndpointKafkaWriter = new SpaceSynchronizationEndpointKafkaWriter(primaryEndpoint, consumerProperties, topic, MASTER_GROUP);
        for(Map.Entry<String, SpaceSynchronizationEndpoint> entry: secondaryEndpoints.entrySet()){
            secondaryEndpointsKafkaWriters.put(entry.getKey(), new SpaceSynchronizationEndpointKafkaWriter(entry.getValue(), consumerProperties, topic, entry.getKey()));
        }
    }

    private Properties initKafkaProducerProperties(Properties kafkaProps){
        Map<Object,Object> props = new HashMap<>(kafkaProps);
        props.putIfAbsent(ProducerConfig.ACKS_CONFIG, "1");
        props.putIfAbsent(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");
        props.putIfAbsent(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaEndpointDataSerializer.class.getName());
        return toProperties(props);
    }

    private Properties initConsumerProperties(Properties kafkaProps){
        Map<Object,Object> props = new HashMap<>(kafkaProps);
        props.putIfAbsent(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.putIfAbsent(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringDeserializer");
        props.putIfAbsent(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaEndpointDataDeserializer.class.getName());
        props.putIfAbsent(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");
        return toProperties(props);
    }

    private Properties toProperties(Map<Object, Object> map){
        Properties result = new Properties();
        result.putAll(map);
        return result;
    }

    public void start(){
        writerThreads.add(GSThreadFactory.daemon(topic + "-" + MASTER_THREAD).newThread(primaryEndpointKafkaWriter));
        for(SpaceSynchronizationEndpointKafkaWriter secondaryWriter: secondaryEndpointsKafkaWriters.values()){
            int counter = 0;
            if(secondaryWriter.getStartingPoint() == null)
                secondaryWriter.setStartingPoint(primaryEndpointKafkaWriter.getStartingPoint());
            writerThreads.add(GSThreadFactory.daemon(topic + "-secondary-endpoint-" + counter).newThread(secondaryWriter));
            counter++;
        }
        writerThreads.forEach(Thread::start);
    }

    @Override
    public void onTransactionConsolidationFailure(ConsolidationParticipantData participantData) {
        sendToKafka(new ProducerRecord<>(topic, participantData.getSourceDetails().getName(), new ConsolidationParticipantEndpointData(participantData, SpaceSyncEndpointMethod.onTransactionConsolidationFailure)));
    }

    @Override
    public void onTransactionSynchronization(TransactionData transactionData) {
        sendToKafka(new ProducerRecord<>(topic, transactionData.getSourceDetails().getName(), new TransactionEndpointData(transactionData, SpaceSyncEndpointMethod.onTransactionSynchronization)));
    }

    @Override
    public void onOperationsBatchSynchronization(OperationsBatchData batchData) {
        sendToKafka(new ProducerRecord<>(topic, batchData.getSourceDetails().getName(), new OperationsBatchEndpointData(batchData, SpaceSyncEndpointMethod.onOperationsBatchSynchronization)));
    }

    @Override
    public void onAddIndex(AddIndexData addIndexData) {
        sendToKafka(new ProducerRecord<>(topic, new AddIndexEndpointData(addIndexData, SpaceSyncEndpointMethod.onAddIndex)));
    }

    @Override
    public void onIntroduceType(IntroduceTypeData introduceTypeData) {
        sendToKafka(new ProducerRecord<>(topic, new IntroduceTypeEndpointData(introduceTypeData, SpaceSyncEndpointMethod.onIntroduceType)));
    }

    private void sendToKafka(ProducerRecord<String, EndpointData> producerRecord){
        try {
            Future<RecordMetadata> future = kafkaProducer.send(producerRecord);
            RecordMetadata recordMetadata = future.get(KAFKA_TIMEOUT, TimeUnit.SECONDS);
            if(logger.isDebugEnabled())
                logger.debug("Written message to Kafka: " + producerRecord + ". partition: " + recordMetadata.partition() + ", offset: " + recordMetadata.offset());
        } catch (Exception e) {
            throw new SpaceKafkaException("Failed to write to kafka", e);
        }
    }

    public void close() {
        writerThreads.forEach(Thread::interrupt);
    }
}