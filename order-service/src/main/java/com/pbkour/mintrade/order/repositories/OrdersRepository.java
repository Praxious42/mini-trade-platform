package com.pbkour.mintrade.order.repositories;

import com.pbkour.mintrade.contracts.db.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrdersRepository extends JpaRepository<OrderEntity, UUID> {
    // create query to get all orders for an account
    List<OrderEntity> findByAccountId(UUID accountId);
}
