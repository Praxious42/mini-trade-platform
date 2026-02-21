package com.pbkour.mintrade.order.spring.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.order.entities.OrderEntity;
import com.pbkour.mintrade.order.services.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderSavedListenerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    private OrderSavedListener listener;

    @Test
    void onSaved_sendsMessage_whenSerializationSucceeds() throws Exception {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        OrderEntity entity = OrderEntity.builder()
            .id(id)
            .createdAt(Instant.now())
            .build();

        OrderService.OrderSavedEvent event = new OrderService.OrderSavedEvent(entity);

        String json = "{\"event\":\"orders-created\"}";
        when(mapper.writeValueAsString(any())).thenReturn(json);

        listener.onSaved(event);

        verify(kafkaTemplate).send(eq("orders.created"), anyString(), eq(json));
    }

    @Test
    void onSaved_doesNotSend_whenSerializationFails() throws Exception {
        UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        OrderEntity entity = OrderEntity.builder()
            .id(id)
            .createdAt(Instant.now())
            .build();

        OrderService.OrderSavedEvent event = new OrderService.OrderSavedEvent(entity);

        when(mapper.writeValueAsString(any())).thenThrow(new RuntimeException("boom"));

        listener.onSaved(event);

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }
}

