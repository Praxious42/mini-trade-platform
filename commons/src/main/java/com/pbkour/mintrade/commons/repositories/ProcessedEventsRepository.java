package com.pbkour.mintrade.commons.repositories;

import com.pbkour.mintrade.commons.entities.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventsRepository extends JpaRepository<ProcessedEventEntity, UUID> {
}

