package com.pbkour.mintrade.portfolio.services;

import com.pbkour.mintrade.commons.kafka.OrdersFilled;
import com.pbkour.mintrade.portfolio.entities.ProcessedEventEntity;
import com.pbkour.mintrade.portfolio.repositories.PositionsRepository;
import com.pbkour.mintrade.portfolio.repositories.ProcessedEventsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {
    private final PositionsRepository positionsRepository;
    private final ProcessedEventsRepository processedEventsRepository;

    @Transactional
    public void processOrdersFilled(OrdersFilled payload) {
        UUID eventId = payload.getEventId();
        if (eventId == null) {
            log.warn("Received OrdersFilled with null eventId, skipping");
            return;
        }

        if (processedEventsRepository.existsById(eventId)) {
            log.info("Ignoring already processed event eventId={}", eventId);
            return;
        }

        try {
            processedEventsRepository.save(new ProcessedEventEntity(eventId, Instant.now()));
            processedEventsRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            log.info("Event already processed (concurrent) eventId={}", eventId);
            return;
        }

        //TODO uncomment the below and fix it, that only understand positive values, we need to update  the OrdersFilled to have a side to correctly compute the current positions
        // Apply fills to positions (simple aggregate: assume fills are positive buys)
//        try {
//            UUID accountId = payload.getAccountId();
//            Symbol symbol = payload.getSymbol();
//
//            List<Fill> fills = payload.getFills();
//            if (fills == null || fills.isEmpty()) {
//                log.info("OrdersFilled has no fills for eventId={}, marking processed", eventId);
//                return;
//            }
//
//            // compute total quantity and average fill price (weighted)
//            BigDecimal totalQty = BigDecimal.ZERO;
//            BigDecimal weightedPriceSum = BigDecimal.ZERO;
//            for (Fill f : fills) {
//                BigDecimal qty = BigDecimal.valueOf(f.getQuantity());
//                totalQty = totalQty.add(qty);
//                weightedPriceSum = weightedPriceSum.add(f.getPrice().multiply(qty));
//            }
//            if (totalQty.compareTo(BigDecimal.ZERO) == 0) {
//                log.info("Total fill quantity is zero for eventId={}, marking processed", eventId);
//                return;
//            }
//
//            PositionEntity.PositionId pid = new PositionEntity.PositionId(accountId, symbol);
//            PositionEntity existing = positionsRepository.findById(pid).orElse(null);
//
//            BigDecimal existingQty = BigDecimal.ZERO;
//            BigDecimal existingAvg = BigDecimal.ZERO;
//            if (existing != null) {
//                existingQty = existing.getNetQty();
//                existingAvg = existing.getAvgPrice();
//            }
//
//            BigDecimal avgFillPrice = weightedPriceSum.divide(totalQty, 8, RoundingMode.HALF_UP);
//            BigDecimal newQty = existingQty.add(totalQty);
//            BigDecimal newAvg;
//            if (newQty.compareTo(BigDecimal.ZERO) == 0) {
//                newAvg = BigDecimal.ZERO;
//            } else {
//                BigDecimal existingValue = existingQty.multiply(existingAvg);
//                BigDecimal fillValue = totalQty.multiply(avgFillPrice);
//                newAvg = existingValue.add(fillValue).divide(newQty, 8, RoundingMode.HALF_UP);
//            }
//
//            PositionEntity posToSave = PositionEntity.builder()
//                .id(pid)
//                .netQty(newQty.setScale(4, RoundingMode.HALF_UP))
//                .avgPrice(newAvg.setScale(5, RoundingMode.HALF_UP))
//                .build();
//
//            positionsRepository.save(posToSave);
//
//            log.info("Processed OrdersFilled eventId={} accountId={} symbol={} qty={} avgPrice={}",
//                eventId, accountId, symbol, totalQty, avgFillPrice);
//        } catch (Exception e) {
//            log.error("Failed to process OrdersFilled eventId={}", payload.getEventId(), e);
//            throw e;
//        }
    }
}
