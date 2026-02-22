package com.pbkour.mintrade.order.services;

import com.pbkour.mintrade.commons.RiskCheckResponse;
import com.pbkour.mintrade.commons.RiskCheckServiceGrpc;
import com.pbkour.mintrade.commons.dto.Order;
import com.pbkour.mintrade.commons.orders.Side;
import com.pbkour.mintrade.commons.orders.Symbol;
import com.pbkour.mintrade.commons.orders.Type;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceGrpcFallbackTest {

    @Mock
    private RiskCheckServiceGrpc.RiskCheckServiceBlockingStub riskCheckServiceBlockingStub;

    @InjectMocks
    private OrderService orderService;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = Order.builder()
            .accountId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
            .symbol(Symbol.AAPL)
            .side(Side.BUY)
            .type(Type.LIMIT)
            .quantity(BigDecimal.valueOf(100))
            .limitPrice(new BigDecimal("150.50"))
            .build();
    }

    @Test
    void whenRiskCheckReturnsNotAllowed_thenCreateOrderThrowsOrderRejectedException() {
        when(riskCheckServiceBlockingStub.checkOrderRisk(any()))
            .thenReturn(RiskCheckResponse.newBuilder().setAllowed(false).setReason("test-deny").build());

        assertThrows(OrderService.OrderRejectedException.class, () -> orderService.createOrder(sampleOrder));
    }

    @Test
    void whenRiskCheckThrowsUnavailable_thenCreateOrderThrowsOrderRejectedException() {
        when(riskCheckServiceBlockingStub.checkOrderRisk(any()))
            .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

        assertThrows(OrderService.OrderRejectedException.class, () -> orderService.createOrder(sampleOrder));
    }

    @Test
    void whenRiskCheckThrowsDeadlineExceeded_thenCreateOrderThrowsOrderRejectedException() {
        when(riskCheckServiceBlockingStub.checkOrderRisk(any()))
            .thenThrow(new StatusRuntimeException(Status.DEADLINE_EXCEEDED));

        assertThrows(OrderService.OrderRejectedException.class, () -> orderService.createOrder(sampleOrder));
    }
}
