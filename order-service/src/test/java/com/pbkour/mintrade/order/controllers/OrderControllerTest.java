package com.pbkour.mintrade.order.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.commons.dto.Order;
import com.pbkour.mintrade.commons.json.ObjectMapperFactory;
import com.pbkour.mintrade.commons.orders.Side;
import com.pbkour.mintrade.commons.orders.Symbol;
import com.pbkour.mintrade.commons.orders.Type;
import com.pbkour.mintrade.commons.responses.OrderResponse;
import com.pbkour.mintrade.order.entities.OrderEntity.OrderEntityValidationException;
import com.pbkour.mintrade.order.services.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private final ObjectMapper mapper = ObjectMapperFactory.create();
    private MockMvc mockMvc;
    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController controller;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new OrderControllerAdvice())
            .build();
    }

    @Test
    void createOrder_delegatesToService_andReturnsOk() throws Exception {
        UUID returnedId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        OrderResponse orderResponse = OrderResponse.builder().orderId(returnedId).build();
        when(orderService.createOrder(any(Order.class))).thenReturn(orderResponse);

        try (InputStream in = getClass().getResourceAsStream("/order-request.json")) {
            assertNotNull(in);
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            mockMvc.perform(post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("11111111-1111-1111-1111-111111111111")));

            verify(orderService, times(1)).createOrder(orderCaptor.capture());
            Order captured = orderCaptor.getValue();
            assertEquals(UUID.fromString("22222222-2222-2222-2222-222222222222"), captured.getAccountId());
            assertEquals(Symbol.AAPL, captured.getSymbol());
            assertEquals(Side.BUY, captured.getSide());
            assertEquals(Type.LIMIT, captured.getType());
            assertEquals(BigDecimal.valueOf(100L), captured.getQuantity());
            assertEquals(new BigDecimal("150.50"), captured.getLimitPrice());
        }
    }

    @Test
    void createOrder_whenValidationException_returnsBadRequest() throws Exception {
        when(orderService.createOrder(any(Order.class))).thenThrow(new OrderEntityValidationException());

        try (InputStream in = getClass().getResourceAsStream("/order-request.json")) {
            assertNotNull(in);
            mockMvc.perform(post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new String(in.readAllBytes(), StandardCharsets.UTF_8)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Validation failed for order")));

            verify(orderService, times(1)).createOrder(any());
        }
    }

    @Test
    void createOrder_withMalformedJson_returnsBadRequest() throws Exception {
        String badJson = "{ invalid json }";

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(badJson))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Malformed JSON request")));

        verify(orderService, never()).createOrder(any());
    }

    @Test
    void cancelOrder_delegatesToService_andReturnsOk() throws Exception {
        UUID id = UUID.fromString("33333333-3333-3333-3333-333333333333");

        doNothing().when(orderService).cancelOrder(id);

        mockMvc.perform(post("/api/v1/orders/" + id + "/cancel"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(id.toString())));

        verify(orderService, times(1)).cancelOrder(id);
    }

    @Test
    void getOrder_returnsOrderFromService() throws Exception {
        UUID id = UUID.fromString("44444444-4444-4444-4444-444444444444");
        OrderResponse returned = OrderResponse.builder()
            .accountId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
            .symbol(Symbol.AAPL)
            .side(Side.BUY)
            .type(Type.LIMIT)
            .quantity(BigDecimal.valueOf(100))
            .limitPrice(new BigDecimal("150.50"))
            .build();

        when(orderService.getOrder(id)).thenReturn(returned);

        mockMvc.perform(get("/api/v1/orders/" + id))
            .andExpect(status().isOk())
            .andExpect(content().json(mapper.writeValueAsString(returned)));

        verify(orderService, times(1)).getOrder(id);
    }

    @Test
    void listOrdersByAccount_returnsListFromService() throws Exception {
        UUID accountId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        OrderResponse o1 = OrderResponse.builder()
            .accountId(accountId)
            .symbol(Symbol.AAPL)
            .side(Side.BUY)
            .type(Type.LIMIT)
            .quantity(BigDecimal.valueOf(100))
            .limitPrice(new BigDecimal("150.50"))
            .build();

        when(orderService.getAccountOrders(accountId, 0, 1)).thenReturn(List.of(o1));

        mockMvc.perform(get("/api/v1/orders")
                .param("accountId", accountId.toString())
                .param("page", "0")
                .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(content().json(mapper.writeValueAsString(List.of(o1))));

        verify(orderService, times(1)).getAccountOrders(accountId, 0, 1);
    }

    @Test
    void listOrdersByAccount_negativePage_returnsBadRequest() throws Exception {
        UUID accountId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        mockMvc.perform(get("/api/v1/orders")
                .param("accountId", accountId.toString())
                .param("page", "-1")
                .param("size", "10"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Could not validate request parameters")));

        verify(orderService, never()).getAccountOrders(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void listOrdersByAccount_sizeTooLarge_returnsBadRequest() throws Exception {
        UUID accountId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        mockMvc.perform(get("/api/v1/orders")
                .param("accountId", accountId.toString())
                .param("page", "0")
                .param("size", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Could not validate request parameters")));

        verify(orderService, never()).getAccountOrders(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }
}
