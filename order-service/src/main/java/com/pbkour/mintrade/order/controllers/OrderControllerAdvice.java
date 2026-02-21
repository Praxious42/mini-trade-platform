package com.pbkour.mintrade.order.controllers;

import com.pbkour.mintrade.order.entities.OrderEntity.OrderEntityValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.Map;

@Slf4j
@ControllerAdvice(assignableTypes = OrderController.class)
public class OrderControllerAdvice {

    @ExceptionHandler(OrderEntityValidationException.class)
    public ResponseEntity<Object> handleOrderValidation(OrderEntityValidationException ex, WebRequest request) {
        log.info("Order validation failed: {}", ex.getMessage());

        Map<String, Object> body = Map.of(
            "timestamp", Instant.now().toString(),
            "status", HttpStatus.BAD_REQUEST.value(),
            "error", "Bad Request",
            "message", "Validation failed for order"
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        log.info("Could not parse message: {}", ex.getMessage());

        Map<String, Object> body = Map.of(
            "timestamp", Instant.now().toString(),
            "status", HttpStatus.BAD_REQUEST.value(),
            "error", "Bad Request",
            "message", "Malformed JSON request"
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(OrderController.OrderControllerValidationException.class)
    public ResponseEntity<Object> handleMessageNotReadable(OrderController.OrderControllerValidationException ex, WebRequest request) {
        log.info("Could not validate request parameters: {}", ex.getMessage());

        Map<String, Object> body = Map.of(
            "timestamp", Instant.now().toString(),
            "status", HttpStatus.BAD_REQUEST.value(),
            "error", "Bad Request",
            "message", "Could not validate request parameters"
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}

