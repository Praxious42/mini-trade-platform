package com.pbkour.mintrade.commons.kafka;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrdersCancelled extends KafkaPayload {
    private UUID orderId;
    private UUID accountId;
}
