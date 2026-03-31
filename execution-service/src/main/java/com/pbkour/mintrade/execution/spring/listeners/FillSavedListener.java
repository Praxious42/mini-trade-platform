package com.pbkour.mintrade.execution.spring.listeners;

import com.pbkour.mintrade.commons.kafka.KafkaJsonPublisherSupport;
import com.pbkour.mintrade.commons.kafka.Fill;
import com.pbkour.mintrade.commons.kafka.Order;
import com.pbkour.mintrade.commons.kafka.OrdersFilled;
import com.pbkour.mintrade.execution.services.OrderFillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FillSavedListener {
    private final KafkaJsonPublisherSupport kafkaJsonPublisherSupport;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSaved(OrderFillService.FillsSavedEvent e) {
        Order order = e.order();
        log.info("Received FillsSavedEvent for orderId={}, preparing OrdersFilled event: {}", order.getOrderId(), e);
        List<Fill> fills = e.fills().stream().map(fillEntity ->
            Fill.builder()
                .fillId(fillEntity.getId())
                .quantity(fillEntity.getQuantity())
                .price(fillEntity.getPrice())
                .timestamp(fillEntity.getTimestamp())
                .build()).toList();

        OrdersFilled payload = OrdersFilled.builder()
            .eventId(UUID.randomUUID())
            .occurredAt(Instant.now())
            .accountId(order.getAccountId())
            .symbol(order.getSymbol())
            .side(order.getSide())
            .orderId(order.getOrderId())
            .fills(fills)
            .build();

        kafkaJsonPublisherSupport.publish("orders.filled", payload);
    }
}
