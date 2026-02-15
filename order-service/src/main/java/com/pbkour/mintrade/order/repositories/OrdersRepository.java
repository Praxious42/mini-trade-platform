package com.pbkour.mintrade.order.repositories;

import com.pbkour.mintrade.contracts.db.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrdersRepository extends JpaRepository<OrderEntity, UUID> {
    Page<OrderEntity> findByAccountId(UUID accountId, Pageable pageable);
}
