package com.pbkour.mintrade.order.controllers;

import com.pbkour.mintrade.contracts.dto.Order;
import com.pbkour.mintrade.order.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.StandardException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/orders")
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<String> createOrder(@RequestBody Order order) {
        log.debug("Received order creation request with payload: {}", order);
        log.info("Received order creation request");

        UUID orderId = orderService.createOrder(order);

        log.info("Created order with id: {}", orderId);

        return ResponseEntity.ok("Creating resource");
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<String> cancelOrder(@PathVariable UUID id) {
        log.info("Received order cancel request");
        orderService.cancelOrder(id);

        return ResponseEntity.ok("Cancelling order with id: " + id);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable UUID id) {
        log.info("Received order retrieval request for id: {}", id);

        return ResponseEntity.ok(orderService.getOrder(id));
    }

    @GetMapping
    public ResponseEntity<List<Order>> listOrdersByAccount(@RequestParam UUID accountId, @RequestParam int page, @RequestParam int size) {
        log.info("Received order retrieval request for account: {}", accountId);
        if (accountId == null) {
            throw new OrderControllerValidationException("Account ID must be provided");
        }
        if (page < 0) {
            throw new OrderControllerValidationException("Page must be non-negative and size must be positive");
        }
        if (size > 100) {
            throw new OrderControllerValidationException("Size must not exceed 100");
        }

        return ResponseEntity.ok(orderService.getAccountOrders(accountId, page, size));
    }

    @StandardException
    public static class OrderControllerValidationException extends RuntimeException {
    }
}
