package com.pbkour.mintrade.execution.generators;

import com.pbkour.mintrade.commons.orders.ExecutionDecision;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExecutionDeciderTest {

    @Test
    void getPartialFills_withPartialFillRateOne_evenQuantity_returnsTwoParts() {
        List<BigDecimal> parts = ExecutionDecider.getPartialFills(new BigDecimal("4"), 1.0);
        assertEquals(2, parts.size());
        assertEquals(0, parts.get(0).compareTo(new BigDecimal("2")));
        assertEquals(0, parts.get(1).compareTo(new BigDecimal("2")));
    }

    @Test
    void getPartialFills_withPartialFillRateOne_oddQuantity_returnsTwoPartsWithRemainder() {
        List<BigDecimal> parts = ExecutionDecider.getPartialFills(new BigDecimal("5"), 1.0);
        assertEquals(2, parts.size());
        assertEquals(0, parts.get(0).compareTo(new BigDecimal("2")));
        assertEquals(0, parts.get(1).compareTo(new BigDecimal("3")));
    }

    @Test
    void getPartialFills_withPartialFillRateZero_returnsWhole() {
        List<BigDecimal> parts = ExecutionDecider.getPartialFills(new BigDecimal("5"), 0.0);
        assertEquals(1, parts.size());
        assertEquals(0, parts.get(0).compareTo(new BigDecimal("5")));
    }

    @Test
    void generateExecutionDecision_withRejectionRateOne_returnsRejected() {
        ExecutionDecision decision = ExecutionDecider.generateExecutionDecision(1.0);
        assertEquals(ExecutionDecision.REJECTED, decision);
    }

    @Test
    void generateExecutionDecision_withRejectionRateZero_returnsAccepted() {
        ExecutionDecision decision = ExecutionDecider.generateExecutionDecision(0.0);
        assertEquals(ExecutionDecision.ACCEPTED, decision);
    }
}
