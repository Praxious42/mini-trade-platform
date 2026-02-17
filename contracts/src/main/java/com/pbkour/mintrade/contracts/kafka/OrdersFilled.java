package com.pbkour.mintrade.contracts.kafka;

import com.pbkour.mintrade.contracts.orders.Symbol;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.UUID;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class OrdersFilled extends KafkaPayload {
    private UUID orderId;
    private UUID accountId;
    private Symbol symbol;
    private List<Fill> fills;
}
