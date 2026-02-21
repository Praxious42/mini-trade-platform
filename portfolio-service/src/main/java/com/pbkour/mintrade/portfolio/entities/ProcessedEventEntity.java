package com.pbkour.mintrade.portfolio.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.StandardException;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Access(AccessType.FIELD)
@Table(name = "processed_events")
public class ProcessedEventEntity {
    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @PrePersist
    public void prePersist() {
        if (processedAt == null) {
            processedAt = Instant.now();
        }
        validate();
    }

    private void validate() {
        if (eventId == null) {
            throw new ProcessedEventValidationException("eventId is required");
        }
        if (processedAt == null) {
            throw new ProcessedEventValidationException("processedAt is required");
        }
    }

    @StandardException
    public static class ProcessedEventValidationException extends RuntimeException {
    }
}

