package com.pbkour.mintrade.portfolio.services;

import com.pbkour.mintrade.commons.generators.PriceGenerator;
import com.pbkour.mintrade.commons.orders.RejectionReason;
import com.pbkour.mintrade.commons.orders.Side;
import com.pbkour.mintrade.commons.orders.Symbol;
import com.pbkour.mintrade.portfolio.entities.AccountEntity;
import com.pbkour.mintrade.portfolio.entities.AccountLimitEntity;
import com.pbkour.mintrade.portfolio.entities.PositionEntity;
import com.pbkour.mintrade.portfolio.repositories.AccountLimitsRepository;
import com.pbkour.mintrade.portfolio.repositories.AccountsRepository;
import com.pbkour.mintrade.portfolio.repositories.PositionsRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
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
    public RiskCheckResult riskCheck(UUID accountId, Symbol symbol, BigDecimal quantity, Side side) {
        Map<Symbol, PositionEntity> positions = positionsRepository.findByIdAccountId(accountId).stream()
            .collect(Collectors.toMap(o -> o.getId().getSymbol(), o -> o));

        if (Side.SELL.equals(side)) {
            PositionEntity position = positions.get(symbol);
            if (position == null || position.getNetQty().abs().compareTo(quantity.abs()) < 0) {
                return RiskCheckResult.builder()
                    .allowed(false)
                    .reason(RejectionReason.INSUFFICIENT_POSITION.name())
                    .requiredMargin(BigDecimal.ZERO)
                    .availableMargin(BigDecimal.ZERO)
                    .build();
            }
            return RiskCheckResult.builder()
                .allowed(true)
                .reason("")
                .requiredMargin(BigDecimal.ZERO)
                .availableMargin(BigDecimal.ZERO)
                .build();
        }

        // For simplicity, we only do FX
        AccountEntity account = accountsRepository.findById(accountId)
            .orElseThrow(() -> new RiskCheckFailedException("account not found"));
        BigDecimal equity = account.getEquity();
        AccountLimitEntity accountLimit = accountLimitsRepository.findById(accountId)
            .orElseThrow(() -> new RiskCheckFailedException("account limit not found"));
        BigDecimal maxNotional = accountLimit.getMaxNotional();

        BigDecimal marginRate = accountLimit.getMarginRateFx();


        BigDecimal orderNotional = quantity.abs().multiply(priceGenerator.generatePrice(symbol));

        BigDecimal requiredMargin = maxNotional.multiply(marginRate);
        BigDecimal usedMargin = positions.values().stream()
            .map(positionEntity -> {
                Symbol posSymbol = positionEntity.getId().getSymbol();
                return positionEntity.getNetQty().abs().multiply(priceGenerator.generatePrice(posSymbol));
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .multiply(marginRate);

        BigDecimal availableMargin = equity.subtract(usedMargin);

        // notional check is for a single transaction, not the account as a whole
        if (orderNotional.compareTo(maxNotional) > 0) {
            return RiskCheckResult.builder()
                .allowed(false)
                .reason(RejectionReason.NOTIONAL_LIMIT.name())
                .requiredMargin(requiredMargin)
                .availableMargin(equity)
                .build();
        }

        if (availableMargin.compareTo(requiredMargin) < 0) {
            return RiskCheckResult.builder()
                .allowed(false)
                .reason(RejectionReason.REQUIRED_MARGIN.name())
                .requiredMargin(requiredMargin)
                .availableMargin(availableMargin)
                .build();
        }

        BigDecimal maxPosPerSymbol = accountLimit.getMaxPosPerSymbol();
        PositionEntity position = positions.get(symbol);
        BigDecimal netQty = position != null ? position.getNetQty().add(quantity) : quantity;

        if (netQty.abs().compareTo(maxPosPerSymbol) > 0) {
            return RiskCheckResult.builder()
                .allowed(false)
                .reason(RejectionReason.POSITION_LIMIT.name())
                .requiredMargin(requiredMargin)
                .availableMargin(availableMargin)
                .build();
        }

        return RiskCheckResult.builder()
            .allowed(true)
            .reason("")
            .requiredMargin(requiredMargin)
            .availableMargin(availableMargin)
            .build();
    }

    @Builder
    public record RiskCheckResult(boolean allowed, String reason, BigDecimal requiredMargin,
                                  BigDecimal availableMargin) {
    }
}
