package com.pbkour.mintrade.portfolio.services;

import com.pbkour.mintrade.commons.kafka.Fill;
import com.pbkour.mintrade.commons.kafka.OrdersFilled;
import com.pbkour.mintrade.commons.orders.Side;
import com.pbkour.mintrade.portfolio.entities.PositionEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static java.util.Optional.ofNullable;

@Component
public class PortfolioCalculator {

    public BigDecimal calculateNewEquity(OrdersFilled payload, BigDecimal currentEquity) {
        BigDecimal fillValue = totalFillValue(payload.getFills());
        Side side = requireSide(payload);

        return side == Side.BUY
            ? currentEquity.subtract(fillValue)
            : currentEquity.add(fillValue);
    }

    public NewPosition calculateNewPosition(OrdersFilled payload, PositionEntity oldPosition) {
        Side side = requireSide(payload);

        return side == Side.BUY
            ? increasePosition(payload, oldPosition)
            : decreasePosition(payload, oldPosition);
    }

    private Side requireSide(OrdersFilled payload) {
        return ofNullable(payload.getSide()).orElseThrow(() ->
            new IllegalStateException("Side is required in OrdersFilled eventId=" + payload.getEventId()));
    }

    private BigDecimal totalFillValue(List<Fill> fills) {
        return fills.stream()
            .map(fill -> fill.getQuantity().multiply(fill.getPrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalFillQuantity(List<Fill> fills) {
        return fills.stream()
            .map(Fill::getQuantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private NewPosition increasePosition(OrdersFilled payload, PositionEntity oldPosition) {
        BigDecimal newQuantity = totalFillQuantity(payload.getFills());
        BigDecimal newPrice = totalFillValue(payload.getFills()).divide(newQuantity, 2, RoundingMode.HALF_UP);

        if (oldPosition == null || oldPosition.getNetQty().compareTo(BigDecimal.ZERO) <= 0) {
            return new NewPosition(newQuantity, newPrice);
        }

        BigDecimal oldQuantity = oldPosition.getNetQty();
        BigDecimal oldAvgPrice = oldPosition.getAvgPrice();
        BigDecimal oldValue = oldQuantity.multiply(oldAvgPrice);

        BigDecimal newValue = newQuantity.multiply(newPrice);
        BigDecimal totalValue = oldValue.add(newValue);
        BigDecimal totalQuantity = oldQuantity.add(newQuantity);

        return new NewPosition(totalQuantity, totalValue.divide(totalQuantity, 2, RoundingMode.HALF_UP));
    }

    private NewPosition decreasePosition(OrdersFilled payload, PositionEntity oldPosition) {
        BigDecimal newQuantityToDecrease = totalFillQuantity(payload.getFills());

        if (oldPosition == null) {
            throw new IllegalStateException(
                "Cannot decrease position that does not exist: accountId=" + payload.getAccountId() + " symbol=" + payload.getSymbol());
        }

        if (oldPosition.getNetQty().compareTo(newQuantityToDecrease) < 0) {
            throw new IllegalStateException(
                "Cannot decrease position more than existing quantity: accountId=" + payload.getAccountId() + " symbol=" + payload.getSymbol());
        }

        BigDecimal newTotalQuantity = oldPosition.getNetQty().subtract(newQuantityToDecrease);
        if (newTotalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return new NewPosition(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        return new NewPosition(newTotalQuantity, oldPosition.getAvgPrice());
    }

    public record NewPosition(BigDecimal netQty, BigDecimal avgPrice) {
    }
}


