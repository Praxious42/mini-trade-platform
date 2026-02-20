package com.pbkour.mintrade.order.kafka.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.commons.kafka.OrdersFilled;
import com.pbkour.mintrade.order.services.OrderService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FillsListenerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private FillsListener fillsListener;

    @Captor
    private ArgumentCaptor<OrdersFilled> ordersFilledCaptor;

    @Test
    void onOrdersCreated_happyPath_callsService() throws Exception {
        String message = "{\"event\":\"ok\"}";

        OrdersFilled payload = new OrdersFilled();
        payload.setOrderId(UUID.randomUUID());

        when(objectMapper.readValue(eq(message), eq(OrdersFilled.class))).thenReturn(payload);

        fillsListener.onOrdersCreated(message, "key-1");

        verify(orderService, times(1)).updateFilledOrder(ordersFilledCaptor.capture());
        assertSame(payload, ordersFilledCaptor.getValue());
    }

    @Test
    void onOrdersCreated_whenDeserializationFails_doesNotCallService() throws Exception {
        String badMessage = "not-a-json";

        when(objectMapper.readValue(eq(badMessage), eq(OrdersFilled.class))).thenThrow(new JsonProcessingException("bad payload") {
        });

        fillsListener.onOrdersCreated(badMessage, "key-2");

        verify(orderService, never()).updateFilledOrder(any());
    }

    @Test
    void onDlq_noInteractionsWithService() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("orders.filled.dlq", 0, 0L, "k", "v");

        fillsListener.onDlq(record);

        verifyNoInteractions(orderService);
    }
}

