package com.pbkour.mintrade.commons.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FillTest {

    private final ObjectMapper mapper = new ObjectMapper()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .registerModule(new JavaTimeModule())
        .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    @Test
    void serializeDeserialize_roundTrip() throws Exception {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Instant ts = Instant.parse("2020-01-01T00:00:00Z");

        Fill fill = Fill.builder()
            .fillId(id)
            .quantity(100L)
            .price(new BigDecimal("123.45"))
            .timestamp(ts)
            .build();

        String json = mapper.writeValueAsString(fill);

        Fill again = mapper.readValue(json, Fill.class);

        assertEquals(fill, again);
    }
}

