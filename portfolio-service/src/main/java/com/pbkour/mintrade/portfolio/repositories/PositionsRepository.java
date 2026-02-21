package com.pbkour.mintrade.portfolio.repositories;

import com.pbkour.mintrade.portfolio.entities.PositionEntity;
import com.pbkour.mintrade.portfolio.entities.PositionEntity.PositionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PositionsRepository extends JpaRepository<PositionEntity, PositionId> {
    List<PositionEntity> findByIdAccountId(UUID accountId);
}

