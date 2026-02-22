package com.pbkour.mintrade.portfolio.services;

import com.pbkour.mintrade.commons.kafka.Fill;
import com.pbkour.mintrade.commons.kafka.OrdersFilled;
import com.pbkour.mintrade.portfolio.entities.PositionEntity;
import com.pbkour.mintrade.portfolio.entities.ProcessedEventEntity;
import com.pbkour.mintrade.portfolio.repositories.PositionsRepository;
import com.pbkour.mintrade.portfolio.repositories.ProcessedEventsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

//        try {
//            PositionEntity.PositionId pid = new PositionEntity.PositionId(payload.getAccountId(), payload.getSymbol());
//            PositionEntity oldPosition = positionsRepository.findById(pid).orElse(null);
//            Side side = payload.getSide();
//
//            NewPosition newPosition;
//            if (side.equals(Side.BUY)) {
//                newPosition = increasePosition(payload, oldPosition);
//            } else {
//
//            }
//
//            PositionEntity posToSave = PositionEntity.builder()
//                .id(pid)
//                .netQty(null)
//                .avgPrice(null)
//                .build();
//
//            positionsRepository.save(posToSave);
//
////            log.info("Processed OrdersFilled eventId={} accountId={} symbol={} qty={} avgPrice={}",
////                eventId, accountId, symbol, totalQty, avgFillPrice);
//        } catch (Exception e) {
//            log.error("Failed to process OrdersFilled eventId={}", payload.getEventId(), e);
//            throw e;
//        }
    }

    //TODO lots of work
    private NewPosition increasePosition(OrdersFilled payload, PositionEntity oldPosition) {
//        BigDecimal newQty = BigDecimal.ZERO;
        BigDecimal newAvgPrice = BigDecimal.ZERO;
        if (oldPosition.getNetQty().compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal fillPrice = payload.getFills().stream().map(Fill::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
            newAvgPrice = fillPrice;
        }
        return new NewPosition(null, null);
    }

    private record NewPosition(BigDecimal netQty, BigDecimal avgPrice) {
    }
}
