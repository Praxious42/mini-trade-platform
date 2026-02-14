package com.pbkour.mintrade.contracts.kafka;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class Fill {
    private UUID fillId;
    private long quantity;
    private double price;
    private Instant timestamp;
}
