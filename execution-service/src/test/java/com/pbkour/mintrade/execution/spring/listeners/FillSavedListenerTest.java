package com.pbkour.mintrade.execution.spring.listeners;

import com.pbkour.mintrade.commons.kafka.KafkaJsonPublisherSupport;
import com.pbkour.mintrade.commons.kafka.Fill;
import com.pbkour.mintrade.commons.kafka.Order;
import com.pbkour.mintrade.commons.kafka.OrdersFilled;
import com.pbkour.mintrade.commons.orders.Symbol;
import com.pbkour.mintrade.execution.entities.FillEntity;
import com.pbkour.mintrade.execution.services.OrderFillService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FillSavedListenerTest {

    @Mock
    private KafkaJsonPublisherSupport kafkaJsonPublisherSupport;

    @InjectMocks
    private FillSavedListener listener;

    @Captor
    private ArgumentCaptor<OrdersFilled> payloadCaptor;

    @Test
    void onSaved_buildsPayload_andDelegatesToPublisher() {
        UUID fillId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID orderId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID accountId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Instant ts = Instant.parse("2020-01-01T00:00:00Z");

        FillEntity fill = FillEntity.builder()
            .id(fillId)
            .orderId(orderId)
            .quantity(new BigDecimal("50"))
            .price(new BigDecimal("123.45"))
            .timestamp(ts)
            .build();

        Order order = Order.builder().orderId(orderId).accountId(accountId).symbol(Symbol.AAPL).build();

        OrderFillService.FillsSavedEvent event = new OrderFillService.FillsSavedEvent(List.of(fill), order);

        listener.onSaved(event);

        verify(kafkaJsonPublisherSupport).publish(eq("orders.filled"), payloadCaptor.capture());
        OrdersFilled of = payloadCaptor.getValue();
        assertNotNull(of);
        assertNotNull(of.getEventId());
        assertNotNull(of.getOccurredAt());
        assertEquals(orderId, of.getOrderId());
        assertEquals(accountId, of.getAccountId());
        assertEquals(Symbol.AAPL, of.getSymbol());
        assertNotNull(of.getFills());
        assertEquals(1, of.getFills().size());

        Fill fillDto = of.getFills().get(0);
        assertEquals(fillId, fillDto.getFillId());
        assertEquals(0, (new BigDecimal("50")).compareTo(fillDto.getQuantity()));
        assertEquals(new BigDecimal("123.45"), fillDto.getPrice());
        assertEquals(ts, fillDto.getTimestamp());
    }

    @Test
    void onSaved_stillDelegatesPayload_whenPublisherHandlesFailures() {
        UUID fillId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UUID orderId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        UUID accountId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        Instant ts = Instant.parse("2020-01-02T00:00:00Z");

        FillEntity fill = FillEntity.builder()
            .id(fillId)
            .orderId(orderId)
            .quantity(new BigDecimal("10"))
            .price(new BigDecimal("10.00"))
            .timestamp(ts)
            .build();

        Order order = Order.builder().orderId(orderId).accountId(accountId).symbol(Symbol.AAPL).build();

        OrderFillService.FillsSavedEvent event = new OrderFillService.FillsSavedEvent(List.of(fill), order);

        assertDoesNotThrow(() -> listener.onSaved(event));

        verify(kafkaJsonPublisherSupport).publish(eq("orders.filled"), any());
    }
}
