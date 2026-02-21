package com.pbkour.mintrade.portfolio.repositories;

import com.pbkour.mintrade.portfolio.entities.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventsRepository extends JpaRepository<ProcessedEventEntity, UUID> {
}

