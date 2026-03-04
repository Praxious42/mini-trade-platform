package com.pbkour.mintrade.commons.responses;

import com.pbkour.mintrade.commons.orders.Side;
import com.pbkour.mintrade.commons.orders.Status;
import com.pbkour.mintrade.commons.orders.Symbol;
import com.pbkour.mintrade.commons.orders.Type;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private UUID orderId;
    private UUID accountId;
    private Symbol symbol;
    private Side side;
    private Type type;
    private BigDecimal quantity;
    private BigDecimal limitPrice;
    private Status status;
    private Instant createdAt;
    private Instant updatedAt;
}
