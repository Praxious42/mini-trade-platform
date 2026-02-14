package com.pbkour.mintrade.order.services;

import com.pbkour.mintrade.contracts.db.OrderEntity;
import com.pbkour.mintrade.contracts.dto.Order;
import com.pbkour.mintrade.contracts.orders.Side;
import com.pbkour.mintrade.contracts.orders.Symbol;
import com.pbkour.mintrade.contracts.orders.Type;
import com.pbkour.mintrade.order.repositories.OrdersRepository;
import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrdersRepository ordersRepository;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<OrderEntity> entityCaptor;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = Order.builder()
            .accountId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
            .symbol(Symbol.AAPL)
            .side(Side.BUY)
            .type(Type.LIMIT)
            .quantity(100L)
            .limitPrice(new BigDecimal("150.50"))
            .build();
    }

    @Test
    void createOrder_savesEntity_andReturnsId() {
        OrderEntity saved = OrderEntity.builder()
            .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
            .accountId(sampleOrder.getAccountId())
            .symbol(sampleOrder.getSymbol())
            .side(sampleOrder.getSide())
            .type(sampleOrder.getType())
            .quantity(sampleOrder.getQuantity())
            .limitPrice(sampleOrder.getLimitPrice())
            .status(null)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .version(0)
            .build();

        when(ordersRepository.save(any(OrderEntity.class))).thenReturn(saved);

        UUID result = orderService.createOrder(sampleOrder);

        assertEquals(saved.getId(), result);
        verify(ordersRepository, times(1)).save(entityCaptor.capture());
        OrderEntity captured = entityCaptor.getValue();
        assertEquals(sampleOrder.getAccountId(), captured.getAccountId());
        assertEquals(sampleOrder.getSymbol(), captured.getSymbol());
        assertEquals(sampleOrder.getSide(), captured.getSide());
        assertEquals(sampleOrder.getType(), captured.getType());
        assertEquals(sampleOrder.getQuantity(), captured.getQuantity());
        assertEquals(sampleOrder.getLimitPrice(), captured.getLimitPrice());
    }

    @Test
    void getOrder_returnsMappedOrder() {
        UUID id = UUID.fromString("44444444-4444-4444-4444-444444444444");
        OrderEntity entity = OrderEntity.builder()
            .id(id)
            .accountId(sampleOrder.getAccountId())
            .symbol(sampleOrder.getSymbol())
            .side(sampleOrder.getSide())
            .type(sampleOrder.getType())
            .quantity(sampleOrder.getQuantity())
            .limitPrice(sampleOrder.getLimitPrice())
            .status(null)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .version(0)
            .build();

        when(ordersRepository.getReferenceById(id)).thenReturn(entity);

        Order returned = orderService.getOrder(id);

        assertNotNull(returned);
        assertEquals(sampleOrder.getAccountId(), returned.getAccountId());
    }

    @Test
    void getAccountOrders_returnsListMapped() {
        UUID accountId = sampleOrder.getAccountId();
        OrderEntity entity = OrderEntity.builder()
            .id(UUID.fromString("55555555-5555-5555-5555-555555555555"))
            .accountId(accountId)
            .symbol(sampleOrder.getSymbol())
            .side(sampleOrder.getSide())
            .type(sampleOrder.getType())
            .quantity(sampleOrder.getQuantity())
            .limitPrice(sampleOrder.getLimitPrice())
            .status(null)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .version(0)
            .build();

        when(ordersRepository.findByAccountId(accountId)).thenReturn(List.of(entity));

        List<Order> orders = orderService.getAccountOrders(accountId);

        assertEquals(1, orders.size());
        assertEquals(accountId, orders.get(0).getAccountId());
    }

    @Test
    void cancelOrder_setsStatusAndSaves() {
        UUID id = UUID.fromString("66666666-6666-6666-6666-666666666666");
        OrderEntity entity = OrderEntity.builder()
            .id(id)
            .accountId(sampleOrder.getAccountId())
            .symbol(sampleOrder.getSymbol())
            .side(sampleOrder.getSide())
            .type(sampleOrder.getType())
            .quantity(sampleOrder.getQuantity())
            .limitPrice(sampleOrder.getLimitPrice())
            .status(null)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .version(0)
            .build();

        when(ordersRepository.getReferenceById(id)).thenReturn(entity);
        when(ordersRepository.save(any(OrderEntity.class))).thenAnswer(i -> i.getArgument(0));

        orderService.cancelOrder(id);

        verify(ordersRepository, times(1)).getReferenceById(id);
        verify(ordersRepository, times(1)).save(entityCaptor.capture());
        OrderEntity saved = entityCaptor.getValue();
        assertEquals(id, saved.getId());
    }
}

