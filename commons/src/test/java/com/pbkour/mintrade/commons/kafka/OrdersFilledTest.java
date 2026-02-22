package com.pbkour.mintrade.commons.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.commons.json.ObjectMapperFactory;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrdersFilledTest {

    private final ObjectMapper mapper = ObjectMapperFactory.create();

    @Test
    void deserializeAndSerialize_ordersFilled_roundTrip() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/orders-filled.json")) {
            assertNotNull(is, "Test resource orders-filled.json should be present");
            OrdersFilled payload = mapper.readValue(is, OrdersFilled.class);
            assertNotNull(payload);
            assertNotNull(payload.getEventId());
            assertNotNull(payload.getOccurredAt());
            assertNotNull(payload.getOrderId());
            assertNotNull(payload.getAccountId());
            assertNotNull(payload.getSymbol());
            assertNotNull(payload.getFills());
            assertEquals(2, payload.getFills().size());

            // verify a fill value
            assertEquals(BigDecimal.valueOf(50L), payload.getFills().get(0).getQuantity());
            assertEquals(UUID.fromString("44444444-4444-4444-4444-444444444444"), payload.getFills().get(0).getFillId());

            // round-trip
            String json = mapper.writeValueAsString(payload);
            OrdersFilled again = mapper.readValue(json, OrdersFilled.class);
            assertEquals(payload, again);
        }
    }
}
