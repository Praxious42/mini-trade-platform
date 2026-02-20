package com.pbkour.mintrade.execution.spring.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.commons.db.FillEntity;
import com.pbkour.mintrade.commons.kafka.Fill;
import com.pbkour.mintrade.commons.kafka.OrdersFilled;
import com.pbkour.mintrade.commons.orders.Symbol;
import com.pbkour.mintrade.execution.services.OrderFillService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FillSavedListenerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    private FillSavedListener listener;

    @Captor
    private ArgumentCaptor<Object> payloadCaptor;

    @Test
    void onSaved_sendsMessage_whenSerializationSucceeds() throws Exception {
        UUID fillId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID orderId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID accountId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Instant ts = Instant.parse("2020-01-01T00:00:00Z");

        FillEntity fill = FillEntity.builder()
            .id(fillId)
            .orderId(orderId)
            .quantity(50L)
            .price(new BigDecimal("123.45"))
            .timestamp(ts)
            .build();

        OrderFillService.FillSavedEvent event = new OrderFillService.FillSavedEvent(fill, accountId, Symbol.AAPL);

        String json = "{\"event\":\"orders-filled\"}";
        when(mapper.writeValueAsString(any())).thenReturn(json);

        listener.onSaved(event);

        // capture the object passed to mapper
        verify(mapper, times(1)).writeValueAsString(payloadCaptor.capture());
        Object captured = payloadCaptor.getValue();
        assertNotNull(captured);
        assertTrue(captured instanceof OrdersFilled);

        OrdersFilled of = (OrdersFilled) captured;
        assertEquals(fillId, of.getEventId());
        assertEquals(ts, of.getOccurredAt());
        assertEquals(orderId, of.getOrderId());
        assertEquals(accountId, of.getAccountId());
        assertEquals(Symbol.AAPL, of.getSymbol());
        assertNotNull(of.getFills());
        assertEquals(1, of.getFills().size());

        Fill fillDto = of.getFills().get(0);
        assertEquals(fillId, fillDto.getFillId());
        assertEquals(50L, fillDto.getQuantity());
        assertEquals(new BigDecimal("123.45"), fillDto.getPrice());
        assertEquals(ts, fillDto.getTimestamp());

        // verify kafka send called with expected args
        verify(kafkaTemplate, times(1)).send("orders.filled", fillId.toString(), json);
    }

    @Test
    void onSaved_doesNotSend_whenSerializationFails() throws Exception {
        UUID fillId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UUID orderId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        UUID accountId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        Instant ts = Instant.parse("2020-01-02T00:00:00Z");

        FillEntity fill = FillEntity.builder()
            .id(fillId)
            .orderId(orderId)
            .quantity(10L)
            .price(new BigDecimal("10.00"))
            .timestamp(ts)
            .build();

        OrderFillService.FillSavedEvent event = new OrderFillService.FillSavedEvent(fill, accountId, Symbol.AAPL);

        when(mapper.writeValueAsString(any())).thenThrow(new RuntimeException("boom"));

        assertDoesNotThrow(() -> listener.onSaved(event));

        verify(mapper, times(1)).writeValueAsString(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }
}

