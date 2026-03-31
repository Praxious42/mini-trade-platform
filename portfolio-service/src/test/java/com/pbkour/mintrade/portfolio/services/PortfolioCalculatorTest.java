package com.pbkour.mintrade.portfolio.services;
import com.pbkour.mintrade.commons.kafka.Fill;
import com.pbkour.mintrade.commons.kafka.OrdersFilled;
import com.pbkour.mintrade.commons.orders.Side;
import com.pbkour.mintrade.commons.orders.Symbol;
import com.pbkour.mintrade.portfolio.entities.PositionEntity;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
class PortfolioCalculatorTest {
    private final PortfolioCalculator calculator = new PortfolioCalculator();
    @Test
    void calculateNewEquity_buySubtractsFillValue() {
        OrdersFilled payload = payload(Side.BUY, List.of(
            fill("2", "10.00"),
            fill("3", "12.00")
        ));
        BigDecimal result = calculator.calculateNewEquity(payload, new BigDecimal("1000.00"));
        assertEquals(0, result.compareTo(new BigDecimal("944.00")));
    }
    @Test
    void calculateNewPosition_buyCreatesOrExtendsPosition() {
        OrdersFilled payload = payload(Side.BUY, List.of(
            fill("2", "10.00"),
            fill("3", "12.00")
        ));
        PositionEntity existing = PositionEntity.builder()
            .id(new PositionEntity.PositionId(UUID.randomUUID(), Symbol.AAPL))
            .netQty(new BigDecimal("5"))
            .avgPrice(new BigDecimal("10.00"))
            .build();
        PortfolioCalculator.NewPosition result = calculator.calculateNewPosition(payload, existing);
        assertEquals(0, result.netQty().compareTo(new BigDecimal("10")));
        assertEquals(0, result.avgPrice().compareTo(new BigDecimal("10.60")));
    }
    @Test
    void calculateNewPosition_sellThrowsWhenPositionIsMissing() {
        OrdersFilled payload = payload(Side.SELL, List.of(fill("1", "10.00")));
        assertThrows(IllegalStateException.class, () -> calculator.calculateNewPosition(payload, null));
    }
    @Test
    void calculateNewPosition_sellDecreasesPosition() {
        OrdersFilled payload = payload(Side.SELL, List.of(fill("3", "12.00")));
        PositionEntity existing = PositionEntity.builder()
            .id(new PositionEntity.PositionId(UUID.randomUUID(), Symbol.AAPL))
            .netQty(new BigDecimal("5"))
            .avgPrice(new BigDecimal("10.00"))
            .build();
        PortfolioCalculator.NewPosition result = calculator.calculateNewPosition(payload, existing);
        assertEquals(0, result.netQty().compareTo(new BigDecimal("2")));
        assertEquals(0, result.avgPrice().compareTo(new BigDecimal("10.00")));
    }
    private OrdersFilled payload(Side side, List<Fill> fills) {
        return OrdersFilled.builder()
            .eventId(UUID.randomUUID())
            .occurredAt(Instant.now())
            .accountId(UUID.randomUUID())
            .symbol(Symbol.AAPL)
            .side(side)
            .fills(fills)
            .build();
    }
    private Fill fill(String quantity, String price) {
        return Fill.builder()
            .fillId(UUID.randomUUID())
            .quantity(new BigDecimal(quantity))
            .price(new BigDecimal(price))
            .timestamp(Instant.now())
            .build();
    }
}
