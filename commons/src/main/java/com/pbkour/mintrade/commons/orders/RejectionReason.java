package com.pbkour.mintrade.commons.orders;

public enum RejectionReason {
    RISK_LIMIT,
    INSUFFICIENT_POSITION,
    REQUIRED_MARGIN,
    POSITION_LIMIT,
    NOTIONAL_LIMIT,
    RANDOM_FAILURE
}
