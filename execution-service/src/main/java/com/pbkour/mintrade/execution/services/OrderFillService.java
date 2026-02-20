package com.pbkour.mintrade.execution.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.commons.db.FillEntity;
import com.pbkour.mintrade.commons.db.FillsRepository;
import com.pbkour.mintrade.commons.kafka.Order;
import com.pbkour.mintrade.commons.kafka.OrdersCreated;
import com.pbkour.mintrade.commons.kafka.OrdersRejected;
import com.pbkour.mintrade.commons.orders.*;
import com.pbkour.mintrade.execution.generators.ExecutionDecisionDecider;
import com.pbkour.mintrade.execution.generators.PriceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderFillService {
    private final PriceGenerator priceGenerator;
    private final FillsRepository fillsRepository;
    private final ApplicationEventPublisher publisher;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper mapper;

    @Transactional
    public void fillOrder(OrdersCreated payload) {
        boolean accepted = ExecutionDecisionDecider.generateExecutionDecision().equals(ExecutionDecision.ACCEPTED);
        if (!accepted) {
            log.info("Rejecting order {} for symbol {}", payload.getOrder().getOrderId(), payload.getOrder().getSymbol());
            rejectOrder(payload);
            return;
        }
        Order order = payload.getOrder();
        BigDecimal price = priceGenerator.generatePrice(order.getSymbol());

        if (order.getType() == Type.LIMIT && ((order.getSide().equals(Side.BUY) && price.compareTo(order.getLimitPrice()) > 0)
            || (order.getSide().equals(Side.SELL) && price.compareTo(order.getLimitPrice()) < 0))) {
            log.info("NOT filling order {} with price {}", order.getSymbol(), price);
            return;
        }

        log.info("Filling order {} with price {}", order.getSymbol(), price);
        FillEntity saved = fillsRepository.save(FillEntity.builder()
            .orderId(order.getOrderId())
            .quantity(order.getQuantity())
            .price(price)
            .build());

        publisher.publishEvent(new FillSavedEvent(saved, order.getAccountId(), order.getSymbol()));
    }

    private void rejectOrder(OrdersCreated payload) {
        OrdersRejected ordersRejected = OrdersRejected.builder()
            .eventId(UUID.randomUUID())
            .occurredAt(Instant.now())
            .orderId(payload.getOrder().getOrderId())
            .reason(RejectionReason.RANDOM_FAILURE)
            .build();

        try {
            kafkaTemplate.send("orders.rejected", ordersRejected.getEventId().toString(), mapper.writeValueAsString(ordersRejected));
            log.info("Order rejected event sent to topic orders.rejected for orderId={}", ordersRejected.getOrderId());
        } catch (Exception ex) {
            log.error("Failed to serialize or send OrdersRejected for orderId={}", ordersRejected.getOrderId(), ex);
        }
    }

    public record FillSavedEvent(FillEntity fill, UUID accountId, Symbol symbol) {
    }
}
