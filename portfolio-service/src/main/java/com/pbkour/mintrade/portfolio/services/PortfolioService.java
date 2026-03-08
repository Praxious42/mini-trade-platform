package com.pbkour.mintrade.portfolio.services;

import com.pbkour.mintrade.commons.kafka.Fill;
import com.pbkour.mintrade.commons.kafka.OrdersFilled;
import com.pbkour.mintrade.commons.orders.Side;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import static java.util.Optional.ofNullable;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {
    private final PositionsRepository positionsRepository;
    private final AccountsRepository accountsRepository;
    private final ProcessedEventRecorder processedEventRecorder;

    @Transactional
    public void processOrdersFilled(OrdersFilled payload) {
        UUID eventId = payload.getEventId();

        if (!processedEventRecorder.markEventProcessed(eventId)) {
            log.info("Skipping processing for already-processed eventId={}", eventId);
            return;
        }

        // Update account equity based on fills
        try {
            AccountEntity account = accountsRepository.findById(payload.getAccountId())
                .orElseThrow(() -> new PortfolioServiceException("Account not found with id=" + payload.getAccountId()));
            BigDecimal equity = account.getEquity();
            BigDecimal equityToAddReduce = payload.getFills().stream().map(fill -> fill.getQuantity().multiply(fill.getPrice())).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal newEquity;

            if (payload.getSide() == Side.BUY) {
                newEquity = equity.subtract(equityToAddReduce);
            } else {
                newEquity = equity.add(equityToAddReduce);
            }

            account.setEquity(newEquity);
            accountsRepository.save(account);

        } catch (Exception e) {
            log.error("Failed to update account during OrdersFilled eventId={}", payload.getEventId(), e);
            throw e;
        }

        // Process the fills and update positions
        try {
            PositionEntity.PositionId pid = new PositionEntity.PositionId(payload.getAccountId(), payload.getSymbol());
            PositionEntity oldPosition = positionsRepository.findById(pid).orElse(null);
            Side side = ofNullable(payload.getSide()).orElseThrow(() -> new PortfolioServiceException("Side is required in OrdersFilled eventId=" + payload.getEventId()));

            NewPosition newPosition;
            if (side.equals(Side.BUY)) {
                log.info("Received OrdersFilled with SIDE=BUY");
                newPosition = increasePosition(payload, oldPosition);
            } else {
                log.info("Received OrdersFilled with SIDE=SELL");
                newPosition = decreasePosition(payload, oldPosition);
            }

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
    }

    private NewPosition decreasePosition(OrdersFilled payload, PositionEntity oldPosition) {
        BigDecimal newQuantityToDecrease = payload.getFills().stream()
            .map(Fill::getQuantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (oldPosition == null) {
            log.error("Attempting to decrease position that does not exist. accountId={} symbol={} decreaseBy={}",
                payload.getAccountId(), payload.getSymbol(), newQuantityToDecrease);
            throw new IllegalStateException("Cannot decrease position that does not exist");
        }

        if (oldPosition.getNetQty().compareTo(newQuantityToDecrease) < 0) {
            log.error("Attempting to decrease position more than existing quantity. accountId={} symbol={} oldQty={} decreaseBy={}",
                payload.getAccountId(), payload.getSymbol(), oldPosition.getNetQty(), newQuantityToDecrease);
            throw new IllegalStateException("Cannot decrease position more than existing quantity");
        }

        BigDecimal newTotalQuantity = oldPosition.getNetQty().subtract(newQuantityToDecrease);

        if (newTotalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return new NewPosition(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        return new NewPosition(newTotalQuantity, oldPosition.getAvgPrice());
    }

    private NewPosition increasePosition(OrdersFilled payload, PositionEntity oldPosition) {
        BigDecimal newQuantity = payload.getFills().stream()
            .map(Fill::getQuantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal newPrice = payload.getFills().stream()
            .map(fill -> fill.getQuantity().multiply(fill.getPrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(newQuantity, 2, RoundingMode.HALF_UP);

        if (oldPosition == null || oldPosition.getNetQty().compareTo(BigDecimal.ZERO) <= 0) {
            return new NewPosition(newQuantity, newPrice);
        } else {
            //new_avg = (old_qty * old_avg + fill_qty * fill_price) / (old_qty + fill_qty)
            BigDecimal oldQuantity = oldPosition.getNetQty();
            BigDecimal oldAvgPrice = oldPosition.getAvgPrice();
            BigDecimal oldPrice = oldQuantity.multiply(oldAvgPrice);

            BigDecimal newValue = newQuantity.multiply(newPrice);
            BigDecimal sumPrice = oldPrice.add(newValue);

            BigDecimal newTotalQuantity = oldQuantity.add(newQuantity);
            BigDecimal newAvgPrice = sumPrice.divide(newTotalQuantity, 2, RoundingMode.HALF_UP);

            return new NewPosition(newTotalQuantity, newAvgPrice);
        }
    }

    private record NewPosition(BigDecimal netQty, BigDecimal avgPrice) {
    }

    @StandardException
    public static class PortfolioServiceException extends RuntimeException {
    }
}
