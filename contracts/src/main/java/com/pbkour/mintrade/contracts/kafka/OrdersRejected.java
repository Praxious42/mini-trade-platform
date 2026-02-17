package com.pbkour.mintrade.contracts.kafka;

import com.pbkour.mintrade.contracts.orders.RejectionReason;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrdersRejected extends KafkaPayload {
    private UUID orderId;
    private RejectionReason reason;
}
