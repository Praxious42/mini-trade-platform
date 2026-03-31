package com.pbkour.mintrade.order.spring.listeners;

import com.pbkour.mintrade.commons.kafka.KafkaJsonPublisherSupport;
import com.pbkour.mintrade.commons.kafka.OrdersCreated;
import com.pbkour.mintrade.order.entities.OrderEntity;
import com.pbkour.mintrade.order.services.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSavedListenerTest {

    @Mock
    private KafkaJsonPublisherSupport kafkaJsonPublisherSupport;

    @InjectMocks
    private OrderSavedListener listener;

    @org.mockito.Captor
    private ArgumentCaptor<OrdersCreated> payloadCaptor;

    @Test
    void onSaved_buildsPayload_andDelegatesToPublisher() {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        OrderEntity entity = OrderEntity.builder()
            .id(id)
            .createdAt(Instant.now())
            .build();

        OrderService.OrderSavedEvent event = new OrderService.OrderSavedEvent(entity);

        listener.onSaved(event);

        verify(kafkaJsonPublisherSupport).publish(any(), payloadCaptor.capture());
        OrdersCreated payload = payloadCaptor.getValue();
        assertNotNull(payload);
        assertNotNull(payload.getEventId());
        assertEquals(entity.getCreatedAt(), payload.getOccurredAt());
        assertEquals(entity.getId(), payload.getOrder().getOrderId());
        assertEquals(entity.getAccountId(), payload.getOrder().getAccountId());
    }

    @Test
    void onSaved_stillDelegatesPayload_whenPublisherHandlesFailures() {
        UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        OrderEntity entity = OrderEntity.builder()
            .id(id)
            .createdAt(Instant.now())
            .build();

        OrderService.OrderSavedEvent event = new OrderService.OrderSavedEvent(entity);

        listener.onSaved(event);

        verify(kafkaJsonPublisherSupport).publish(any(), any());
    }
}

