package com.pbkour.mintrade.execution.services;

import com.pbkour.mintrade.commons.db.FillEntity;
import com.pbkour.mintrade.commons.db.FillsRepository;
import com.pbkour.mintrade.commons.kafka.Order;
import com.pbkour.mintrade.commons.kafka.OrdersCreated;
import com.pbkour.mintrade.commons.orders.Side;
import com.pbkour.mintrade.commons.orders.Symbol;
import com.pbkour.mintrade.commons.orders.Type;
import com.pbkour.mintrade.execution.generators.PriceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderFillService {
    private final PriceGenerator priceGenerator;
    private final FillsRepository fillsRepository;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public void fillOrder(OrdersCreated payload) {
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

    public record FillSavedEvent(FillEntity fill, UUID accountId, Symbol symbol) {
    }
}
