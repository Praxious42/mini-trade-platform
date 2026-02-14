package com.pbkour.mintrade.contracts.kafka;

import com.pbkour.mintrade.contracts.orders.Side;
import com.pbkour.mintrade.contracts.orders.Symbol;
import com.pbkour.mintrade.contracts.orders.Type;
import lombok.Data;

import java.util.UUID;

@Data
public class Order {
    private UUID orderId;
    private UUID accountId;
    private Symbol symbol;
    private Side side;
    private Type type;
    private Long quantity;
    private Double limitPrice;
}
