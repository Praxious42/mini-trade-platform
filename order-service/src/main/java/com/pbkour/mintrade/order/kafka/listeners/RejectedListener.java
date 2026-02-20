package com.pbkour.mintrade.order.kafka.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.commons.kafka.OrdersRejected;
import com.pbkour.mintrade.order.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RejectedListener {
    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    @KafkaListener(topics = "orders.rejected")
    public void onOrdersRejected(String message, @Header(name = "kafka_receivedMessageKey", required = false) String key) {
        try {
            OrdersRejected payload = objectMapper.readValue(message, OrdersRejected.class);
            log.info("Received OrdersRejected Message: {}", payload);

            orderService.rejectOrder(payload);
        } catch (Exception e) {
            log.error("[RejectedListener] failed to process orders.rejected key={}", key, e);
        }
    }


    @KafkaListener(topics = "orders.rejected.dlq")
    public void onDlq(ConsumerRecord<String, String> consumerRecord) {
        log.info("[RejectedListenerDLQ] received orders.rejected.dlq consumerRecord={}", consumerRecord);
    }
}
