package com.pbkour.mintrade.execution.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.commons.kafka.OrdersCreated;
import com.pbkour.mintrade.execution.services.OrderFillService;
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
public class OrdersListener {
    private final ObjectMapper objectMapper;
    private final OrderFillService orderFillService;

    @KafkaListener(topics = "orders.created", groupId = "execution-service-group")
    public void onOrdersCreated(String message, @Header(name = "kafka_receivedMessageKey", required = false) String key) {
        try {
            if (message == null || message.isEmpty()) {
                log.warn("[OrdersListener] received empty orders.created message with key={}", key);
                return;
            }

            OrdersCreated payload = objectMapper.readValue(message, OrdersCreated.class);
            if (payload == null) {
                log.warn("[OrdersListener] failed to deserialize orders.created message with key={}", key);
                throw new OrdersListenerException("Failed to deserialize orders.created message");
            }

            log.info("[OrdersListener] received orders.created key={} orderId={} payload={}",
                key, payload.getOrder() != null ? payload.getOrder().getOrderId() : "<null>", payload);

            orderFillService.fillOrder(payload);
        } catch (Exception e) {
            log.error("[OrdersListener] failed to process orders.created key={}", key, e);
            throw new OrdersListenerException(e);
        }
    }

    @KafkaListener(topics = "orders.created.dlq")
    public void onDlq(ConsumerRecord<String, String> consumerRecord) {
        log.info("[OrdersListenerDLQ] received orders.created.dlq consumerRecord={}", consumerRecord);
    }

    @StandardException
    public static class OrdersListenerException extends RuntimeException {
    }
}
