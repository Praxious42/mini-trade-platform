package com.pbkour.mintrade.commons.entities;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProcessedEventEntityTest {

    @Test
    void prePersist_setsProcessedAt_whenNull() {
        ProcessedEventEntity e = ProcessedEventEntity.builder()
                .eventId(UUID.randomUUID())
                .processedAt(null)
                .build();

        assertNull(e.getProcessedAt());
        e.prePersist();
        assertNotNull(e.getProcessedAt());
        // processedAt should be very recent
        assertTrue(Duration.between(e.getProcessedAt(), Instant.now()).abs().toSeconds() < 5);
    }

    @Test
    void prePersist_throwsWhenEventIdNull() {
        ProcessedEventEntity e = ProcessedEventEntity.builder()
                .eventId(null)
                .processedAt(Instant.now())
                .build();

        assertThrows(ProcessedEventEntity.ProcessedEventValidationException.class, e::prePersist);
    }

    @Test
    void validate_passesWhenBothPresent() {
        ProcessedEventEntity e = ProcessedEventEntity.builder()
                .eventId(UUID.randomUUID())
                .processedAt(Instant.now())
                .build();

        // calling prePersist should not throw and should preserve processedAt
        e.prePersist();
        assertNotNull(e.getProcessedAt());
    }
}

