package com.pbkour.mintrade.execution.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.commons.db.FillEntity;
import com.pbkour.mintrade.commons.db.FillsRepository;
import com.pbkour.mintrade.commons.kafka.Order;
import com.pbkour.mintrade.commons.kafka.OrdersCreated;
import com.pbkour.mintrade.commons.kafka.OrdersRejected;
import com.pbkour.mintrade.commons.orders.ExecutionDecision;
import com.pbkour.mintrade.commons.orders.Side;
import com.pbkour.mintrade.commons.orders.Symbol;
import com.pbkour.mintrade.commons.orders.Type;
import com.pbkour.mintrade.execution.generators.ExecutionDecider;
import com.pbkour.mintrade.execution.generators.PriceGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderFillServiceTest {

    @Mock
    private PriceGenerator priceGenerator;

    @Mock
    private FillsRepository fillsRepository;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    private OrderFillService orderFillService;

    @Captor
    private ArgumentCaptor<List<FillEntity>> fillCaptor;

    @Test
    void fillOrder_whenAccepted_savesFill_andPublishesEvent() {
        Order order = Order.builder()
            .orderId(UUID.randomUUID())
            .accountId(UUID.randomUUID())
            .symbol(Symbol.AAPL)
            .side(Side.BUY)
            .type(Type.MARKET)
            .quantity(10L)
            .build();

        OrdersCreated payload = OrdersCreated.builder()
            .order(order)
            .build();

        when(priceGenerator.generatePrice(Symbol.AAPL)).thenReturn(new BigDecimal("180.00"));

        try (MockedStatic<ExecutionDecider> mock = Mockito.mockStatic(ExecutionDecider.class)) {
            mock.when(ExecutionDecider::generateExecutionDecision).thenReturn(ExecutionDecision.ACCEPTED);

            orderFillService.fillOrder(payload);
        }

        verify(fillsRepository).saveAll(fillCaptor.capture());
        List<FillEntity> capturedFills = fillCaptor.getValue();
        capturedFills.forEach(fillEntity -> assertEquals(order.getOrderId(), fillEntity.getOrderId()));
        capturedFills.forEach(fillEntity -> assertNotNull(fillEntity.getQuantity()));
        capturedFills.forEach(fillEntity -> assertEquals(0, fillEntity.getPrice().compareTo(new BigDecimal("180.00"))));

        verify(publisher, times(1)).publishEvent(any(OrderFillService.FillsSavedEvent.class));
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void fillOrder_whenRejected_sendsKafkaRejected_andDoesNotSave() throws Exception {
        Order order = Order.builder()
            .orderId(UUID.randomUUID())
            .accountId(UUID.randomUUID())
            .symbol(Symbol.AAPL)
            .side(Side.BUY)
            .type(Type.MARKET)
            .quantity(10L)
            .build();

        OrdersCreated payload = OrdersCreated.builder()
            .order(order)
            .build();

        // mock static decision to REJECTED
        try (MockedStatic<ExecutionDecider> mock = Mockito.mockStatic(ExecutionDecider.class)) {
            mock.when(ExecutionDecider::generateExecutionDecision).thenReturn(ExecutionDecision.REJECTED);

            // make mapper.serialize return some JSON
            when(mapper.writeValueAsString(any(OrdersRejected.class))).thenReturn("{\"orderId\":\"x\"}");

            orderFillService.fillOrder(payload);
        }

        verify(fillsRepository, never()).save(any());
        verify(publisher, never()).publishEvent(any());
        verify(kafkaTemplate, times(1)).send(eq("orders.rejected"), anyString(), anyString());
    }

    @Test
    void fillOrder_limitOrder_priceNotFavorable_doesNotSaveOrPublish() {
        Order order = Order.builder()
            .orderId(UUID.randomUUID())
            .accountId(UUID.randomUUID())
            .symbol(Symbol.AAPL)
            .side(Side.BUY)
            .type(Type.LIMIT)
            .quantity(5L)
            .limitPrice(new BigDecimal("150.00"))
            .build();

        OrdersCreated payload = OrdersCreated.builder()
            .order(order)
            .build();

        // price higher than limit for BUY should not fill
        when(priceGenerator.generatePrice(Symbol.AAPL)).thenReturn(new BigDecimal("160.00"));

        try (MockedStatic<ExecutionDecider> mock = Mockito.mockStatic(ExecutionDecider.class)) {
            mock.when(ExecutionDecider::generateExecutionDecision).thenReturn(ExecutionDecision.ACCEPTED);

            orderFillService.fillOrder(payload);
        }

        verify(fillsRepository, never()).save(any());
        verify(publisher, never()).publishEvent(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }
}

