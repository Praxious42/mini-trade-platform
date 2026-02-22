package com.pbkour.mintrade.execution.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.StandardException;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Access(AccessType.FIELD)
@Table(name = "fills")
public class FillEntity {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    @Column(name = "qty", precision = 19, scale = 4, nullable = false)
    private BigDecimal quantity;
    @Column(name = "price", precision = 18, scale = 8, nullable = false)
    private BigDecimal price;
    @Column(name = "ts", nullable = false)
    private Instant timestamp;

    @PrePersist
    public void prePersistUpdate() throws FillEntityValidationException {
        timestamp = Instant.now();
        validate();
    }

    private void validate() throws FillEntityValidationException {
        if (orderId == null) {
            throw new FillEntityValidationException("orderId is null");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new FillEntityValidationException("price is invalid");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new FillEntityValidationException("quantity is invalid");
        }
    }

    @StandardException
    public static class FillEntityValidationException extends RuntimeException {
    }
}
