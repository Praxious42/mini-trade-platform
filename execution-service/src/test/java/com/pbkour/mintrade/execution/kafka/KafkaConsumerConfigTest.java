package com.pbkour.mintrade.execution.kafka;

import com.pbkour.mintrade.commons.kafka.config.KafkaConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
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

        ConcurrentKafkaListenerContainerFactory<String, String> factory = config.kafkaListenerContainerFactory(new DefaultErrorHandler());
        assertNotNull(factory, "Container factory should be created");
        assertInstanceOf(DefaultKafkaConsumerFactory.class, factory.getConsumerFactory(), "Listener factory should use DefaultKafkaConsumerFactory");

        DefaultKafkaConsumerFactory<String, String> defaultFactory = (DefaultKafkaConsumerFactory<String, String>) factory.getConsumerFactory();
        Map<String, Object> props = defaultFactory.getConfigurationProperties();

        assertEquals("host.test:1234", props.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals("test-group", props.get(ConsumerConfig.GROUP_ID_CONFIG));
    }

    @Test
    void kafkaErrorHandlerConfigured() throws IllegalAccessException {
        KafkaConsumerConfig config = new KafkaConsumerConfig();

        // set retry/backoff properties
        ReflectionTestUtils.setField(config, "maxRetryAttempts", 5);
        ReflectionTestUtils.setField(config, "initialInterval", 200L);
        ReflectionTestUtils.setField(config, "multiplier", 1.5);
        ReflectionTestUtils.setField(config, "maxInterval", 2000L);

        // create a simple KafkaTemplate for the recoverer
        KafkaTemplate<String, String> template = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(new HashMap<>()));

        DefaultErrorHandler handler = config.kafkaErrorHandler(template);
        assertNotNull(handler, "DefaultErrorHandler should be created");
        assertInstanceOf(DefaultErrorHandler.class, handler, "Should be DefaultErrorHandler");
    }

    private Object findFieldValue(Object target, Class<?> fieldType) throws IllegalAccessException {
        Class<?> cls = target.getClass();
        // first pass: find exact or assignable type
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                if (fieldType.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object val = f.get(target);
                    if (val != null) {
                        return val;
                    }
                }
            }
            cls = cls.getSuperclass();
        }
        // second pass: fallback to name heuristics but ensure instance matches
        cls = target.getClass();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                String name = f.getName().toLowerCase();
                if (name.contains(fieldType.getSimpleName().toLowerCase()) || name.contains("recover") || name.contains("backoff") || name.contains("notretry") || name.contains("notretryable") || name.contains("retry")) {
                    f.setAccessible(true);
                    Object val = f.get(target);
                    if (val != null && fieldType.isInstance(val)) {
                        return val;
                    }
                }
            }
            cls = cls.getSuperclass();
        }
        return null;
    }
}
