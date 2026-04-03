package com.pbkour.mintrade.commons.orders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatusTest {

	@Test
	void enum_containsAllExpected_states() {
		Status[] vals = Status.values();
		// ensure all declared states are present
		assertTrue(vals.length >= 6);
		assertEquals(Status.NEW, Status.valueOf("NEW"));
		assertEquals(Status.ACCEPTED, Status.valueOf("ACCEPTED"));
		assertEquals(Status.PARTIALLY_FILLED, Status.valueOf("PARTIALLY_FILLED"));
		assertEquals(Status.FILLED, Status.valueOf("FILLED"));
		assertEquals(Status.CANCELLED, Status.valueOf("CANCELLED"));
		assertEquals(Status.REJECTED, Status.valueOf("REJECTED"));
	}
}


