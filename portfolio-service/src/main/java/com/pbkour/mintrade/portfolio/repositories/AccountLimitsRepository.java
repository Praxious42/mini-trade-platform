package com.pbkour.mintrade.portfolio.repositories;

import com.pbkour.mintrade.portfolio.entities.AccountLimitEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountLimitsRepository extends JpaRepository<AccountLimitEntity, UUID> {
}

