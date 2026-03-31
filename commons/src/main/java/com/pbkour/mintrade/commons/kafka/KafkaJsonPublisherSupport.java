package com.pbkour.mintrade.commons.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaJsonPublisherSupport {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publish(String topic, KafkaPayload payload) {
        if (payload == null) {
            log.warn("Skipping Kafka publish to topic={} because payload is null", topic);
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, payload.getEventId().toString(), json);
            log.info("Sent {} event to topic {} for eventId={}", payload.getClass().getSimpleName(), topic, payload.getEventId());
        } catch (Exception ex) {
            log.error("Failed to serialize or send {} to topic={} eventId={}",
                payload.getClass().getSimpleName(), topic, payload.getEventId(), ex);
        }
    }
}


