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

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FillSavedListener {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper mapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSaved(OrderFillService.FillSavedEvent e) {
        OrdersFilled payload = OrdersFilled.builder()
            .eventId(e.fill().getId())
            .occurredAt(e.fill().getTimestamp())
            .accountId(e.accountId())
            .symbol(e.symbol())
            .orderId(e.fill().getOrderId())
            .fills(List.of(Fill.builder()
                .fillId(e.fill().getId())
                .quantity(e.fill().getQuantity())
                .price(e.fill().getPrice())
                .timestamp(e.fill().getTimestamp())
                .build()))
            .build();

        try {
            kafkaTemplate.send("orders.filled", payload.getEventId().toString(), mapper.writeValueAsString(payload));
            log.info("Order filled event sent to topic orders.filled for orderId={}", e.fill().getId());
        } catch (Exception ex) {
            log.error("Failed to serialize or send OrdersFilled for orderId={}", e.fill().getId(), ex);
        }
    }
}
