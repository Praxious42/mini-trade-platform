package com.pbkour.mintrade.order.kafka;

import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
public class KafkaTopicCreator implements ApplicationRunner {
    @Value("${kafka.topic.names}")
    private List<String> topicNames;

    @Value("${kafka.topic.partitions:3}")
    private int partitions;

    @Value("${kafka.topic.replication-factor:1}")
    private short replicationFactor;

    @Value("${spring.kafka.bootstrap-servers:${KAFKA_BOOTSTRAP_SERVERS:host.docker.internal:9092}}")
    private String bootstrapServers;

    @Override
    public void run(@Nonnull ApplicationArguments args) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        if (topicNames == null || topicNames.isEmpty()) {
            log.warn("No Kafka topic names configured; skipping topic creation");
            return;
        }

        log.info("KafkaTopicCreator connecting to {} to ensure topics {} exist", bootstrapServers, topicNames);

        try (AdminClient admin = AdminClient.create(props)) {
            Set<String> topics = admin.listTopics(new ListTopicsOptions().listInternal(true)).names().get();
            if (topics.containsAll(topicNames)) {
                log.info("Topics {} already exist; skipping creation", topicNames);
                return;
            }

            List<NewTopic> newTopics = topicNames.stream().filter(s -> !topics.contains(s))
                .map(s -> new NewTopic(s, partitions, replicationFactor))
                .toList();

            CreateTopicsResult result = admin.createTopics(newTopics);

            Collection<KafkaFuture<Void>> futures = result.values().values();
            KafkaFuture.allOf(futures.toArray(new KafkaFuture[0])).get();

            log.info("Created topics={} with partitions={} replicationFactor={}",
                newTopics.stream().map(NewTopic::name).toList(), partitions, replicationFactor);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while creating topics {}", topicNames, ie);
        } catch (ExecutionException ee) {
            Throwable t = ee.getCause() != null ? ee.getCause() : ee;
            log.error("Error creating topics {}: {}", topicNames, t.getMessage(), t);
        } catch (Exception e) {
            log.error("Unexpected error while creating topics {}", topicNames, e);
        }
    }
}
