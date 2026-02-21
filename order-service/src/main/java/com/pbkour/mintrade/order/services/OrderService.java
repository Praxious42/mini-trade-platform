package com.pbkour.mintrade.order.services;

import com.pbkour.mintrade.commons.db.OrderEntity;
import com.pbkour.mintrade.commons.db.OrdersRepository;
import com.pbkour.mintrade.commons.dto.Order;
import com.pbkour.mintrade.commons.kafka.OrdersFilled;
import com.pbkour.mintrade.commons.kafka.OrdersRejected;
import com.pbkour.mintrade.commons.orders.Status;
import lombok.RequiredArgsConstructor;
import lombok.experimental.StandardException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrdersRepository ordersRepository;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public UUID createOrder(Order order) {
        OrderEntity entity = OrderEntity.builder()
            .accountId(order.getAccountId())
            .symbol(order.getSymbol())
            .side(order.getSide())
            .type(order.getType())
            .quantity(order.getQuantity())
            .limitPrice(order.getLimitPrice())
            .status(Status.NEW)
            .version(0)
            .build();

        OrderEntity savedOrder = ordersRepository.save(entity);

        publisher.publishEvent(new OrderSavedEvent(savedOrder));

        return savedOrder.getId();
    }

    public Order getOrder(UUID id) {
        return ordersRepository.findById(id)
            .map(OrderEntity::mapToOrder)
            .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
    }

    public List<Order> getAccountOrders(UUID accountId, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "updatedAt");
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        return ordersRepository.findByAccountId(accountId, pageRequest).getContent().stream().map(OrderEntity::mapToOrder).toList();
    }

    public void cancelOrder(UUID id) {
        Optional.of(ordersRepository.getReferenceById(id))
            .ifPresent(order -> {
                order.setStatus(Status.CANCELLED);
                ordersRepository.save(order);
            });
    }

    @Transactional
    public void updateFilledOrder(OrdersFilled ordersFilled) {
        UUID orderId = ordersFilled.getOrderId();
        ordersRepository.findById(orderId).ifPresentOrElse(
            order -> {
                order.setStatus(ordersFilled.getFills().size() > 1 ? Status.PARTIALLY_FILLED : Status.FILLED);
                ordersRepository.save(order);
            },
            () -> log.warn("Received OrdersFilled event for non-existent orderId={}", orderId)
        );
    }

    @Transactional
    public void rejectOrder(OrdersRejected ordersRejected) {
        UUID orderId = ordersRejected.getOrderId();
        ordersRepository.findById(orderId).ifPresentOrElse(
            order -> {
                order.setStatus(Status.REJECTED);
                ordersRepository.save(order);
            },
            () -> log.warn("Received OrdersRejected event for non-existent orderId={}", orderId)
        );
    }

    public record OrderSavedEvent(OrderEntity order) {
    }

    @StandardException
    public static class OrderNotFoundException extends RuntimeException {
    }
}
