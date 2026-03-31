package com.pbkour.mintrade.portfolio.kafka.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pbkour.mintrade.commons.kafka.KafkaJsonListenerSupport;
import com.pbkour.mintrade.commons.kafka.OrdersFilled;
import com.pbkour.mintrade.portfolio.services.PortfolioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class FillsListenerTest {

    private ObjectMapper objectMapper;
    private PortfolioService portfolioService;
    private FillsListener listener;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        portfolioService = mock(PortfolioService.class);
        listener = new FillsListener(new KafkaJsonListenerSupport(objectMapper), portfolioService);
    }

    @Test
    void onOrdersFilled_parsesValidPayload_andDelegatesToService() throws Exception {
        OrdersFilled payload = OrdersFilled.builder()
            .eventId(UUID.randomUUID())
            .occurredAt(Instant.now())
            .build();

        String json = objectMapper.writeValueAsString(payload);

        assertDoesNotThrow(() -> listener.onOrdersFilled(json, "key-1"));

        verify(portfolioService).processOrdersFilled(any(OrdersFilled.class));
    }

    @Test
    void onOrdersFilled_whenObjectMapperFails_throwsIllegalStateException() throws Exception {
        ObjectMapper bad = mock(ObjectMapper.class);
        when(bad.readValue(anyString(), eq(OrdersFilled.class)))
            .thenThrow(new JsonProcessingException("bad json") {
            });

        FillsListener badListener = new FillsListener(new KafkaJsonListenerSupport(bad), portfolioService);

        assertThrows(FillsListener.FillsListenerException.class, () -> badListener.onOrdersFilled("not-a-json", "k"));

        verify(portfolioService, never()).processOrdersFilled(any());
    }
}
