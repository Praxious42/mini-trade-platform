package com.pbkour.mintrade.contracts.kafka;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class OrdersCreated extends KafkaPayload {
    private Order order;
}
