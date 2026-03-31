package com.pbkour.mintrade.order.kafka.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pbkour.mintrade.commons.kafka.KafkaJsonListenerSupport;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FillsListenerTest {
    @Mock
    private KafkaJsonListenerSupport kafkaJsonListenerSupport;
    @Mock
    private OrderService orderService;
    @InjectMocks
    private FillsListener fillsListener;

    @Captor
    private ArgumentCaptor<OrdersFilled> ordersFilledCaptor;

    @Test
    void onOrdersFilled_happyPath_callsService() throws Exception {
        String message = "{\"event\":\"ok\"}";

        OrdersFilled payload = OrdersFilled.builder().orderId(UUID.randomUUID()).eventId(UUID.randomUUID()).build();

        when(kafkaJsonListenerSupport.deserialize(message, OrdersFilled.class)).thenReturn(payload);

        fillsListener.onOrdersFilled(message, "key-1");

        verify(orderService, times(1)).updateFilledOrder(ordersFilledCaptor.capture());
        assertSame(payload, ordersFilledCaptor.getValue());
    }

    @Test
    void onOrdersFilled_whenDeserializationFails_doesNotCallService() throws Exception {
        String badMessage = "not-a-json";

        when(kafkaJsonListenerSupport.deserialize(badMessage, OrdersFilled.class)).thenThrow(new JsonProcessingException("bad payload") {
        });

        assertThrows(FillsListener.FillsListenerException.class, () -> fillsListener.onOrdersFilled(badMessage, "key-2"));

        verify(orderService, never()).updateFilledOrder(any());
    }

    @Test
    void onDlq_noInteractionsWithService() {
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("orders.filled.dlq", 0, 0L, "k", "v");

        fillsListener.onDlq(consumerRecord);

        verifyNoInteractions(orderService);
    }
}
