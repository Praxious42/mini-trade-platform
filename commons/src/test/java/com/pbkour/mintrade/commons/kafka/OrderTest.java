package com.pbkour.mintrade.commons.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.commons.json.ObjectMapperFactory;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrderTest {

    private final ObjectMapper mapper = ObjectMapperFactory.create();

    @Test
    void deserializeAndSerialize_order_roundTrip() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/order.json")) {
            assertNotNull(is, "Test resource order.json should be present");
            Order order = mapper.readValue(is, Order.class);
            assertNotNull(order);
            assertEquals(UUID.fromString("11111111-1111-1111-1111-111111111111"), order.getOrderId());
            assertEquals(UUID.fromString("22222222-2222-2222-2222-222222222222"), order.getAccountId());
            assertEquals(com.pbkour.mintrade.commons.orders.Symbol.AAPL, order.getSymbol());
            assertEquals(com.pbkour.mintrade.commons.orders.Side.BUY, order.getSide());
            assertEquals(com.pbkour.mintrade.commons.orders.Type.LIMIT, order.getType());
            assertEquals(Long.valueOf(100L), order.getQuantity());
            assertEquals(new BigDecimal("150.50"), order.getLimitPrice());

            // round-trip
            String json = mapper.writeValueAsString(order);
            Order again = mapper.readValue(json, Order.class);
            assertEquals(order, again);
        }
    }
}

