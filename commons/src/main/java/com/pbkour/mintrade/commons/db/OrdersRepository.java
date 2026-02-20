package com.pbkour.mintrade.commons.db;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrdersRepository extends JpaRepository<OrderEntity, UUID> {
    Page<OrderEntity> findByAccountId(UUID accountId, Pageable pageable);
}
