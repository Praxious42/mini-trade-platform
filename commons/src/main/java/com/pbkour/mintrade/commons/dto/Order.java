package com.pbkour.mintrade.commons.dto;

import com.pbkour.mintrade.commons.orders.Side;
import com.pbkour.mintrade.commons.orders.Symbol;
import com.pbkour.mintrade.commons.orders.Type;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class Order {
    private UUID accountId;
    private Symbol symbol;
    private Side side;
    private Type type;
    private BigDecimal quantity;
    private BigDecimal limitPrice;
    private Instant createdAt;
    private Instant updatedAt;
}
