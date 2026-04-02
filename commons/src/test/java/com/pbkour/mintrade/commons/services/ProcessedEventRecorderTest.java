package com.pbkour.mintrade.commons.services;

import com.pbkour.mintrade.commons.repositories.ProcessedEventsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessedEventRecorderTest {

    @Mock
    private ProcessedEventsRepository processedEventsRepository;

    private ProcessedEventRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new ProcessedEventRecorder(processedEventsRepository);
    }

    @Test
    void processIfNotProcessed_runsActionForNewEvent() {
        UUID eventId = UUID.randomUUID();
        when(processedEventsRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicBoolean executed = new AtomicBoolean(false);

        recorder.processIfNotProcessed(eventId, "OrdersFilled", () -> executed.set(true));

        assertTrue(executed.get());
    }

    @Test
    void processIfNotProcessed_skipsActionForDuplicateEvent() {
        UUID eventId = UUID.randomUUID();
        doThrow(new DataIntegrityViolationException("duplicate")).when(processedEventsRepository).saveAndFlush(any());

        AtomicBoolean executed = new AtomicBoolean(false);

        recorder.processIfNotProcessed(eventId, "OrdersFilled", () -> executed.set(true));

        assertFalse(executed.get());
    }

    @Test
    void processIfNotProcessed_skipsActionForNullEventId() {
        AtomicBoolean executed = new AtomicBoolean(false);

        recorder.processIfNotProcessed(null, "OrdersFilled", () -> executed.set(true));

        assertFalse(executed.get());
    }
}

