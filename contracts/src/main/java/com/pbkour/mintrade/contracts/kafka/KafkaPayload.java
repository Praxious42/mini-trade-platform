package com.pbkour.mintrade.contracts.kafka;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public abstract class KafkaPayload {
    private UUID eventId;
    private Instant occurredAt;
}
