package com.pbkour.mintrade.contracts.kafka;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrdersCancelled extends KafkaPayload {
    private UUID orderId;
    private UUID accountId;
}
