package com.pbkour.mintrade.contracts.kafka;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class OrdersCancelled extends KafkaPayload {
    private UUID orderId;
    private UUID accountId;
}
