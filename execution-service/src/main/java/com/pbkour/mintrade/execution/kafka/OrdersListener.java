package com.pbkour.mintrade.execution.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.commons.kafka.OrdersCreated;
import com.pbkour.mintrade.execution.services.OrderFillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import static java.util.Optional.ofNullable;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrdersListener {
    private final ObjectMapper objectMapper;
    private final OrderFillService orderFillService;

    @KafkaListener(topics = "orders.created")
    public void onOrdersCreated(String message, @Header(name = "kafka_receivedMessageKey", required = false) String key) {
        try {
            if (message == null || message.isEmpty()) {
                log.warn("[OrdersListener] received empty orders.created message with key={}", key);
                return;
            }

            OrdersCreated payload = objectMapper.readValue(message, OrdersCreated.class);
            log.info("[OrdersListener] received orders.created key={} orderId={} payload={}",
                key, payload != null && payload.getOrder() != null ? payload.getOrder().getOrderId() : "<null>", payload);

            ofNullable(payload).ifPresentOrElse(
                orderFillService::fillOrder,
                () -> log.warn("[OrdersListener] received null payload for orders.created key={}", key)
            );

        } catch (Exception e) {
            log.error("[OrdersListener] failed to process orders.created key={}", key, e);
        }
    }

    @KafkaListener(topics = "orders.created.dlq")
    public void onDlq(ConsumerRecord<String, String> consumerRecord) {
        log.info("[OrdersListenerDLQ] received orders.created.dlq consumerRecord={}", consumerRecord);
    }
}
