package com.pbkour.mintrade.commons.kafka.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(classes = KafkaProducerConfig.class)
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=host.test:1234")
class KafkaProducerConfigTest {

    @Autowired
    ProducerFactory<String, String> producerFactory;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void producerFactoryConfigured() {
        assertNotNull(producerFactory, "ProducerFactory should be created");
        assertInstanceOf(DefaultKafkaProducerFactory.class, producerFactory, "ProducerFactory should be a DefaultKafkaProducerFactory");

        DefaultKafkaProducerFactory<String, String> defaultFactory =
            (DefaultKafkaProducerFactory<String, String>) producerFactory;

        Map<String, Object> config = defaultFactory.getConfigurationProperties();
        assertEquals("host.test:1234", config.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals(StringSerializer.class, config.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG));
        assertEquals(StringSerializer.class, config.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG));
    }

    @Test
    void kafkaTemplateCreated() {
        assertNotNull(kafkaTemplate, "KafkaTemplate should be created");
        assertInstanceOf(DefaultKafkaProducerFactory.class, kafkaTemplate.getProducerFactory(), "KafkaTemplate should use DefaultKafkaProducerFactory");
    }
}

