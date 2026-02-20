package com.pbkour.mintrade.commons.kafka;

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
public class Fill {
    private UUID fillId;
    private long quantity;
    private BigDecimal price;
    private Instant timestamp;
}
