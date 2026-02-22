package com.pbkour.mintrade.commons.kafka;

import com.pbkour.mintrade.commons.orders.Side;
import com.pbkour.mintrade.commons.orders.Symbol;
import com.pbkour.mintrade.commons.orders.Type;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private UUID orderId;
    private UUID accountId;
    private Symbol symbol;
    private Side side;
    private Type type;
    private BigDecimal quantity;
    private BigDecimal limitPrice;
}
