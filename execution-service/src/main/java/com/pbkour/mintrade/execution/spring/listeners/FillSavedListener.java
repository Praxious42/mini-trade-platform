package com.pbkour.mintrade.execution.spring.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.commons.kafka.Fill;
import com.pbkour.mintrade.commons.kafka.OrdersFilled;
import com.pbkour.mintrade.execution.services.OrderFillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
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
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper mapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSaved(OrderFillService.FillsSavedEvent e) {
        log.info("Received FillsSavedEvent for orderId={}, preparing OrdersFilled event: {}", e.orderId(), e);
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
            .accountId(e.accountId())
            .symbol(e.symbol())
            .orderId(e.orderId())
            .fills(fills)
            .build();

        try {
            kafkaTemplate.send("orders.filled", payload.getEventId().toString(), mapper.writeValueAsString(payload));
            log.info("Order filled event sent to topic orders.filled for orderId={}", e.orderId());
        } catch (Exception ex) {
            log.error("Failed to serialize or send OrdersFilled for orderId={}", e.orderId(), ex);
        }
    }
}
