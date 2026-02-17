package com.pbkour.mintrade.execution.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.contracts.kafka.OrdersCreated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrdersListener {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "orders.created")
    public void onOrdersCreated(String message, @Header(name = "kafka_receivedMessageKey", required = false) String key) {
        try {
            OrdersCreated payload = objectMapper.readValue(message, OrdersCreated.class);
            log.info("[OrdersListener] received orders.created key={} orderId={} payload={}",
                key, payload != null && payload.getOrder() != null ? payload.getOrder().getOrderId() : "<null>", payload);


        } catch (Exception e) {
            log.error("[OrdersListener] failed to process orders.created key={}", key, e);
            throw new RuntimeException(e);
        }
    }

}
