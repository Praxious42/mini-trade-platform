package com.pbkour.mintrade.commons.kafka;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
public abstract class KafkaPayload {
    private UUID eventId;
    private Instant occurredAt;
}
