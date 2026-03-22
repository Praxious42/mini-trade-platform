package com.pbkour.mintrade.portfolio.entities;

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
@Table(name = "account_limits")
public class AccountLimitEntity {
    @Id
    @Column(name = "account_id", nullable = false, unique = true)
    private UUID accountId;
    @Column(name = "max_notional", precision = 19, scale = 4, nullable = false)
    private BigDecimal maxNotional;
    @Column(name = "max_pos_per_symbol", precision = 19, scale = 4, nullable = false)
    private BigDecimal maxPosPerSymbol;
    @Column(name = "margin_rate_fx", precision = 6, scale = 5, nullable = false)
    private BigDecimal marginRateFx;
    @Column(name = "margin_rate_stock", precision = 6, scale = 5, nullable = false)
    private BigDecimal marginRateStock;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @PrePersist
    public void prePersist() throws AccountLimitEntityValidationException {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        validate();
    }

    @PreUpdate
    public void preUpdate() throws AccountLimitEntityValidationException {
        updatedAt = Instant.now();
        validate();
    }

    private void validate() throws AccountLimitEntityValidationException {
        if (maxNotional != null && maxNotional.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AccountLimitEntityValidationException();
        }
        if (maxPosPerSymbol != null && maxPosPerSymbol.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AccountLimitEntityValidationException();
        }
        if (marginRateFx != null && (marginRateFx.compareTo(BigDecimal.ZERO) <= 0 || marginRateFx.compareTo(BigDecimal.ONE) > 0)) {
            throw new AccountLimitEntityValidationException();
        }
        if (marginRateStock != null && (marginRateStock.compareTo(BigDecimal.ZERO) <= 0 || marginRateStock.compareTo(BigDecimal.ONE) > 0)) {
            throw new AccountLimitEntityValidationException();
        }
    }

    @StandardException
    public static class AccountLimitEntityValidationException extends RuntimeException {
    }
}
