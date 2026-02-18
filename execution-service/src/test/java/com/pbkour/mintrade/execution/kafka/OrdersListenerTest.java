package com.pbkour.mintrade.execution.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.contracts.json.ObjectMapperFactory;
import com.pbkour.mintrade.contracts.kafka.Order;
import com.pbkour.mintrade.contracts.kafka.OrdersCreated;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class OrdersListenerTest {

    private final ObjectMapper mapper = ObjectMapperFactory.create();

    @Test
    void onOrdersCreated_parsesValidPayloadWithoutThrowing() throws Exception {
        OrdersCreated payload = OrdersCreated.builder()
            .eventId(UUID.randomUUID())
            .occurredAt(Instant.now())
            .order(Order.builder()
                .orderId(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .limitPrice(BigDecimal.valueOf(123.45))
                .quantity(10L)
                .build())
            .build();

        String json = mapper.writeValueAsString(payload);

        OrdersListener listener = new OrdersListener(mapper);

        assertDoesNotThrow(() -> listener.onOrdersCreated(json, "key-1"));
    }

    @Test
    void onOrdersCreated_handlesMalformedJsonWithoutThrowing() {
        OrdersListener listener = new OrdersListener(mapper);

        assertDoesNotThrow(() -> listener.onOrdersCreated("not-a-json", "k"));
    }

    @Test
    void onDlq_handlesConsumerRecordWithoutThrowing() {
        OrdersListener listener = new OrdersListener(mapper);

        ConsumerRecord<String, String> record = new ConsumerRecord<>("orders.created.dlq", 0, 0L, "key-1", "{\"some\":\"payload\"}");

        assertDoesNotThrow(() -> listener.onDlq(record));
    }
}
