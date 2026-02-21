package com.pbkour.mintrade.portfolio.entities;

import com.pbkour.mintrade.commons.orders.Symbol;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PositionEntityTest {

    @Test
    void prePersist_setsUpdatedAtAndPasses_forValidPosition() {
        PositionEntity.PositionId id = new PositionEntity.PositionId(UUID.randomUUID(), Symbol.AAPL);
        PositionEntity p = PositionEntity.builder()
            .id(id)
            .netQty(new BigDecimal("10"))
            .avgPrice(new BigDecimal("150.00"))
            .build();

        assertDoesNotThrow(p::prePersist);
        assertNotNull(p.getUpdatedAt());
    }

    @Test
    void prePersist_throws_whenRequiredFieldsMissing_orInvalid() {
        PositionEntity missingId = PositionEntity.builder()
            .netQty(new BigDecimal("1"))
            .avgPrice(new BigDecimal("1"))
            .build();

        assertThrows(PositionEntity.PositionValidationException.class, missingId::prePersist);

        PositionEntity missingAcct = PositionEntity.builder()
            .id(new PositionEntity.PositionId(null, Symbol.AAPL))
            .netQty(new BigDecimal("1"))
            .avgPrice(new BigDecimal("1"))
            .build();

        assertThrows(PositionEntity.PositionValidationException.class, missingAcct::prePersist);

        PositionEntity missingSymbol = PositionEntity.builder()
            .id(new PositionEntity.PositionId(UUID.randomUUID(), null))
            .netQty(new BigDecimal("1"))
            .avgPrice(new BigDecimal("1"))
            .build();

        assertThrows(PositionEntity.PositionValidationException.class, missingSymbol::prePersist);

        PositionEntity nullQty = PositionEntity.builder()
            .id(new PositionEntity.PositionId(UUID.randomUUID(), Symbol.AAPL))
            .netQty(null)
            .avgPrice(new BigDecimal("1"))
            .build();

        assertThrows(PositionEntity.PositionValidationException.class, nullQty::prePersist);

        PositionEntity avgZeroButQtyZero = PositionEntity.builder()
            .id(new PositionEntity.PositionId(UUID.randomUUID(), Symbol.AAPL))
            .netQty(new BigDecimal("0"))
            .avgPrice(new BigDecimal("1"))
            .build();

        assertThrows(PositionEntity.PositionValidationException.class, avgZeroButQtyZero::prePersist);

        PositionEntity negativeAvg = PositionEntity.builder()
            .id(new PositionEntity.PositionId(UUID.randomUUID(), Symbol.AAPL))
            .netQty(new BigDecimal("1"))
            .avgPrice(new BigDecimal("-1"))
            .build();

        assertThrows(PositionEntity.PositionValidationException.class, negativeAvg::prePersist);
    }
}

