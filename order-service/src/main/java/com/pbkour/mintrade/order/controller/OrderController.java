package com.pbkour.mintrade.order.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("api/v1/orders")
public class OrderController {
    @PostMapping
    public ResponseEntity<String> createOrder(@RequestBody String order) {
        log.debug("Received order creation request with payload: {}", order);
        log.info("Received order creation request");
        return ResponseEntity.ok("Creating resource");
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<String> cancelOrder(@PathVariable String id) {
        log.info("Received order cancel request");
        return ResponseEntity.ok("Cancelling order with id: " + id);
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> getOrder(@PathVariable Long id) {
        log.info("Received order retrieval request for id: {}", id);
        return ResponseEntity.ok("Retrieved order with id: " + id);
    }

    @GetMapping
    public ResponseEntity<String> listOrdersByAccount(@RequestParam String accountId) {
        log.info("Received order retrieval request for account: {}", accountId);
        return ResponseEntity.ok("Retrieved orders for account: " + accountId);
    }
}
