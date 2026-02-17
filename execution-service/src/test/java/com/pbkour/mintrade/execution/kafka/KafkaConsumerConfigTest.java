package com.pbkour.mintrade.execution.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KafkaConsumerConfigTest {

    @Test
    void consumerFactoryConfigured() {
        KafkaConsumerConfig config = new KafkaConsumerConfig();
        ReflectionTestUtils.setField(config, "bootstrapAddress", "host.test:1234");
        ReflectionTestUtils.setField(config, "groupId", "test-group");

        ConsumerFactory<String, String> consumerFactory = config.consumerFactory();
        assertNotNull(consumerFactory, "ConsumerFactory should be created");
        assertInstanceOf(DefaultKafkaConsumerFactory.class, consumerFactory, "Should be DefaultKafkaConsumerFactory");

        DefaultKafkaConsumerFactory<String, String> defaultFactory = (DefaultKafkaConsumerFactory<String, String>) consumerFactory;
        Map<String, Object> props = defaultFactory.getConfigurationProperties();

        assertEquals("host.test:1234", props.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals("test-group", props.get(ConsumerConfig.GROUP_ID_CONFIG));
        assertEquals(StringDeserializer.class, props.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
        assertEquals(StringDeserializer.class, props.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));
    }

    @Test
    void kafkaListenerContainerFactoryUsesConsumerFactory() {
        KafkaConsumerConfig config = new KafkaConsumerConfig();
        ReflectionTestUtils.setField(config, "bootstrapAddress", "host.test:1234");
        ReflectionTestUtils.setField(config, "groupId", "test-group");

        ConcurrentKafkaListenerContainerFactory<String, String> factory = config.kafkaListenerContainerFactory();
        assertNotNull(factory, "Container factory should be created");
        assertInstanceOf(DefaultKafkaConsumerFactory.class, factory.getConsumerFactory(), "Listener factory should use DefaultKafkaConsumerFactory");

        DefaultKafkaConsumerFactory<String, String> defaultFactory = (DefaultKafkaConsumerFactory<String, String>) factory.getConsumerFactory();
        Map<String, Object> props = defaultFactory.getConfigurationProperties();

        assertEquals("host.test:1234", props.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals("test-group", props.get(ConsumerConfig.GROUP_ID_CONFIG));
    }
}

