package com.pbkour.mintrade.order.services;

import com.pbkour.mintrade.contracts.db.OrderEntity;
import com.pbkour.mintrade.contracts.dto.Order;
import com.pbkour.mintrade.contracts.orders.Status;
import com.pbkour.mintrade.order.repositories.OrdersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrdersRepository ordersRepository;

    public UUID createOrder(Order order) {
        OrderEntity entity = OrderEntity.builder()
            .accountId(order.getAccountId())
            .symbol(order.getSymbol())
            .side(order.getSide())
            .type(order.getType())
            .quantity(order.getQuantity())
            .limitPrice(order.getLimitPrice())
            .status(Status.NEW)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .version(0)
            .build();

        OrderEntity savedOrder = ordersRepository.save(entity);

        return savedOrder.getId();
    }

    public Order getOrder(UUID id) {
        return ordersRepository.getReferenceById(id).mapToOrder();
    }

    public List<Order> getAccountOrders(UUID accountId) {
        return ordersRepository.findByAccountId(accountId).stream().map(OrderEntity::mapToOrder).toList();
    }

    public void cancelOrder(UUID id) {
        Optional.of(ordersRepository.getReferenceById(id))
            .ifPresent(order -> {
                order.setStatus(Status.CANCELLED);
                order.setUpdatedAt(Instant.now());
                ordersRepository.save(order);
            });
    }
}
