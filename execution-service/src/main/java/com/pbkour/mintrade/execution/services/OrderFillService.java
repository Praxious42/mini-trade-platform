package com.pbkour.mintrade.execution.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.commons.generators.PriceGenerator;
import com.pbkour.mintrade.commons.kafka.Order;
import com.pbkour.mintrade.commons.kafka.OrdersCreated;
import com.pbkour.mintrade.commons.kafka.OrdersRejected;
import com.pbkour.mintrade.commons.orders.ExecutionDecision;
import com.pbkour.mintrade.commons.orders.RejectionReason;
import com.pbkour.mintrade.commons.orders.Side;
import com.pbkour.mintrade.commons.orders.Type;
import com.pbkour.mintrade.commons.services.ProcessedEventRecorder;
import com.pbkour.mintrade.execution.entities.FillEntity;
import com.pbkour.mintrade.execution.generators.ExecutionDecider;
import com.pbkour.mintrade.execution.repositories.FillsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
    private final ProcessedEventRecorder processedEventRecorder;

    private static boolean isUnfavorableLimit(Order order, BigDecimal price) {
        return order.getType() == Type.LIMIT && ((order.getSide().equals(Side.BUY) && price.compareTo(order.getLimitPrice()) > 0)
            || (order.getSide().equals(Side.SELL) && price.compareTo(order.getLimitPrice()) < 0));
    }

    @Transactional
    public void fillOrder(OrdersCreated payload) {
        UUID eventId = payload.getEventId();
        processedEventRecorder.processIfNotProcessed(eventId, "OrdersCreated", () -> {
            boolean accepted = ExecutionDecider.generateExecutionDecision().equals(ExecutionDecision.ACCEPTED);
            if (!accepted) {
                log.info("Rejecting order {} for symbol {}", payload.getOrder().getOrderId(), payload.getOrder().getSymbol());
                rejectOrder(payload);
                return;
            }
            Order order = payload.getOrder();
            BigDecimal price = priceGenerator.generatePrice(order.getSymbol());

            if (isUnfavorableLimit(order, price)) {
                log.info("NOT filling order {} with price {}", order.getSymbol(), price);
                return;
            }

            log.info("Filling order {} with price {}", order.getSymbol(), price);
            List<BigDecimal> partialFills = ExecutionDecider.getPartialFills(order.getQuantity());
            log.info("Fill quantities: {}", partialFills);
            List<FillEntity> fillEntities = partialFills.stream().map(qty -> FillEntity.builder()
                .orderId(order.getOrderId())
                .quantity(qty)
                .price(price)
                .build()).toList();

            List<FillEntity> savedFillEntities = fillsRepository.saveAll(fillEntities);

            publisher.publishEvent(new FillsSavedEvent(savedFillEntities, order));
        });
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

    public record FillsSavedEvent(List<FillEntity> fills, Order order) {
    }
}
