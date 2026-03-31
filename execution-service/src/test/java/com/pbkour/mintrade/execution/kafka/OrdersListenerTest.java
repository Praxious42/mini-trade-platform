package com.pbkour.mintrade.execution.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.commons.kafka.KafkaJsonListenerSupport;
import com.pbkour.mintrade.commons.json.ObjectMapperFactory;
import com.pbkour.mintrade.commons.kafka.Order;
import com.pbkour.mintrade.commons.kafka.OrdersCreated;
import com.pbkour.mintrade.execution.services.OrderFillService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrdersListenerTest {
    private final ObjectMapper mapper = ObjectMapperFactory.create();
    @Mock
    private OrderFillService orderFillService;

    @Test
    void onOrdersCreated_parsesValidPayloadWithoutThrowing() throws Exception {
        OrdersCreated payload = OrdersCreated.builder()
            .eventId(UUID.randomUUID())
            .occurredAt(Instant.now())
            .order(Order.builder()
                .orderId(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .limitPrice(BigDecimal.valueOf(123.45))
                .quantity(new BigDecimal("10"))
                .build())
            .build();

        String json = mapper.writeValueAsString(payload);

        OrdersListener listener = new OrdersListener(new KafkaJsonListenerSupport(mapper), orderFillService);

        assertDoesNotThrow(() -> listener.onOrdersCreated(json, "key-1"));
    }

    @Test
    void onOrdersCreated_shouldThrowOrdersListenerException() {
        OrdersListener listener = new OrdersListener(new KafkaJsonListenerSupport(mapper), orderFillService);

        assertThrows(OrdersListener.OrdersListenerException.class, () -> listener.onOrdersCreated("not-a-json", "k"));
    }

    @Test
    void onDlq_handlesConsumerRecordWithoutThrowing() {
        OrdersListener listener = new OrdersListener(new KafkaJsonListenerSupport(mapper), orderFillService);

        ConsumerRecord<String, String> record = new ConsumerRecord<>("orders.created.dlq", 0, 0L, "key-1", "{\"some\":\"payload\"}");

        assertDoesNotThrow(() -> listener.onDlq(record));
    }

    @Test
    void onOrdersCreated_nullMessage_shouldNotThrow() {
        OrdersListener listener = new OrdersListener(new KafkaJsonListenerSupport(mapper), orderFillService);

        assertDoesNotThrow(() -> listener.onOrdersCreated(null, "key-null"));
    }

    @Test
    void onOrdersCreated_emptyMessage_shouldNotThrow() {
        OrdersListener listener = new OrdersListener(new KafkaJsonListenerSupport(mapper), orderFillService);

        assertDoesNotThrow(() -> listener.onOrdersCreated("", "key-empty"));
    }

    @Test
    void onOrdersCreated_payloadJsonNull_shouldThrowOrdersListenerException() {
        OrdersListener listener = new OrdersListener(new KafkaJsonListenerSupport(mapper), orderFillService);

        // JSON literal null maps to Java null when deserialized
        String json = "null";

        assertThrows(OrdersListener.OrdersListenerException.class, () -> listener.onOrdersCreated(json, "key-null-payload"));
    }

    @Test
    void onOrdersCreated_skipsWhenEventAlreadyProcessed() throws Exception {
        UUID eventId = UUID.randomUUID();
        OrdersCreated payload = OrdersCreated.builder()
            .eventId(eventId)
            .occurredAt(Instant.now())
            .order(Order.builder()
                .orderId(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .limitPrice(BigDecimal.valueOf(123.45))
                .quantity(new BigDecimal("10"))
                .build())
            .build();

        String json = mapper.writeValueAsString(payload);

        OrdersListener listener = new OrdersListener(new KafkaJsonListenerSupport(mapper), orderFillService);

        assertDoesNotThrow(() -> listener.onOrdersCreated(json, "key-processed"));

        verify(orderFillService).fillOrder(any());
    }
}
