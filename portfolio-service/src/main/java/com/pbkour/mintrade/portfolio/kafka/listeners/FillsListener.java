package com.pbkour.mintrade.portfolio.kafka.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.commons.kafka.OrdersFilled;
import com.pbkour.mintrade.portfolio.services.PortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FillsListener {
    private final ObjectMapper objectMapper;
    private final PortfolioService portfolioService;

    @KafkaListener(topics = "orders.filled")
    public void onOrdersFilled(String message, @Header(name = "kafka_receivedMessageKey", required = false) String key) {
        try {
            OrdersFilled payload = objectMapper.readValue(message, OrdersFilled.class);
            log.info("[FillsListener] Received OrdersFilled Message: {}", payload);

            portfolioService.processOrdersFilled(payload);
        } catch (Exception e) {
            log.error("[FillsListener] failed to process orders.filled key={}", key, e);
            throw new IllegalStateException(e);
        }
    }

    @KafkaListener(topics = "orders.filled.dlq")
    public void onDlq(ConsumerRecord<String, String> consumerRecord) {
        log.info("[FillsListenerDLQ] received orders.filled.dlq consumerRecord={}", consumerRecord);
    }
}
