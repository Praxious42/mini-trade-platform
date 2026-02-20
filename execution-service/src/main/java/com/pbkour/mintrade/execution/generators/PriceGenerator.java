package com.pbkour.mintrade.execution.generators;

import com.pbkour.mintrade.commons.orders.Symbol;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.SplittableRandom;

@Component
public class PriceGenerator {

    public BigDecimal generatePrice(Symbol symbol) {
        return generatePrice(symbol, System.currentTimeMillis() / 1000L);
    }

    private BigDecimal generatePrice(Symbol symbol, long tick) {
        long seed = 42L ^ symbol.name().hashCode();
        SplittableRandom rng = new SplittableRandom(seed + tick);

        BigDecimal base = initialPrice(symbol);

        double wobble = (rng.nextDouble() - 0.5) * 0.01;
        return base.multiply(BigDecimal.valueOf(1.0 + wobble));
    }

    private BigDecimal initialPrice(Symbol symbol) {
        return switch (symbol) {
            case EURUSD -> new BigDecimal("1.0800");
            case GBPUSD -> new BigDecimal("1.2600");
            case USDJPY -> new BigDecimal("150.00");
            case AAPL -> new BigDecimal("180.00");
        };
    }
}
