package com.pbkour.mintrade.contracts.kafka;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrdersCreated extends KafkaPayload {
    private Order order;
}
