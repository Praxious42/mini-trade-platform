package com.pbkour.mintrade.portfolio.services;

import com.pbkour.mintrade.commons.generators.PriceGenerator;
import com.pbkour.mintrade.commons.orders.Symbol;
import com.pbkour.mintrade.portfolio.entities.AccountEntity;
import com.pbkour.mintrade.portfolio.entities.AccountLimitEntity;
import com.pbkour.mintrade.portfolio.entities.PositionEntity;
import com.pbkour.mintrade.portfolio.entities.PositionEntity.PositionId;
import com.pbkour.mintrade.portfolio.repositories.AccountLimitsRepository;
import com.pbkour.mintrade.portfolio.repositories.AccountsRepository;
import com.pbkour.mintrade.portfolio.repositories.PositionsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskServiceTest {

    @Mock
    private AccountsRepository accountsRepository;
    @Mock
    private AccountLimitsRepository accountLimitsRepository;
    @Mock
    private PositionsRepository positionsRepository;
    @Mock
    private PriceGenerator priceGenerator;

    @InjectMocks
    private RiskService riskService;

    private UUID accountId;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
    }

    @Test
    void accountNotFound_throwsRiskCheckFailed() {
        when(accountsRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(RiskService.RiskCheckFailedException.class,
            () -> riskService.riskCheck(accountId, Symbol.EURUSD, BigDecimal.ONE));
    }

    @Test
    void accountLimitNotFound_throwsRiskCheckFailed() {
        AccountEntity account = AccountEntity.builder()
            .id(accountId)
            .equity(new BigDecimal("1000.00"))
            .createdAt(Instant.now())
            .build();

        when(accountsRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountLimitsRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(RiskService.RiskCheckFailedException.class,
            () -> riskService.riskCheck(accountId, Symbol.EURUSD, BigDecimal.ONE));
    }

    @Test
    void insufficientMargin_throwsRiskCheckFailed() {
        // equity small so available margin will be less than required
        AccountEntity account = AccountEntity.builder()
            .id(accountId)
            .equity(new BigDecimal("100.00"))
            .createdAt(Instant.now())
            .build();

        AccountLimitEntity limit = AccountLimitEntity.builder()
            .accountId(accountId)
            .maxNotional(new BigDecimal("1000"))
            .maxPosPerSymbol(new BigDecimal("1000"))
            .marginRateFx(new BigDecimal("0.1"))
            .marginRateStock(new BigDecimal("0.1"))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // positions exist and will contribute to used margin
        PositionEntity pos = PositionEntity.builder()
            .id(new PositionId(accountId, Symbol.EURUSD))
            .netQty(new BigDecimal("5"))
            .avgPrice(new BigDecimal("10.00"))
            .build();

        when(accountsRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountLimitsRepository.findById(accountId)).thenReturn(Optional.of(limit));
        when(positionsRepository.findByIdAccountId(accountId)).thenReturn(List.of(pos));
        // priceGenerator returns 10 for any symbol
        when(priceGenerator.generatePrice(Symbol.EURUSD)).thenReturn(new BigDecimal("10.00"));

        // requiredMargin = maxNotional * marginRate = 1000 * 0.1 = 100
        // usedMargin = sum(|5| * 10) * 0.1 = 50 * 0.1 = 5
        // availableMargin = 100 - 5 = 95 < 100 -> fail
        assertThrows(RiskService.RiskCheckFailedException.class,
            () -> riskService.riskCheck(accountId, Symbol.EURUSD, BigDecimal.ZERO));
    }

    @Test
    void maxPositionExceeded_throwsRiskCheckFailed() {
        AccountEntity account = AccountEntity.builder()
            .id(accountId)
            .equity(new BigDecimal("100000.00"))
            .createdAt(Instant.now())
            .build();

        AccountLimitEntity limit = AccountLimitEntity.builder()
            .accountId(accountId)
            .maxNotional(new BigDecimal("100000"))
            .maxPosPerSymbol(new BigDecimal("10"))
            .marginRateFx(new BigDecimal("0.1"))
            .marginRateStock(new BigDecimal("0.1"))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // existing position of 8 units for EURUSD
        PositionEntity pos = PositionEntity.builder()
            .id(new PositionId(accountId, Symbol.EURUSD))
            .netQty(new BigDecimal("8"))
            .avgPrice(new BigDecimal("100.00"))
            .build();

        when(accountsRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountLimitsRepository.findById(accountId)).thenReturn(Optional.of(limit));
        when(positionsRepository.findByIdAccountId(accountId)).thenReturn(List.of(pos));
        when(priceGenerator.generatePrice(Symbol.EURUSD)).thenReturn(new BigDecimal("100.00"));

        // try to add quantity 5 -> new netQty = 13 > maxPosPerSymbol(10)
        assertThrows(RiskService.RiskCheckFailedException.class,
            () -> riskService.riskCheck(accountId, Symbol.EURUSD, new BigDecimal("5")));
    }

    @Test
    void riskCheck_success_noException() {
        AccountEntity account = AccountEntity.builder()
            .id(accountId)
            .equity(new BigDecimal("100000.00"))
            .createdAt(Instant.now())
            .build();

        AccountLimitEntity limit = AccountLimitEntity.builder()
            .accountId(accountId)
            .maxNotional(new BigDecimal("100000"))
            .maxPosPerSymbol(new BigDecimal("100"))
            .marginRateFx(new BigDecimal("0.1"))
            .marginRateStock(new BigDecimal("0.1"))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(accountsRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountLimitsRepository.findById(accountId)).thenReturn(Optional.of(limit));
        // no existing positions
        when(positionsRepository.findByIdAccountId(accountId)).thenReturn(List.of());
        // price needed for order notional calculation
        when(priceGenerator.generatePrice(Symbol.EURUSD)).thenReturn(new BigDecimal("1"));

        assertDoesNotThrow(() -> riskService.riskCheck(accountId, Symbol.EURUSD, new BigDecimal("10")));
    }

    @Test
    void openingNewPosition_exceedsMaxPosPerSymbol_throwsRiskCheckFailed() {
        AccountEntity account = AccountEntity.builder()
            .id(accountId)
            .equity(new BigDecimal("100000.00"))
            .createdAt(Instant.now())
            .build();

        AccountLimitEntity limit = AccountLimitEntity.builder()
            .accountId(accountId)
            .maxNotional(new BigDecimal("100000"))
            .maxPosPerSymbol(new BigDecimal("10"))
            .marginRateFx(new BigDecimal("0.1"))
            .marginRateStock(new BigDecimal("0.1"))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // No existing position for GBPUSD; trying to open with quantity 11 should fail
        when(accountsRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountLimitsRepository.findById(accountId)).thenReturn(Optional.of(limit));
        when(positionsRepository.findByIdAccountId(accountId)).thenReturn(List.of());
        // price stub for GBPUSD used when computing order notional
        when(priceGenerator.generatePrice(Symbol.GBPUSD)).thenReturn(new BigDecimal("1"));

        assertThrows(RiskService.RiskCheckFailedException.class,
            () -> riskService.riskCheck(accountId, Symbol.GBPUSD, new BigDecimal("11")));
    }

    @Test
    void usedMargin_calculatedAcrossMultipleSymbols_andRespectsPrices() {
        AccountEntity account = AccountEntity.builder()
            .id(accountId)
            .equity(new BigDecimal("1000.00"))
            .createdAt(Instant.now())
            .build();

        AccountLimitEntity limit = AccountLimitEntity.builder()
            .accountId(accountId)
            .maxNotional(new BigDecimal("1000"))
            .maxPosPerSymbol(new BigDecimal("1000"))
            .marginRateFx(new BigDecimal("0.1"))
            .marginRateStock(new BigDecimal("0.1"))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // existing positions: EURUSD qty=2 @ price 100, AAPL qty=3 @ price 10
        PositionEntity p1 = PositionEntity.builder()
            .id(new PositionId(accountId, Symbol.EURUSD))
            .netQty(new BigDecimal("2"))
            .avgPrice(new BigDecimal("100.00"))
            .build();

        PositionEntity p2 = PositionEntity.builder()
            .id(new PositionId(accountId, Symbol.AAPL))
            .netQty(new BigDecimal("3"))
            .avgPrice(new BigDecimal("10.00"))
            .build();

        when(accountsRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountLimitsRepository.findById(accountId)).thenReturn(Optional.of(limit));
        when(positionsRepository.findByIdAccountId(accountId)).thenReturn(List.of(p1, p2));

        // price generator must be called with corresponding symbols and return the configured prices
        when(priceGenerator.generatePrice(Symbol.EURUSD)).thenReturn(new BigDecimal("100"));
        when(priceGenerator.generatePrice(Symbol.AAPL)).thenReturn(new BigDecimal("10"));

        // usedMargin = (|2|*100 + |3|*10) * 0.1 = (200 + 30) * 0.1 = 233 * 0.1 = 23.0
        // requiredMargin = maxNotional * marginRate = 1000 * 0.1 = 100
        // availableMargin = equity - usedMargin = 1000 - 23 = 977 >= 100 -> success
        assertDoesNotThrow(() -> riskService.riskCheck(accountId, Symbol.EURUSD, BigDecimal.ZERO));

        // if equity were lower, e.g., 10, available < required and should throw
        AccountEntity poor = AccountEntity.builder()
            .id(accountId)
            .equity(new BigDecimal("10.00"))
            .createdAt(Instant.now())
            .build();
        when(accountsRepository.findById(accountId)).thenReturn(Optional.of(poor));

        assertThrows(RiskService.RiskCheckFailedException.class,
            () -> riskService.riskCheck(accountId, Symbol.EURUSD, BigDecimal.ZERO));
    }

    @Test
    void orderNotional_exceedsMaxNotional_throwsRiskCheckFailed() {
        AccountEntity account = AccountEntity.builder()
            .id(accountId)
            .equity(new BigDecimal("100000.00"))
            .createdAt(Instant.now())
            .build();

        // maxNotional set to 100
        AccountLimitEntity limit = AccountLimitEntity.builder()
            .accountId(accountId)
            .maxNotional(new BigDecimal("100"))
            .maxPosPerSymbol(new BigDecimal("1000"))
            .marginRateFx(new BigDecimal("0.1"))
            .marginRateStock(new BigDecimal("0.1"))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(accountsRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountLimitsRepository.findById(accountId)).thenReturn(Optional.of(limit));
        when(positionsRepository.findByIdAccountId(accountId)).thenReturn(List.of());

        // price 60, quantity 2 => orderNotional = 120 > 100 -> should throw
        when(priceGenerator.generatePrice(Symbol.AAPL)).thenReturn(new BigDecimal("60"));

        assertThrows(RiskService.RiskCheckFailedException.class,
            () -> riskService.riskCheck(accountId, Symbol.AAPL, new BigDecimal("2")));
    }

    @Test
    void orderNotional_equalToMaxNotional_allowsRiskCheck() {
        AccountEntity account = AccountEntity.builder()
            .id(accountId)
            .equity(new BigDecimal("100000.00"))
            .createdAt(Instant.now())
            .build();

        // maxNotional set to 100
        AccountLimitEntity limit = AccountLimitEntity.builder()
            .accountId(accountId)
            .maxNotional(new BigDecimal("100"))
            .maxPosPerSymbol(new BigDecimal("1000"))
            .marginRateFx(new BigDecimal("0.1"))
            .marginRateStock(new BigDecimal("0.1"))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(accountsRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountLimitsRepository.findById(accountId)).thenReturn(Optional.of(limit));
        when(positionsRepository.findByIdAccountId(accountId)).thenReturn(List.of());

        // price 50, quantity 2 => orderNotional = 100 == maxNotional -> should not throw
        when(priceGenerator.generatePrice(Symbol.AAPL)).thenReturn(new BigDecimal("50"));

        assertDoesNotThrow(() -> riskService.riskCheck(accountId, Symbol.AAPL, new BigDecimal("2")));
    }
}

