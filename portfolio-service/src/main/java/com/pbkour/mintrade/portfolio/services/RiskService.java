package com.pbkour.mintrade.portfolio.services;

import com.pbkour.mintrade.commons.generators.PriceGenerator;
import com.pbkour.mintrade.commons.orders.Symbol;
import com.pbkour.mintrade.portfolio.entities.AccountEntity;
import com.pbkour.mintrade.portfolio.entities.AccountLimitEntity;
import com.pbkour.mintrade.portfolio.entities.PositionEntity;
import com.pbkour.mintrade.portfolio.repositories.AccountLimitsRepository;
import com.pbkour.mintrade.portfolio.repositories.AccountsRepository;
import com.pbkour.mintrade.portfolio.repositories.PositionsRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.StandardException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RiskService {
    private final AccountsRepository accountsRepository;
    private final AccountLimitsRepository accountLimitsRepository;
    private final PositionsRepository positionsRepository;
    private final PriceGenerator priceGenerator;

    @Transactional
    public void riskCheck(UUID accountId, Symbol symbol, BigDecimal quantity) {
        // For simplicity, we only do FX
        AccountEntity account = accountsRepository.findById(accountId)
            .orElseThrow(() -> new RiskCheckFailedException("account not found"));
        BigDecimal equity = account.getEquity();
        AccountLimitEntity accountLimit = accountLimitsRepository.findById(accountId)
            .orElseThrow(() -> new RiskCheckFailedException("account limit not found"));
        BigDecimal maxNotional = accountLimit.getMaxNotional();

        BigDecimal marginRate = accountLimit.getMarginRateFx();
        Map<Symbol, PositionEntity> positions = positionsRepository.findByIdAccountId(accountId).stream()
            .collect(Collectors.toMap(o -> o.getId().getSymbol(), o -> o));

        BigDecimal orderNotional = quantity.abs().multiply(priceGenerator.generatePrice(symbol));

        if (orderNotional.compareTo(maxNotional) > 0) {
            throw new RiskCheckFailedException("Risk check failed: order notional " + orderNotional + " exceeds max notional " + maxNotional);
        }

        BigDecimal requiredMargin = maxNotional.multiply(marginRate);

        BigDecimal usedMargin = positions.values().stream()
            .map(positionEntity -> {
                Symbol posSymbol = positionEntity.getId().getSymbol();
                return positionEntity.getNetQty().abs().multiply(priceGenerator.generatePrice(posSymbol));
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .multiply(marginRate);

        BigDecimal availableMargin = equity.subtract(usedMargin);

        if (availableMargin.compareTo(requiredMargin) < 0) {
            throw new RiskCheckFailedException("Risk check failed: available margin " + availableMargin + " is less than required margin " + requiredMargin);
        }

        BigDecimal maxPosPerSymbol = accountLimit.getMaxPosPerSymbol();
        PositionEntity position = positions.get(symbol);
        BigDecimal netQty = position != null ? position.getNetQty().add(quantity) : quantity;

        if (netQty.abs().compareTo(maxPosPerSymbol) > 0) {
            throw new RiskCheckFailedException("Risk check failed: net quantity " + netQty + " exceeds max position per symbol " + maxPosPerSymbol);
        }
    }

    @StandardException
    public static class RiskCheckFailedException extends RuntimeException {
    }
}
