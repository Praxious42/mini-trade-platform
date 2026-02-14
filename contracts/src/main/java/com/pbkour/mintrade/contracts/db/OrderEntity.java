package com.pbkour.mintrade.contracts.db;

import com.pbkour.mintrade.contracts.orders.Side;
import com.pbkour.mintrade.contracts.orders.Symbol;
import com.pbkour.mintrade.contracts.orders.Type;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class OrderEntity {
    private UUID id;
    private UUID accountId;
    private Symbol symbol;
    private Side side;
    private Type type;
    private long quantity;
    private Double limitPrice;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;
}
