package com.pbkour.mintrade.portfolio.services;

import com.pbkour.mintrade.commons.kafka.OrdersFilled;
import com.pbkour.mintrade.commons.services.ProcessedEventRecorder;
import com.pbkour.mintrade.portfolio.entities.AccountEntity;
import com.pbkour.mintrade.portfolio.entities.PositionEntity;
import com.pbkour.mintrade.portfolio.repositories.AccountsRepository;
import com.pbkour.mintrade.portfolio.repositories.PositionsRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.StandardException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {
    private final PositionsRepository positionsRepository;
    private final AccountsRepository accountsRepository;
    private final ProcessedEventRecorder processedEventRecorder;
    private final PortfolioCalculator portfolioCalculator;

    @Transactional
    public void processOrdersFilled(OrdersFilled payload) {
        UUID eventId = payload.getEventId();
        processedEventRecorder.processIfNotProcessed(eventId, "OrdersFilled", () -> {
            // Update account equity based on fills
            try {
                AccountEntity account = accountsRepository.findById(payload.getAccountId())
                    .orElseThrow(() -> new PortfolioServiceException("Account not found with id=" + payload.getAccountId()));
                account.setEquity(portfolioCalculator.calculateNewEquity(payload, account.getEquity()));

                accountsRepository.save(account);

            } catch (Exception e) {
                log.error("Failed to update account during OrdersFilled eventId={}", payload.getEventId(), e);
                throw e;
            }

            // Process the fills and update positions
            try {
                PositionEntity.PositionId pid = new PositionEntity.PositionId(payload.getAccountId(), payload.getSymbol());
                PositionEntity oldPosition = positionsRepository.findById(pid).orElse(null);
                PortfolioCalculator.NewPosition newPosition = portfolioCalculator.calculateNewPosition(payload, oldPosition);

                PositionEntity posToSave = PositionEntity.builder()
                    .id(pid)
                    .netQty(newPosition.netQty())
                    .avgPrice(newPosition.avgPrice())
                    .build();

                positionsRepository.save(posToSave);

                log.info("New position saved with id={}", posToSave.getId());
            } catch (Exception e) {
                log.error("Failed to update positions during OrdersFilled eventId={}", payload.getEventId(), e);
                throw e;
            }
        });
    }


    @StandardException
    public static class PortfolioServiceException extends RuntimeException {
    }
}
