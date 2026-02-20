package com.pbkour.mintrade.commons.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FillsRepository extends JpaRepository<FillEntity, UUID> {
}
