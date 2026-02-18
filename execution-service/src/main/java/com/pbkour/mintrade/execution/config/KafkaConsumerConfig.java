package com.pbkour.mintrade.execution.config;

import com.fasterxml.jackson.databind.JsonMappingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@EnableKafka
@Configuration
public class KafkaConsumerConfig {
    @Value("${spring.kafka.bootstrap-servers:${KAFKA_BOOTSTRAP_SERVERS:host.docker.internal:9092}}")
    private String bootstrapAddress;

    @Value("${spring.kafka.consumer.group-id:${KAFKA_CONSUMER_GROUP_ID:default-group}}")
    private String groupId;

    @Value("${kafka.consumer.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${kafka.consumer.retry.initial-interval:500}")
    private long initialInterval;

    @Value("${kafka.consumer.retry.multiplier:2.0}")
    private double multiplier;

    @Value("${kafka.consumer.retry.max-interval:5000}")
    private long maxInterval;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
            bootstrapAddress);
        props.put(
            ConsumerConfig.GROUP_ID_CONFIG,
            groupId);
        props.put(
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            StringDeserializer.class);
        props.put(
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
    kafkaListenerContainerFactory(DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(kafkaErrorHandler);

        return factory;
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        DeadLetterPublishingRecoverer recoverer =
            new DeadLetterPublishingRecoverer(template, (consumerRecord, ex) ->
                new TopicPartition(consumerRecord.topic() + ".dlq", consumerRecord.partition())
            );

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(maxRetryAttempts);
        backOff.setInitialInterval(initialInterval);
        backOff.setMultiplier(multiplier);
        backOff.setMaxInterval(maxInterval);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

        handler.addNotRetryableExceptions(IllegalArgumentException.class, JsonMappingException.class);

        handler.setRetryListeners(
            (consumerRecord, ex, deliveryAttempt) -> log.info("Retry attempt {} for topic={} partition={} offset={}",
                deliveryAttempt, consumerRecord.topic(), consumerRecord.partition(), consumerRecord.offset()));

        return handler;
    }
}
