package com.pbkour.mintrade.execution.generators;

import com.pbkour.mintrade.commons.orders.ExecutionDecision;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExecutionDeciderTest {

    @Test
    void getPartialFills_withPartialFillRateOne_evenQuantity_returnsTwoParts() {
        List<Long> parts = ExecutionDecider.getPartialFills(4L, 1.0);
        assertEquals(2, parts.size());
        assertEquals(2L, parts.get(0));
        assertEquals(2L, parts.get(1));
    }

    @Test
    void getPartialFills_withPartialFillRateOne_oddQuantity_returnsTwoPartsWithRemainder() {
        List<Long> parts = ExecutionDecider.getPartialFills(5L, 1.0);
        assertEquals(2, parts.size());
        assertEquals(2L, parts.get(0));
        assertEquals(3L, parts.get(1));
    }

    @Test
    void getPartialFills_withPartialFillRateZero_returnsWhole() {
        List<Long> parts = ExecutionDecider.getPartialFills(5L, 0.0);
        assertEquals(1, parts.size());
        assertEquals(5L, parts.get(0));
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
