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
@Table(name = "accounts")
public class AccountEntity {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(name = "equity", precision = 18, scale = 8, nullable = false)
    private BigDecimal equity;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() throws AccountEntityValidationException {
        createdAt = Instant.now();
        validate();
    }

    @PreUpdate
    public void preUpdate() throws AccountEntityValidationException {
        validate();
    }

    private void validate() throws AccountEntityValidationException {
        if (equity == null || equity.compareTo(BigDecimal.ZERO) < 0) {
            throw new AccountEntityValidationException();
        }
    }

    @StandardException
    public static class AccountEntityValidationException extends RuntimeException {
    }
}
