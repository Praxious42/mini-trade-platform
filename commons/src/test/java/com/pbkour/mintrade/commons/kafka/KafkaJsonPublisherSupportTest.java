package com.pbkour.mintrade.commons.kafka;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.commons.json.ObjectMapperFactory;
import com.pbkour.mintrade.commons.orders.Side;
import com.pbkour.mintrade.commons.orders.Symbol;
import com.pbkour.mintrade.commons.orders.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class KafkaJsonPublisherSupportTest {
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    private ObjectMapper mapper;
    private KafkaJsonPublisherSupport support;
    @BeforeEach
    void setUp() {
        mapper = ObjectMapperFactory.create();
        support = new KafkaJsonPublisherSupport(kafkaTemplate, mapper);
    }
    @Test
    void publish_serializesAndSendsPayload() {
        OrdersCreated payload = OrdersCreated.builder()
            .eventId(UUID.randomUUID())
            .occurredAt(Instant.parse("2020-01-01T00:00:00Z"))
            .order(Order.builder()
                .orderId(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .symbol(Symbol.AAPL)
                .side(Side.BUY)
                .type(Type.MARKET)
                .quantity(new BigDecimal("10"))
                .build())
            .build();
        support.publish("orders.created", payload);
        verify(kafkaTemplate).send(eq("orders.created"), eq(payload.getEventId().toString()), anyString());
    }
    @Test
    void publish_skipsNullPayload() {
        assertDoesNotThrow(() -> support.publish("orders.created", null));
        verifyNoInteractions(kafkaTemplate);
    }
    @Test
    void publish_handlesSerializationFailureWithoutThrowing() throws Exception {
        ObjectMapper badMapper = mock(ObjectMapper.class);
        when(badMapper.writeValueAsString(any())).thenThrow(new RuntimeException("boom"));
        KafkaJsonPublisherSupport badSupport = new KafkaJsonPublisherSupport(kafkaTemplate, badMapper);
        OrdersCreated payload = OrdersCreated.builder()
            .eventId(UUID.randomUUID())
            .occurredAt(Instant.now())
            .build();
        assertDoesNotThrow(() -> badSupport.publish("orders.created", payload));
        verifyNoInteractions(kafkaTemplate);
    }
}
