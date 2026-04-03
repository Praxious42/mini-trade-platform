package com.pbkour.mintrade.commons.orders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionDecisionTest {

    @Test
    void enum_hasExpectedValues() {
        ExecutionDecision[] values = ExecutionDecision.values();
        assertArrayEquals(new ExecutionDecision[]{ExecutionDecision.ACCEPTED, ExecutionDecision.REJECTED}, values);
    }

    @Test
    void valueOf_returnsCorrect() {
        assertEquals(ExecutionDecision.ACCEPTED, ExecutionDecision.valueOf("ACCEPTED"));
        assertEquals(ExecutionDecision.REJECTED, ExecutionDecision.valueOf("REJECTED"));
    }
}

