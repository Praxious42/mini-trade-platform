package com.pbkour.mintrade.order.spring.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.commons.kafka.Order;
import com.pbkour.mintrade.commons.kafka.OrdersCreated;
import com.pbkour.mintrade.order.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSavedListener {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper mapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSaved(OrderService.OrderSavedEvent e) {
        OrdersCreated payload = OrdersCreated.builder()
            .eventId(UUID.randomUUID())
            .occurredAt(e.order().getCreatedAt())
            .order(Order.mapToOrder(e.order()))
            .build();

        try {
            kafkaTemplate.send("orders.created", payload.getEventId().toString(), mapper.writeValueAsString(payload));
            log.info("Order saved event sent to topic orders-created for orderId={}", e.order().getId());
        } catch (Exception ex) {
            log.error("Failed to serialize or send OrdersCreated for orderId={}", e.order().getId(), ex);
        }
    }
}
