package com.pbkour.mintrade.order.services;

import com.pbkour.mintrade.commons.dto.Order;
import com.pbkour.mintrade.commons.orders.Status;
import com.pbkour.mintrade.order.entities.OrderEntity;
import com.pbkour.mintrade.order.repositories.OrdersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RejectedOrderService {
    private final OrdersRepository ordersRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistRejectedOrder(Order order) {
        OrderEntity entity = OrderEntity.builder()
            .accountId(order.getAccountId())
            .symbol(order.getSymbol())
            .side(order.getSide())
            .type(order.getType())
            .quantity(order.getQuantity())
            .limitPrice(order.getLimitPrice())
            .status(Status.REJECTED)
            .version(0)
            .build();

        ordersRepository.save(entity);
        ordersRepository.flush();
    }
}

