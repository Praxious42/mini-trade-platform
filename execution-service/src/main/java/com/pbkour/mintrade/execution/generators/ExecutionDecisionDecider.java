package com.pbkour.mintrade.execution.generators;

import com.pbkour.mintrade.commons.orders.ExecutionDecision;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExecutionDecisionDecider {
    private static final double DEFAULT_REJECTION_RATE = 0.05;

    public static ExecutionDecision generateExecutionDecision() {
        return generateExecutionDecision(DEFAULT_REJECTION_RATE);
    }

    public static ExecutionDecision generateExecutionDecision(double rejectionRate) {
        double randomValue = Math.random();
        if (randomValue < rejectionRate) {
            return ExecutionDecision.REJECTED;
        } else {
            return ExecutionDecision.ACCEPTED;
        }
    }
}
