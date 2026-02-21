package com.pbkour.mintrade.portfolio.entities;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProcessedEventEntityTest {

    @Test
    void prePersist_setsProcessedAt_whenNullAndPasses() {
        ProcessedEventEntity e = ProcessedEventEntity.builder()
            .eventId(UUID.randomUUID())
            .processedAt(null)
            .build();

        assertDoesNotThrow(e::prePersist);
        assertNotNull(e.getProcessedAt());
    }

    @Test
    void validate_throws_whenMissingFields() {
        ProcessedEventEntity missingId = ProcessedEventEntity.builder()
            .processedAt(Instant.now())
            .build();

        assertThrows(ProcessedEventEntity.ProcessedEventValidationException.class, missingId::prePersist);

        ProcessedEventEntity e = ProcessedEventEntity.builder().build();
        assertThrows(ProcessedEventEntity.ProcessedEventValidationException.class, e::prePersist);
    }
}
