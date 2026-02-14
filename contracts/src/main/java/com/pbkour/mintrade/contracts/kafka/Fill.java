package com.pbkour.mintrade.contracts.kafka;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class Fill {
    UUID fillId;
    long quantity;
    double price;
    Instant timestamp;
}
