package com.pbkour.mintrade.order.kafka.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.commons.kafka.OrdersFilled;
import com.pbkour.mintrade.order.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.StandardException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FillsListener {
    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    @KafkaListener(topics = "orders.filled", groupId = "order-service-group")
    public void onOrdersFilled(String message, @Header(name = "kafka_receivedMessageKey", required = false) String key) {
        try {
            OrdersFilled payload = objectMapper.readValue(message, OrdersFilled.class);

            if (payload == null) {
                log.error("[FillsListener] received null payload for orders.filled key={}", key);
                throw new FillsListenerException("Received null payload for orders.filled");
            }

            log.info("Received OrdersFilled Message: {}", payload);

            orderService.updateFilledOrder(payload);
        } catch (Exception e) {
            log.error("[FillsListener] failed to process orders.filled key={}", key, e);
            throw new FillsListenerException(e.getMessage(), e);
        }
    }


    @KafkaListener(topics = "orders.filled.dlq")
    public void onDlq(ConsumerRecord<String, String> consumerRecord) {
        log.info("[FillsListenerDLQ] received orders.filled.dlq consumerRecord={}", consumerRecord);
    }

    @StandardException
    public static class FillsListenerException extends RuntimeException {
    }
}
