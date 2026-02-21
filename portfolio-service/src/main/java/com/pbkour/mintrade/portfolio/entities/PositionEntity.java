package com.pbkour.mintrade.portfolio.entities;

import com.pbkour.mintrade.commons.orders.Symbol;
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
@Table(name = "positions")
public class PositionEntity {
    @EmbeddedId
    private PositionId id;

    @Column(name = "net_qty", precision = 19, scale = 4, nullable = false)
    private BigDecimal netQty;

    @Column(name = "avg_price", precision = 19, scale = 5, nullable = false)
    private BigDecimal avgPrice;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        updatedAt = Instant.now();
        validate();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
        validate();
    }

    private void validate() {
        if (id == null) {
            throw new PositionValidationException("id is required");
        }
        if (id.getAccountId() == null) {
            throw new PositionValidationException("accountId is required");
        }
        if (id.getSymbol() == null) {
            throw new PositionValidationException("symbol is required");
        }
        if (netQty == null) {
            throw new PositionValidationException("netQty is required");
        }
        if (avgPrice == null) {
            throw new PositionValidationException("avgPrice is required");
        }

        boolean netNotZero = netQty.compareTo(BigDecimal.ZERO) != 0;
        boolean avgIsZero = avgPrice.compareTo(BigDecimal.ZERO) == 0;

        if (!netNotZero && !avgIsZero) {
            throw new PositionValidationException("if you have no position, average price should reset to 0");
        }

        if (avgPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new PositionValidationException("avgPrice must be >= 0");
        }
    }

    @StandardException
    public static class PositionValidationException extends RuntimeException {
    }

    @Data
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionId {
        @Column(name = "account_id", nullable = false)
        private UUID accountId;

        @Column(name = "symbol", nullable = false, length = 32)
        private Symbol symbol;
    }
}
