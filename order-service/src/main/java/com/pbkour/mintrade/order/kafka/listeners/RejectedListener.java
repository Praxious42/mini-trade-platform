package com.pbkour.mintrade.order.kafka.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.commons.kafka.OrdersRejected;
import com.pbkour.mintrade.commons.repositories.ProcessedEventsRepository;
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
    private final ObjectMapper objectMapper;
    private final OrderService orderService;
    private final ProcessedEventsRepository processedEventsRepository;


    @KafkaListener(topics = "orders.rejected")
    public void onOrdersRejected(String message, @Header(name = "kafka_receivedMessageKey", required = false) String key) {
        try {
            OrdersRejected payload = objectMapper.readValue(message, OrdersRejected.class);

            if (payload == null) {
                log.error("[RejectedListener] received null payload for orders.rejected key={}", key);
                throw new RejectedListenerException("Received null payload for orders.rejected");
            }

            log.info("Received OrdersRejected Message: {}", payload);

            processedEventsRepository.findById(payload.getEventId())
                .ifPresent(processedEventEntity -> {
                    throw new EventAlreadyProcessedException("Event already processed for eventId=" + key);
                });

            orderService.rejectOrder(payload);
        } catch (EventAlreadyProcessedException e) {
            log.error("[RejectedListener] event already processed for orders.rejected key={}", key, e);
            throw new RejectedListenerException(e.getMessage(), e);
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
    public static class EventAlreadyProcessedException extends RuntimeException {
    }

    @StandardException
    public static class RejectedListenerException extends RuntimeException {
    }
}
