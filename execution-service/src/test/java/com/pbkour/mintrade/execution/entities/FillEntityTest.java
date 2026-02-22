package com.pbkour.mintrade.execution.entities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class FillEntityTest {

    @Test
    void prePersist_setsTimestampAndPassesValidation_whenFieldsValid() {
        UUID orderId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        FillEntity fill = FillEntity.builder()
            .orderId(orderId)
            .quantity(new BigDecimal("5"))
            .price(new BigDecimal("12.34"))
            .build();

        Assertions.assertNull(fill.getTimestamp(), "timestamp should start null before persist");

        Instant before = Instant.now();
        fill.prePersistUpdate();
        Instant after = Instant.now();

        Assertions.assertNotNull(fill.getTimestamp(), "timestamp must be set by prePersistUpdate");
        Assertions.assertFalse(fill.getTimestamp().isBefore(before), "timestamp should be >= before time");
        Assertions.assertFalse(fill.getTimestamp().isAfter(after.plusSeconds(1)), "timestamp should not be in the future");
    }

    @Test
    void prePersist_throws_whenOrderIdIsNull() {
        FillEntity fill = FillEntity.builder()
            .quantity(new BigDecimal("1"))
            .price(new BigDecimal("1.00"))
            .build();

        assertThrows(FillEntity.FillEntityValidationException.class, fill::prePersistUpdate);
    }

    @Test
    void prePersist_throws_whenPriceIsNull() {
        FillEntity fill = FillEntity.builder()
            .orderId(UUID.randomUUID())
            .quantity(new BigDecimal("1"))
            .price(null)
            .build();

        assertThrows(FillEntity.FillEntityValidationException.class, fill::prePersistUpdate);
    }

    @Test
    void prePersist_throws_whenPriceIsNegative() {
        FillEntity fill = FillEntity.builder()
            .orderId(UUID.randomUUID())
            .quantity(new BigDecimal("1"))
            .price(new BigDecimal("-0.01"))
            .build();

        assertThrows(FillEntity.FillEntityValidationException.class, fill::prePersistUpdate);
    }

    @Test
    void prePersist_throws_whenQuantityIsNull_orNonPositive() {
        FillEntity nullQty = FillEntity.builder()
            .orderId(UUID.randomUUID())
            .quantity(null)
            .price(new BigDecimal("1.00"))
            .build();

        assertThrows(FillEntity.FillEntityValidationException.class, nullQty::prePersistUpdate);

        FillEntity zeroQty = FillEntity.builder()
            .orderId(UUID.randomUUID())
            .quantity(new BigDecimal("0"))
            .price(new BigDecimal("1.00"))
            .build();

        assertThrows(FillEntity.FillEntityValidationException.class, zeroQty::prePersistUpdate);

        FillEntity negQty = FillEntity.builder()
            .orderId(UUID.randomUUID())
            .quantity(new BigDecimal("-5"))
            .price(new BigDecimal("1.00"))
            .build();

        assertThrows(FillEntity.FillEntityValidationException.class, negQty::prePersistUpdate);
    }
}

