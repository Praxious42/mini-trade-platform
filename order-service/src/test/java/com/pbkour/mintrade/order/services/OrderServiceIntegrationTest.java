package com.pbkour.mintrade.order.services;

import com.pbkour.mintrade.commons.RiskCheckResponse;
import com.pbkour.mintrade.commons.RiskCheckServiceGrpc;
import com.pbkour.mintrade.commons.dto.Order;
import com.pbkour.mintrade.commons.orders.Side;
import com.pbkour.mintrade.commons.orders.Status;
import com.pbkour.mintrade.commons.orders.Symbol;
import com.pbkour.mintrade.commons.orders.Type;
import com.pbkour.mintrade.order.entities.OrderEntity;
import com.pbkour.mintrade.order.repositories.OrdersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrdersRepository ordersRepository;

    @BeforeEach
    void clean() {
        ordersRepository.deleteAll();
    }

    @Test
    void rejectedOrderIsCommittedEvenWhenCreateOrderThrows() {
        // arrange - market order to avoid limit price validation
        Order order = Order.builder()
            .accountId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
            .symbol(Symbol.AAPL)
            .side(Side.BUY)
            .type(Type.MARKET)
            .quantity(BigDecimal.valueOf(1))
            .build();

        // act - createOrder should throw OrderRejectedException
        assertThrows(OrderService.OrderRejectedException.class, () -> orderService.createOrder(order));

        // assert - rejected order was persisted
        List<OrderEntity> all = ordersRepository.findAll();
        assertEquals(1, all.size(), "Rejected order should have been committed in a separate transaction");
        OrderEntity saved = all.get(0);
        assertEquals(Status.REJECTED, saved.getStatus());
        assertEquals(order.getAccountId(), saved.getAccountId());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public RiskCheckServiceGrpc.RiskCheckServiceBlockingStub riskCheckServiceBlockingStub() {
            RiskCheckServiceGrpc.RiskCheckServiceBlockingStub stub = Mockito.mock(RiskCheckServiceGrpc.RiskCheckServiceBlockingStub.class);
            when(stub.checkOrderRisk(any())).thenReturn(RiskCheckResponse.newBuilder().setAllowed(false).setReason("test-rejected").build());
            return stub;
        }
    }
}
