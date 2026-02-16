package com.pbkour.mintrade.contracts.kafka;

import com.pbkour.mintrade.contracts.db.OrderEntity;
import com.pbkour.mintrade.contracts.orders.Side;
import com.pbkour.mintrade.contracts.orders.Symbol;
import com.pbkour.mintrade.contracts.orders.Type;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class Order {
    private UUID orderId;
    private UUID accountId;
    private Symbol symbol;
    private Side side;
    private Type type;
    private Long quantity;
    private BigDecimal limitPrice;

    public static Order mapToOrder(OrderEntity orderEntity) {
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
