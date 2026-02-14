package com.pbkour.mintrade.contracts.db;

import com.pbkour.mintrade.contracts.dto.Order;
import com.pbkour.mintrade.contracts.orders.Side;
import com.pbkour.mintrade.contracts.orders.Status;
import com.pbkour.mintrade.contracts.orders.Symbol;
import com.pbkour.mintrade.contracts.orders.Type;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    @Column(name = "qty", nullable = false)
    private Long quantity;
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

    @PrePersist
    public void prePersistUpdate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public Order mapToOrder() {
        return Order.builder()
            .accountId(this.accountId)
            .symbol(this.symbol)
            .side(this.side)
            .type(this.type)
            .quantity(this.quantity)
            .limitPrice(this.limitPrice)
            .build();
    }
}
