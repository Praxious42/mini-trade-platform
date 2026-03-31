package com.pbkour.mintrade.order.kafka.listeners;

import com.pbkour.mintrade.commons.kafka.KafkaJsonListenerSupport;
import com.pbkour.mintrade.commons.kafka.OrdersRejected;
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
public class RejectedListener {
    private final KafkaJsonListenerSupport kafkaJsonListenerSupport;
    private final OrderService orderService;

    @KafkaListener(topics = "orders.rejected")
    public void onOrdersRejected(String message, @Header(name = "kafka_receivedMessageKey", required = false) String key) {
        try {
            OrdersRejected payload = kafkaJsonListenerSupport.deserialize(message, OrdersRejected.class);

            if (payload == null) {
                log.error("[RejectedListener] received null payload for orders.rejected key={}", key);
                throw new RejectedListenerException("Received null payload for orders.rejected");
            }

            log.info("Received OrdersRejected Message: {}", payload);

            orderService.rejectOrder(payload);
        } catch (Exception e) {
            log.error("[RejectedListener] failed to process orders.rejected key={}", key, e);
            throw new RejectedListenerException(e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "orders.rejected.dlq")
    public void onDlq(ConsumerRecord<String, String> consumerRecord) {
        log.info("[RejectedListenerDLQ] received orders.rejected.dlq consumerRecord={}", consumerRecord);
    }

    @StandardException
    public static class RejectedListenerException extends RuntimeException {
    }
}
