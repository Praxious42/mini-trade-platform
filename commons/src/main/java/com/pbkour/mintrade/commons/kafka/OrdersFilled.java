package com.pbkour.mintrade.commons.kafka;

import com.pbkour.mintrade.commons.orders.Side;
import com.pbkour.mintrade.commons.orders.Symbol;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrdersFilled extends KafkaPayload {
    private UUID orderId;
    private UUID accountId;
    private Symbol symbol;
    private Side side;
    private List<Fill> fills;
}
