package com.pbkour.mintrade.execution.generators;

import com.pbkour.mintrade.commons.orders.ExecutionDecision;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExecutionDecider {
    private static final double DEFAULT_REJECTION_RATE = 0.05;
    private static final double DEFAULT_PARTIAL_FILL_RATE = 0.5;

    public static List<Long> getPartialFills(long quantity) {
        return getPartialFills(quantity, DEFAULT_PARTIAL_FILL_RATE);
    }

    protected static List<Long> getPartialFills(long quantity, double partialFillRate) {
        double randomValue = Math.random();
        log.info("This is a partial fill");
        if (randomValue < partialFillRate) {
            return List.of(quantity / 2, quantity - quantity / 2);
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
