package com.pbkour.mintrade.portfolio.services;

import com.pbkour.mintrade.commons.kafka.Fill;
import com.pbkour.mintrade.commons.kafka.OrdersFilled;
import com.pbkour.mintrade.commons.orders.Side;
import com.pbkour.mintrade.commons.orders.Symbol;
import com.pbkour.mintrade.commons.services.ProcessedEventRecorder;
import com.pbkour.mintrade.portfolio.entities.AccountEntity;
import com.pbkour.mintrade.portfolio.entities.PositionEntity;
import com.pbkour.mintrade.portfolio.entities.PositionEntity.PositionId;
import com.pbkour.mintrade.portfolio.repositories.AccountsRepository;
import com.pbkour.mintrade.portfolio.repositories.PositionsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {
    @Mock
    ProcessedEventRecorder processedEventRecorder;
    @Captor
    ArgumentCaptor<AccountEntity> accountCaptor;
    @Captor
    ArgumentCaptor<PositionEntity> positionCaptor;
    @Mock
    private PositionsRepository positionsRepository;
    @Mock
    private AccountsRepository accountsRepository;
    @InjectMocks
    private PortfolioService portfolioService;
    private UUID accountId;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        when(processedEventRecorder.markEventProcessed(any())).thenReturn(true);
        accountId = UUID.randomUUID();
        eventId = UUID.randomUUID();
    }

    @Test
    void testProcessOrdersFilled_buy_createsPosition_and_updatesAccountEquity() {
        // given
        AccountEntity existing = AccountEntity.builder()
            .id(accountId)
            .equity(new BigDecimal("1000.00"))
            .createdAt(Instant.now())
            .build();

        when(accountsRepository.findById(accountId)).thenReturn(Optional.of(existing));

        var fills = List.of(
            Fill.builder().fillId(UUID.randomUUID()).quantity(new BigDecimal("2")).price(new BigDecimal("10.00")).timestamp(Instant.now()).build(),
            Fill.builder().fillId(UUID.randomUUID()).quantity(new BigDecimal("3")).price(new BigDecimal("12.00")).timestamp(Instant.now()).build()
        );

        OrdersFilled payload = OrdersFilled.builder()
            .eventId(eventId)
            .accountId(accountId)
            .symbol(Symbol.AAPL)
            .side(Side.BUY)
            .fills(fills)
            .build();

        // when
        portfolioService.processOrdersFilled(payload);

        // then
        verify(accountsRepository).save(accountCaptor.capture());
        AccountEntity savedAccount = accountCaptor.getValue();

        // equity reduction: 2*10 + 3*12 = 20 + 36 = 56
        assertEquals(0, savedAccount.getEquity().compareTo(new BigDecimal("944.00")));

        verify(positionsRepository).save(positionCaptor.capture());
        PositionEntity savedPos = positionCaptor.getValue();
        PositionId expectedId = new PositionId(accountId, Symbol.AAPL);
        assertEquals(expectedId.getAccountId(), savedPos.getId().getAccountId());
        assertEquals(expectedId.getSymbol(), savedPos.getId().getSymbol());

        // new quantity = 2 + 3 = 5
        assertEquals(0, savedPos.getNetQty().compareTo(new BigDecimal("5")));
        // weighted avg price = (2*10 + 3*12) / 5 = 56 / 5 = 11.20
        assertEquals(0, savedPos.getAvgPrice().compareTo(new BigDecimal("11.20")));
    }

    @Test
    void testProcessOrdersFilled_sell_decreasesPosition_and_updatesAccountEquity() {
        // given
        AccountEntity existing = AccountEntity.builder()
            .id(accountId)
            .equity(new BigDecimal("500.00"))
            .createdAt(Instant.now())
            .build();
        when(accountsRepository.findById(accountId)).thenReturn(Optional.of(existing));

        PositionEntity oldPos = PositionEntity.builder()
            .id(new PositionId(accountId, Symbol.AAPL))
            .netQty(new BigDecimal("5"))
            .avgPrice(new BigDecimal("10.00"))
            .build();

        when(positionsRepository.findById(any())).thenReturn(Optional.of(oldPos));

        var fills = List.of(
            Fill.builder().fillId(UUID.randomUUID()).quantity(new BigDecimal("3")).price(new BigDecimal("12.00")).timestamp(Instant.now()).build()
        );

        OrdersFilled payload = OrdersFilled.builder()
            .eventId(eventId)
            .accountId(accountId)
            .symbol(Symbol.AAPL)
            .side(Side.SELL)
            .fills(fills)
            .build();

        // when
        portfolioService.processOrdersFilled(payload);

        // then
        // equity increase: 3 * 12 = 36 -> 500 + 36 = 536
        verify(accountsRepository).save(accountCaptor.capture());
        AccountEntity savedAccount = accountCaptor.getValue();
        assertEquals(0, savedAccount.getEquity().compareTo(new BigDecimal("536.00")));

        verify(positionsRepository).save(positionCaptor.capture());
        PositionEntity savedPos = positionCaptor.getValue();
        // netQty should be 5 - 3 = 2
        assertEquals(0, savedPos.getNetQty().compareTo(new BigDecimal("2")));
        // avg price remains old avgPrice (10.00)
        assertEquals(0, savedPos.getAvgPrice().compareTo(new BigDecimal("10.00")));
    }

    @Test
    void testProcessOrdersFilled_skipsWhenAlreadyProcessed() {
        when(processedEventRecorder.markEventProcessed(any())).thenReturn(false);
        OrdersFilled payload = OrdersFilled.builder()
            .eventId(eventId)
            .accountId(accountId)
            .symbol(Symbol.AAPL)
            .side(Side.BUY)
            .fills(List.of())
            .build();

        portfolioService.processOrdersFilled(payload);

        verify(accountsRepository, never()).findById(any());
        verify(positionsRepository, never()).save(any());
    }

    @Test
    void testProcessOrdersFilled_decreaseNonexistentPosition_throws() {
        when(accountsRepository.findById(accountId)).thenReturn(Optional.of(AccountEntity.builder()
            .id(accountId)
            .equity(new BigDecimal("100.00"))
            .createdAt(Instant.now())
            .build()));

        when(positionsRepository.findById(any())).thenReturn(Optional.empty());

        var fills = List.of(
            Fill.builder().fillId(UUID.randomUUID()).quantity(new BigDecimal("1")).price(new BigDecimal("10.00")).timestamp(Instant.now()).build()
        );

        OrdersFilled payload = OrdersFilled.builder()
            .eventId(eventId)
            .accountId(accountId)
            .symbol(Symbol.AAPL)
            .side(Side.SELL)
            .fills(fills)
            .build();

        assertThrows(IllegalStateException.class, () -> portfolioService.processOrdersFilled(payload));
    }

    @Test
    void testProcessOrdersFilled_decreaseMoreThanExisting_throws() {
        when(accountsRepository.findById(accountId)).thenReturn(Optional.of(AccountEntity.builder()
            .id(accountId)
            .equity(new BigDecimal("100.00"))
            .createdAt(Instant.now())
            .build()));

        PositionEntity oldPos = PositionEntity.builder()
            .id(new PositionId(accountId, Symbol.AAPL))
            .netQty(new BigDecimal("1"))
            .avgPrice(new BigDecimal("10.00"))
            .build();

        when(positionsRepository.findById(any())).thenReturn(Optional.of(oldPos));

        var fills = List.of(
            Fill.builder().fillId(UUID.randomUUID()).quantity(new BigDecimal("2")).price(new BigDecimal("10.00")).timestamp(Instant.now()).build()
        );

        OrdersFilled payload = OrdersFilled.builder()
            .eventId(eventId)
            .accountId(accountId)
            .symbol(Symbol.AAPL)
            .side(Side.SELL)
            .fills(fills)
            .build();

        assertThrows(IllegalStateException.class, () -> portfolioService.processOrdersFilled(payload));
    }
}
