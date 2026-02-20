package com.pbkour.mintrade.commons.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.commons.json.ObjectMapperFactory;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrdersCreatedTest {

    private final ObjectMapper mapper = ObjectMapperFactory.create();

    @Test
    void deserializeAndSerialize_ordersCreated_roundTrip() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/orders-created.json")) {
            assertNotNull(is, "Test resource orders-created.json should be present");
            OrdersCreated payload = mapper.readValue(is, OrdersCreated.class);
            assertNotNull(payload);
            assertNotNull(payload.getEventId());
            assertNotNull(payload.getOccurredAt());
            assertNotNull(payload.getOrder());
            assertEquals(BigDecimal.valueOf(150.5), payload.getOrder().getLimitPrice());

            // round-trip
            String json = mapper.writeValueAsString(payload);
            OrdersCreated again = mapper.readValue(json, OrdersCreated.class);
            assertEquals(payload, again);
        }
    }
}
