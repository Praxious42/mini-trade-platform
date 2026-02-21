package com.pbkour.mintrade.portfolio.entities;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountEntityTest {

    @Test
    void prePersist_setsCreatedAtAndPasses_whenEquityValid() {
        AccountEntity entity = AccountEntity.builder()
            .equity(new BigDecimal("100.00"))
            .build();

        assertDoesNotThrow(entity::prePersistUpdate);
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    void prePersist_throws_whenEquityNullOrNegative() {
        AccountEntity nullEquity = AccountEntity.builder()
            .equity(null)
            .build();

        assertThrows(AccountEntity.AccountEntityValidationException.class, nullEquity::prePersistUpdate);

        AccountEntity negative = AccountEntity.builder()
            .equity(new BigDecimal("-0.0001"))
            .build();

        assertThrows(AccountEntity.AccountEntityValidationException.class, negative::prePersistUpdate);
    }
}

