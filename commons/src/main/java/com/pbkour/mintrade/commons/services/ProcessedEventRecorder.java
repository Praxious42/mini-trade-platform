package com.pbkour.mintrade.commons.services;

import com.pbkour.mintrade.commons.entities.ProcessedEventEntity;
import com.pbkour.mintrade.commons.repositories.ProcessedEventsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessedEventRecorder {
    private final ProcessedEventsRepository processedEventsRepository;

    /**
     * Records the processed event in a separate transaction and flushes it so that
     * duplicate processing can be detected across instances immediately.
     *
     * @return true if the event was recorded, false if it was already recorded (duplicate)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markEventProcessed(UUID eventId) {
        try {
            if (eventId == null) {
                log.warn("Received OrdersFilled with null eventId, processing will be skipped");
                return false;
            }

            processedEventsRepository.saveAndFlush(new ProcessedEventEntity(eventId, Instant.now()));
            return true;
        } catch (DataIntegrityViolationException e) {
            // treat duplicate-key/DataIntegrityViolation as already processed
            log.debug("Event {} already processed (duplicate key / DataIntegrityViolation)", eventId);
            return false;
        }
    }
}
