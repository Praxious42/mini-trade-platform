package com.pbkour.mintrade.commons.kafka.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(classes = {KafkaConsumerConfig.class, KafkaConsumerConfigTest.TestConfig.class})
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=host.test:1234")
class KafkaConsumerConfigTest {

    @Autowired
    ConsumerFactory<String, String> consumerFactory;

    @Autowired
    KafkaConsumerConfig config;

    @Test
    void consumerFactoryConfigured() {
        assertNotNull(consumerFactory, "ConsumerFactory should be created");
        assertInstanceOf(DefaultKafkaConsumerFactory.class, consumerFactory, "Should be DefaultKafkaConsumerFactory");

        DefaultKafkaConsumerFactory<String, String> defaultFactory = (DefaultKafkaConsumerFactory<String, String>) consumerFactory;
        Map<String, Object> props = defaultFactory.getConfigurationProperties();

        assertEquals("host.test:1234", props.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals(StringDeserializer.class, props.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
        assertEquals(StringDeserializer.class, props.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));
    }

    @Test
    void kafkaListenerContainerFactoryUsesConsumerFactory() {
        DefaultErrorHandler err = new DefaultErrorHandler();
        ConcurrentKafkaListenerContainerFactory<String, String> factory = config.kafkaListenerContainerFactory(err);
        assertNotNull(factory, "Container factory should be created");
        assertInstanceOf(DefaultKafkaConsumerFactory.class, factory.getConsumerFactory(), "Listener factory should use DefaultKafkaConsumerFactory");
    }

    @Test
    void kafkaErrorHandlerConfigured() {
        // create a simple KafkaTemplate for the recoverer
        KafkaTemplate<String, String> template = testTemplate();

        DefaultErrorHandler handler = config.kafkaErrorHandler(template);
        assertNotNull(handler, "DefaultErrorHandler should be created");
        assertInstanceOf(DefaultErrorHandler.class, handler, "Should be DefaultErrorHandler");
    }

    private KafkaTemplate<String, String> testTemplate() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(new HashMap<>()));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        KafkaTemplate<String, String> kafkaTemplate() {
            return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(new HashMap<>()));
        }
    }
}


