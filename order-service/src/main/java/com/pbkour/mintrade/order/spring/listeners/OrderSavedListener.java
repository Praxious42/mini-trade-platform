package com.pbkour.mintrade.order.spring.listeners;

import com.pbkour.mintrade.commons.kafka.KafkaJsonPublisherSupport;
import com.pbkour.mintrade.commons.kafka.Order;
import com.pbkour.mintrade.commons.kafka.OrdersCreated;
import com.pbkour.mintrade.order.entities.OrderEntity;
import com.pbkour.mintrade.order.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSavedListener {
    private final KafkaJsonPublisherSupport kafkaJsonPublisherSupport;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSaved(OrderService.OrderSavedEvent e) {
        OrdersCreated payload = OrdersCreated.builder()
            .eventId(UUID.randomUUID())
            .occurredAt(e.order().getCreatedAt())
            .order(mapToOrder(e.order()))
            .build();

        kafkaJsonPublisherSupport.publish("orders.created", payload);
    }

    private Order mapToOrder(OrderEntity orderEntity) {
        return Order.builder()
            .orderId(orderEntity.getId())
            .accountId(orderEntity.getAccountId())
            .symbol(orderEntity.getSymbol())
            .side(orderEntity.getSide())
            .type(orderEntity.getType())
            .quantity(orderEntity.getQuantity())
            .limitPrice(orderEntity.getLimitPrice())
            .build();
    }
}
