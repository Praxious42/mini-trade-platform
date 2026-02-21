package com.pbkour.mintrade.portfolio.entities;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountLimitEntityTest {

    @Test
    void prePersist_setsTimestampsAndPasses_whenValuesValid() {
        AccountLimitEntity entity = AccountLimitEntity.builder()
            .accountId(java.util.UUID.randomUUID())
            .maxNotional(new BigDecimal("100000.00"))
            .maxPosPerSymbol(new BigDecimal("1000"))
            .marginRateFx(new BigDecimal("0.1"))
            .marginRateStock(new BigDecimal("0.2"))
            .build();

        assertDoesNotThrow(entity::prePersistUpdate);
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    void prePersist_throws_whenInvalidValues() {
        AccountLimitEntity negNotional = AccountLimitEntity.builder()
            .accountId(java.util.UUID.randomUUID())
            .maxNotional(new BigDecimal("-1.00"))
            .build();

        assertThrows(AccountLimitEntity.AccountLimitEntityValidationException.class, negNotional::prePersistUpdate);

        AccountLimitEntity zeroPos = AccountLimitEntity.builder()
            .accountId(java.util.UUID.randomUUID())
            .maxPosPerSymbol(new BigDecimal("0"))
            .build();

        assertThrows(AccountLimitEntity.AccountLimitEntityValidationException.class, zeroPos::prePersistUpdate);

        AccountLimitEntity badFx = AccountLimitEntity.builder()
            .accountId(java.util.UUID.randomUUID())
            .marginRateFx(new BigDecimal("1.5"))
            .build();

        assertThrows(AccountLimitEntity.AccountLimitEntityValidationException.class, badFx::prePersistUpdate);
    }
}

