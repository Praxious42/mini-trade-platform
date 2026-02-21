package com.pbkour.mintrade.portfolio.repositories;

import com.pbkour.mintrade.portfolio.entities.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountsRepository extends JpaRepository<AccountEntity, UUID> {
}

