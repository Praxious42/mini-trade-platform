package com.pbkour.mintrade.contracts.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.contracts.json.ObjectMapperFactory;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrdersRejectedTest {

    private final ObjectMapper mapper = ObjectMapperFactory.create();

    @Test
    void deserializeAndSerialize_ordersRejected_roundTrip() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/orders-rejected.json")) {
            assertNotNull(is, "Test resource orders-rejected.json should be present");
            OrdersRejected payload = mapper.readValue(is, OrdersRejected.class);
            assertNotNull(payload);
            assertNotNull(payload.getEventId());
            assertNotNull(payload.getOccurredAt());
            assertNotNull(payload.getOrderId());
            assertNotNull(payload.getReason());
            assertEquals(com.pbkour.mintrade.contracts.orders.RejectionReason.RISK_LIMIT, payload.getReason());

            // round-trip
            String json = mapper.writeValueAsString(payload);
            OrdersRejected again = mapper.readValue(json, OrdersRejected.class);
            assertEquals(payload, again);
        }
    }
}
