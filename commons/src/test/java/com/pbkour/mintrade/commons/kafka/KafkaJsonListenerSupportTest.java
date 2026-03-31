package com.pbkour.mintrade.commons.kafka;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.pbkour.mintrade.commons.json.ObjectMapperFactory;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
class KafkaJsonListenerSupportTest {
    private final KafkaJsonListenerSupport support = new KafkaJsonListenerSupport(ObjectMapperFactory.create());
    @Test
    void deserialize_roundTripsPayload() throws Exception {
        OrdersCreated payload = OrdersCreated.builder()
            .eventId(UUID.randomUUID())
            .occurredAt(Instant.now())
            .build();
        String json = ObjectMapperFactory.create().writeValueAsString(payload);
        OrdersCreated result = support.deserialize(json, OrdersCreated.class);
        assertEquals(payload.getEventId(), result.getEventId());
        assertEquals(payload.getOccurredAt(), result.getOccurredAt());
    }
    @Test
    void deserialize_invalidJsonThrows() {
        assertThrows(JsonProcessingException.class, () -> support.deserialize("not-json", OrdersCreated.class));
    }
}
