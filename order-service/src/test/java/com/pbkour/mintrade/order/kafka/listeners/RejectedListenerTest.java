package com.pbkour.mintrade.order.kafka.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.commons.json.ObjectMapperFactory;
import com.pbkour.mintrade.commons.kafka.OrdersRejected;
import com.pbkour.mintrade.commons.repositories.ProcessedEventsRepository;
import com.pbkour.mintrade.order.services.OrderService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RejectedListenerTest {
    private final ObjectMapper mapper = ObjectMapperFactory.create();
    @Mock
    ProcessedEventsRepository processedEventsRepository;
    @Mock
    private OrderService orderService;

    @Test
    void onOrdersRejected_parsesValidPayload_andCallsOrderService() throws Exception {
        OrdersRejected payload = OrdersRejected.builder()
            .eventId(UUID.randomUUID())
            .occurredAt(Instant.now())
            .orderId(UUID.randomUUID())
            .build();

        String json = mapper.writeValueAsString(payload);

        RejectedListener listener = new RejectedListener(mapper, orderService);

        assertDoesNotThrow(() -> listener.onOrdersRejected(json, "key-1"));

        verify(orderService, times(1)).rejectOrder(any(OrdersRejected.class));
    }

    @Test
    void onOrdersRejected_throwsRejectedListenerException() {
        RejectedListener listener = new RejectedListener(mapper, orderService);

        assertThrows(RejectedListener.RejectedListenerException.class, () -> listener.onOrdersRejected("not-a-json", "k"));

        verify(orderService, never()).rejectOrder(any());
    }

    @Test
    void onDlq_handlesConsumerRecordWithoutThrowing() {
        RejectedListener listener = new RejectedListener(mapper, orderService);

        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("orders.rejected.dlq", 0, 0L, "key-1", "{\"some\":\"payload\"}");

        assertDoesNotThrow(() -> listener.onDlq(consumerRecord));
    }
}

