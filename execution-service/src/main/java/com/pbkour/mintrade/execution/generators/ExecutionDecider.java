package com.pbkour.mintrade.execution.generators;

import com.pbkour.mintrade.commons.orders.ExecutionDecision;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExecutionDecider {
    private static final double DEFAULT_REJECTION_RATE = 0.05;
    private static final double DEFAULT_PARTIAL_FILL_RATE = 0.5;

    public static List<BigDecimal> getPartialFills(BigDecimal quantity) {
        if (quantity.equals(BigDecimal.ONE)) {
            return List.of(quantity);
        }
        return getPartialFills(quantity, DEFAULT_PARTIAL_FILL_RATE);
    }

    protected static List<BigDecimal> getPartialFills(BigDecimal quantity, double partialFillRate) {
        double randomValue = Math.random();
        if (randomValue < partialFillRate) {
            log.info("This is a partial fill");
            BigDecimal half = quantity.divide(BigDecimal.valueOf(2), 0, RoundingMode.DOWN);
            return List.of(half, quantity.subtract(half));
        }

        return List.of(quantity);
    }

    public static ExecutionDecision generateExecutionDecision() {
        return generateExecutionDecision(DEFAULT_REJECTION_RATE);
    }

    protected static ExecutionDecision generateExecutionDecision(double rejectionRate) {
        double randomValue = Math.random();
        if (randomValue < rejectionRate) {
            return ExecutionDecision.REJECTED;
        } else {
            return ExecutionDecision.ACCEPTED;
        }
    }
}
