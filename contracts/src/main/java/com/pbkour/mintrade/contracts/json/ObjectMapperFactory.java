package com.pbkour.mintrade.contracts.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Factory for a pre-configured Jackson ObjectMapper used across modules/tests.
 */
public final class ObjectMapperFactory {

    private ObjectMapperFactory() {
    }

    public static ObjectMapper create() {
        return new ObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(new JavaTimeModule());
    }
}

