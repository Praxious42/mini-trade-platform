package com.pbkour.mintrade.contracts.kafka;

import com.pbkour.mintrade.contracts.orders.RejectionReason;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class OrdersRejected extends KafkaPayload {
    private UUID orderId;
    private RejectionReason reason;
}
