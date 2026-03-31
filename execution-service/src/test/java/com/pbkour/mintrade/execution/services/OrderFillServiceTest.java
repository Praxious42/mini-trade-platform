package com.pbkour.mintrade.execution.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.commons.generators.PriceGenerator;
import com.pbkour.mintrade.commons.kafka.Order;
import com.pbkour.mintrade.commons.kafka.OrdersCreated;
import com.pbkour.mintrade.commons.kafka.OrdersRejected;
import com.pbkour.mintrade.commons.orders.ExecutionDecision;
import com.pbkour.mintrade.commons.orders.Side;
import com.pbkour.mintrade.commons.orders.Symbol;
import com.pbkour.mintrade.commons.orders.Type;
import com.pbkour.mintrade.commons.services.ProcessedEventRecorder;
import com.pbkour.mintrade.execution.entities.FillEntity;
import com.pbkour.mintrade.execution.generators.ExecutionDecider;
import com.pbkour.mintrade.execution.repositories.FillsRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderFillServiceTest {

    @Mock
    private ProcessedEventRecorder processedEventRecorder;
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

    @BeforeEach
    void setup() {
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return null;
        }).when(processedEventRecorder).processIfNotProcessed(any(), anyString(), any(Runnable.class));
    }

    @Test
    void fillOrder_whenAccepted_savesFill_andPublishesEvent() {
        Order order = Order.builder()
            .orderId(UUID.randomUUID())
            .accountId(UUID.randomUUID())
            .symbol(Symbol.AAPL)
            .side(Side.BUY)
            .type(Type.MARKET)
            .quantity(new BigDecimal("10"))
            .build();

        OrdersCreated payload = OrdersCreated.builder()
            .eventId(UUID.randomUUID())
            .order(order)
            .build();

        when(priceGenerator.generatePrice(Symbol.AAPL)).thenReturn(new BigDecimal("180.00"));

        List<BigDecimal> knownFills = List.of(new BigDecimal("6"), new BigDecimal("4"));
        try (MockedStatic<ExecutionDecider> mock = Mockito.mockStatic(ExecutionDecider.class)) {
            mock.when(ExecutionDecider::generateExecutionDecision).thenReturn(ExecutionDecision.ACCEPTED);
            mock.when(() -> ExecutionDecider.getPartialFills(order.getQuantity())).thenReturn(knownFills);

            orderFillService.fillOrder(payload);
        }

        verify(fillsRepository).saveAll(fillCaptor.capture());
        List<FillEntity> capturedFills = fillCaptor.getValue();
        assertEquals(knownFills.size(), capturedFills.size());
        for (int i = 0; i < capturedFills.size(); i++) {
            assertEquals(order.getOrderId(), capturedFills.get(i).getOrderId());
            assertEquals(0, knownFills.get(i).compareTo(capturedFills.get(i).getQuantity()));
            assertEquals(0, capturedFills.get(i).getPrice().compareTo(new BigDecimal("180.00")));
        }

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
            .quantity(new BigDecimal("10"))
            .build();

        OrdersCreated payload = OrdersCreated.builder()
            .order(order)
            .build();

        try (MockedStatic<ExecutionDecider> mock = Mockito.mockStatic(ExecutionDecider.class)) {
            mock.when(ExecutionDecider::generateExecutionDecision).thenReturn(ExecutionDecision.REJECTED);

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
            .quantity(new BigDecimal("5"))
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
