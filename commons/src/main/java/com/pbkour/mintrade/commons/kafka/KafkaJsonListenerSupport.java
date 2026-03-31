package com.pbkour.mintrade.commons.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaJsonListenerSupport {
    private final ObjectMapper objectMapper;

    public <T> T deserialize(String message, Class<T> type) throws JsonProcessingException {
        return objectMapper.readValue(message, type);
    }
}

