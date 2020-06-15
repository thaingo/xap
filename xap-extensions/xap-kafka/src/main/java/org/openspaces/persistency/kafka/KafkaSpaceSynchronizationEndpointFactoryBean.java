package org.openspaces.persistency.kafka;

import com.gigaspaces.sync.SpaceSynchronizationEndpoint;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Properties;

public class KafkaSpaceSynchronizationEndpointFactoryBean implements FactoryBean<KafkaSpaceSynchronizationEndpoint>, InitializingBean, DisposableBean {
    private SpaceSynchronizationEndpoint synchronizationEndpoint;
    private KafkaSpaceSynchronizationEndpoint kafkaSpaceSynchronizationEndpoint;
    private Properties kafkaProperties;
    private String topic;

    @Override
    public KafkaSpaceSynchronizationEndpoint getObject() {
        this.kafkaSpaceSynchronizationEndpoint.start();
        return kafkaSpaceSynchronizationEndpoint;
    }

    @Override
    public Class<?> getObjectType() {
        return KafkaSpaceSynchronizationEndpoint.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public Properties getKafkaProperties() {
        return kafkaProperties;
    }

    public void setKafkaProperties(Properties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    public SpaceSynchronizationEndpoint getSynchronizationEndpoint() {
        return synchronizationEndpoint;
    }

    public void setSynchronizationEndpoint(SpaceSynchronizationEndpoint synchronizationEndpoint) {
        this.synchronizationEndpoint = synchronizationEndpoint;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.kafkaSpaceSynchronizationEndpoint = new KafkaSpaceSynchronizationEndpoint(synchronizationEndpoint, kafkaProperties, topic);
    }
}
