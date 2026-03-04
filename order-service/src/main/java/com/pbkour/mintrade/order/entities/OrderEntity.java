package com.pbkour.mintrade.order.entities;

import com.pbkour.mintrade.commons.dto.Order;
import com.pbkour.mintrade.commons.orders.Side;
import com.pbkour.mintrade.commons.orders.Status;
import com.pbkour.mintrade.commons.orders.Symbol;
import com.pbkour.mintrade.commons.orders.Type;
import com.pbkour.mintrade.commons.responses.OrderResponse;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.StandardException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Access(AccessType.FIELD)
@Table(name = "orders")
public class OrderEntity {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(name = "account_id", nullable = false)
    private UUID accountId;
    @Enumerated(EnumType.STRING)
    @Column(name = "symbol", nullable = false)
    private Symbol symbol;
    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false)
    private Side side;
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private Type type;
    @Column(name = "qty", precision = 19, scale = 4, nullable = false)
    private BigDecimal quantity;
    @Column(name = "limit_price", precision = 18, scale = 8)
    private BigDecimal limitPrice;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    @Column(name = "version", nullable = false)
    private int version;

    public static OrderResponse mapToOrderResponse(OrderEntity orderEntity) {
        return OrderResponse.builder()
            .orderId(orderEntity.getId())
            .accountId(orderEntity.getAccountId())
            .symbol(orderEntity.getSymbol())
            .side(orderEntity.getSide())
            .type(orderEntity.getType())
            .quantity(orderEntity.getQuantity())
            .limitPrice(orderEntity.getLimitPrice())
            .status(orderEntity.getStatus())
            .createdAt(orderEntity.getCreatedAt())
            .updatedAt(orderEntity.getUpdatedAt())
            .build();
    }

    @PrePersist
    public void prePersistUpdate() throws OrderEntityValidationException {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        validate();
    }

    @PreUpdate
    public void preUpdate() throws OrderEntityValidationException {
        updatedAt = Instant.now();
        validate();
    }

    public Order mapToOrder() {
        return Order.builder()
            .accountId(this.accountId)
            .symbol(this.symbol)
            .side(this.side)
            .type(this.type)
            .quantity(this.quantity)
            .limitPrice(this.limitPrice)
            .createdAt(this.createdAt)
            .updatedAt(this.updatedAt)
            .build();
    }

    private void validate() throws OrderEntityValidationException {
        if (accountId == null) {
            throw new OrderEntityValidationException("Account ID must be specified");
        }
        if (symbol == null) {
            throw new OrderEntityValidationException("Symbol must be specified");
        }
        if (side == null) {
            throw new OrderEntityValidationException("Side must be specified");
        }
        if (type == null) {
            throw new OrderEntityValidationException("Type must be specified");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderEntityValidationException("Quantity must be a positive number");
        }
        if (type == Type.LIMIT && (limitPrice == null || limitPrice.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new OrderEntityValidationException("Limit price must be a positive number for limit orders");
        }
        if (status == null) {
            throw new OrderEntityValidationException("Status must be specified");
        }
    }

    @StandardException
    public static class OrderEntityValidationException extends RuntimeException {
    }
}
