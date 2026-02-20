package com.pbkour.mintrade.commons.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.commons.json.ObjectMapperFactory;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrdersCancelledTest {

    private final ObjectMapper mapper = ObjectMapperFactory.create();

    @Test
    void deserializeAndSerialize_ordersCancelled_roundTrip() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/orders-cancelled.json")) {
            assertNotNull(is, "Test resource orders-cancelled.json should be present");
            OrdersCancelled payload = mapper.readValue(is, OrdersCancelled.class);
            assertNotNull(payload);
            assertNotNull(payload.getEventId());
            assertNotNull(payload.getOccurredAt());
            assertNotNull(payload.getOrderId());
            assertNotNull(payload.getAccountId());

            assertEquals(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"), payload.getOrderId());

            String json = mapper.writeValueAsString(payload);
            OrdersCancelled again = mapper.readValue(json, OrdersCancelled.class);
            assertEquals(payload, again);
        }
    }
}
