package com.pbkour.mintrade.order.controllers;

import com.pbkour.mintrade.order.entities.OrderEntity.OrderEntityValidationException;
import com.pbkour.mintrade.order.services.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class OrderControllerAdvice {

    private Map<String, Object> buildBody(HttpStatus status, String error, String message, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        if (request != null) {
            // WebRequest.getDescription(false) returns something like "uri=/api/..."; include for debugging
            String desc = request.getDescription(false);
            body.put("path", desc != null ? desc.replaceFirst("uri=", "") : null);
        }
        return body;
    }

    @ExceptionHandler(OrderEntityValidationException.class)
    public ResponseEntity<Object> handleOrderValidation(OrderEntityValidationException ex, WebRequest request) {
        log.info("Order validation failed: {}", ex.getMessage());

        Map<String, Object> body = buildBody(HttpStatus.BAD_REQUEST, "Bad Request", "Validation failed for order", request);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        log.info("Could not parse message: {}", ex.getMessage());

        Map<String, Object> body = buildBody(HttpStatus.BAD_REQUEST, "Bad Request", "Malformed JSON request", request);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(OrderController.OrderControllerValidationException.class)
    public ResponseEntity<Object> handleControllerValidation(OrderController.OrderControllerValidationException ex, WebRequest request) {
        log.info("Could not validate request parameters: {}", ex.getMessage());

        Map<String, Object> body = buildBody(HttpStatus.BAD_REQUEST, "Bad Request", "Could not validate request parameters", request);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Map OrderRejectedException (thrown when risk check disallows the order) to 422 Unprocessable Entity
    @ExceptionHandler(OrderService.OrderRejectedException.class)
    public ResponseEntity<Object> handleOrderRejected(OrderService.OrderRejectedException ex, WebRequest request) {
        log.info("Order rejected by business rules: {}", ex.getMessage());

        HttpStatus status = HttpStatus.valueOf(422);
        Map<String, Object> body = buildBody(status, "Unprocessable Entity", ex.getMessage() != null ? ex.getMessage() : "Order rejected", request);

        return ResponseEntity.status(status).body(body);
    }

    // Generic fallback for unexpected server errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unhandled exception in OrderController: {}", ex.getMessage(), ex);

        Map<String, Object> body = buildBody(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", "Temporary error processing request", request);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
