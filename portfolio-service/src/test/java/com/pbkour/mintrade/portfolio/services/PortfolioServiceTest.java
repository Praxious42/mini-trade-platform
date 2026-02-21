package com.pbkour.mintrade.portfolio.services;

import com.pbkour.mintrade.commons.kafka.OrdersFilled;
import com.pbkour.mintrade.portfolio.entities.ProcessedEventEntity;
import com.pbkour.mintrade.portfolio.repositories.PositionsRepository;
import com.pbkour.mintrade.portfolio.repositories.ProcessedEventsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PortfolioServiceTest {

    private PositionsRepository positionsRepository;
    private ProcessedEventsRepository processedEventsRepository;
    private PortfolioService portfolioService;

    @BeforeEach
    void setUp() {
        positionsRepository = mock(PositionsRepository.class);
        processedEventsRepository = mock(ProcessedEventsRepository.class);
        portfolioService = new PortfolioService(positionsRepository, processedEventsRepository);
    }

    @Test
    void processOrdersFilled_skips_whenEventIdNull() {
        OrdersFilled payload = OrdersFilled.builder().eventId(null).build();

        assertDoesNotThrow(() -> portfolioService.processOrdersFilled(payload));

        verifyNoInteractions(processedEventsRepository, positionsRepository);
    }

    @Test
    void processOrdersFilled_skips_whenAlreadyProcessed() {
        UUID id = UUID.randomUUID();
        OrdersFilled payload = OrdersFilled.builder().eventId(id).build();

        when(processedEventsRepository.existsById(id)).thenReturn(true);

        portfolioService.processOrdersFilled(payload);

        verify(processedEventsRepository).existsById(id);
        verify(processedEventsRepository, never()).save(any(ProcessedEventEntity.class));
    }

    @Test
    void processOrdersFilled_savesProcessedEvent_andHandlesConcurrentInsert() {
        UUID id = UUID.randomUUID();
        OrdersFilled payload = OrdersFilled.builder().eventId(id).build();

        when(processedEventsRepository.existsById(id)).thenReturn(false);

        when(processedEventsRepository.save(any(ProcessedEventEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> portfolioService.processOrdersFilled(payload));

        ArgumentCaptor<ProcessedEventEntity> captor = ArgumentCaptor.forClass(ProcessedEventEntity.class);
        verify(processedEventsRepository).save(captor.capture());
        assertEquals(id, captor.getValue().getEventId());

        reset(processedEventsRepository);
        when(processedEventsRepository.existsById(id)).thenReturn(false);
        when(processedEventsRepository.save(any(ProcessedEventEntity.class)))
            .thenThrow(new org.springframework.dao.DataIntegrityViolationException("dup"));

        assertDoesNotThrow(() -> portfolioService.processOrdersFilled(payload));
        verify(processedEventsRepository).save(any(ProcessedEventEntity.class));
    }
}
