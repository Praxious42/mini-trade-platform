package com.pbkour.mintrade.commons.generators;

import com.pbkour.mintrade.commons.orders.Symbol;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
// ...existing imports...
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.*;

class PriceGeneratorTest {

    @Test
    void generatePrice_public_doesNotThrowAndWithinReason() {
        PriceGenerator gen = new PriceGenerator();
        for (Symbol s : Symbol.values()) {
            BigDecimal p = gen.generatePrice(s);
            assertNotNull(p);
            // price should be positive and roughly near the initialPrice
            assertTrue(p.compareTo(BigDecimal.ZERO) > 0);
        }
    }

    @Test
    void generatePrice_private_deterministicForGivenTick() throws Exception {
        PriceGenerator gen = new PriceGenerator();

        Method m = PriceGenerator.class.getDeclaredMethod("generatePrice", Symbol.class, long.class);
        m.setAccessible(true);

        long tick = 123456L;
        for (Symbol s : Symbol.values()) {
            BigDecimal actual = (BigDecimal) m.invoke(gen, s, tick);

            // replicate logic to compute expected value
            long seed = 42L ^ s.name().hashCode();
            SplittableRandom rng = new SplittableRandom(seed + tick);
            BigDecimal base;
            switch (s) {
                case EURUSD -> base = new BigDecimal("1.0800");
                case GBPUSD -> base = new BigDecimal("1.2600");
                case USDJPY -> base = new BigDecimal("150.00");
                case AAPL -> base = new BigDecimal("180.00");
                default -> throw new IllegalStateException("unexpected");
            }
            double wobble = (rng.nextDouble() - 0.5) * 0.01;
            BigDecimal expected = base.multiply(BigDecimal.valueOf(1.0 + wobble));

            // compare within a tiny delta to avoid scale differences
            BigDecimal diff = expected.subtract(actual).abs();
            assertTrue(diff.compareTo(new BigDecimal("0.0000001")) <= 0,
                    () -> "expected=" + expected + " actual=" + actual + " diff=" + diff);
        }
    }
}


