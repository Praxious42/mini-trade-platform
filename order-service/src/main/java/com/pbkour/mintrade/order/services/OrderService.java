package com.pbkour.mintrade.order.services;

import com.pbkour.mintrade.commons.RiskCheckRequest;
import com.pbkour.mintrade.commons.RiskCheckResponse;
import com.pbkour.mintrade.commons.RiskCheckServiceGrpc;
import com.pbkour.mintrade.commons.dto.Order;
import com.pbkour.mintrade.commons.kafka.Fill;
import com.pbkour.mintrade.commons.kafka.OrdersFilled;
import com.pbkour.mintrade.commons.kafka.OrdersRejected;
import com.pbkour.mintrade.commons.orders.Status;
import com.pbkour.mintrade.order.entities.OrderEntity;
import com.pbkour.mintrade.order.repositories.OrdersRepository;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.experimental.StandardException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrdersRepository ordersRepository;
    private final ApplicationEventPublisher publisher;
    private final RiskCheckServiceGrpc.RiskCheckServiceBlockingStub riskCheckServiceBlockingStub;
    private final RejectedOrderService rejectedOrderService;

    @Transactional
    public UUID createOrder(Order order) {
        RiskCheckResponse riskCheckResponse = checkRisk(order);
        if (!riskCheckResponse.getAllowed()) {
            log.info("Order rejected by risk check for accountId={}, symbol={}, side={}, quantity={}: {}",
                order.getAccountId(), order.getSymbol(), order.getSide(), order.getQuantity(), riskCheckResponse.getReason());
            // persist rejected order in a separate transaction so it isn't rolled back with the outer transaction
            rejectedOrderService.persistRejectedOrder(order);
            throw new OrderRejectedException("Order rejected by risk check: " + riskCheckResponse.getReason());
        }

        OrderEntity entity = OrderEntity.builder()
            .accountId(order.getAccountId())
            .symbol(order.getSymbol())
            .side(order.getSide())
            .type(order.getType())
            .quantity(order.getQuantity())
            .limitPrice(order.getLimitPrice())
            .status(Status.NEW)
            .version(0)
            .build();

        OrderEntity savedOrder = ordersRepository.save(entity);

        publisher.publishEvent(new OrderSavedEvent(savedOrder));

        return savedOrder.getId();
    }


    public Order getOrder(UUID id) {
        return ordersRepository.findById(id)
            .map(OrderEntity::mapToOrder)
            .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
    }

    public List<Order> getAccountOrders(UUID accountId, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "updatedAt");
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        return ordersRepository.findByAccountId(accountId, pageRequest).getContent().stream().map(OrderEntity::mapToOrder).toList();
    }

    public void cancelOrder(UUID id) {
        Optional.of(ordersRepository.getReferenceById(id))
            .ifPresent(order -> {
                order.setStatus(Status.CANCELLED);
                ordersRepository.save(order);
            });
    }

    @Transactional
    public void updateFilledOrder(OrdersFilled ordersFilled) {
        log.info("Updating filled orders for {}", ordersFilled);
        UUID orderId = ordersFilled.getOrderId();
        ordersRepository.findById(orderId).ifPresentOrElse(
            order -> {
                BigDecimal quantityFilled = ordersFilled.getFills().stream().map(Fill::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
                log.info("Total quantity filled for orderId={} is {}", orderId, quantityFilled);
                order.setStatus(quantityFilled.compareTo(order.getQuantity()) < 0 ? Status.PARTIALLY_FILLED : Status.FILLED);
                ordersRepository.save(order);
            },
            () -> log.warn("Received OrdersFilled event for non-existent orderId={}", orderId)
        );
    }

    @Transactional
    public void rejectOrder(OrdersRejected ordersRejected) {
        UUID orderId = ordersRejected.getOrderId();
        ordersRepository.findById(orderId).ifPresentOrElse(
            order -> {
                order.setStatus(Status.REJECTED);
                ordersRepository.save(order);
            },
            () -> log.warn("Received OrdersRejected event for non-existent orderId={}", orderId)
        );
    }

    public RiskCheckResponse checkRisk(Order order) {
        // if the blocking stub wasn't provided (tests or misconfiguration), allow by default
        if (riskCheckServiceBlockingStub == null) {
            return RiskCheckResponse.newBuilder().setAllowed(true).setReason("no-rpc-fallback").build();
        }

        RiskCheckRequest riskCheckRequest = RiskCheckRequest.newBuilder()
            .setAccountId(order.getAccountId().toString())
            .setSymbol(order.getSymbol().name())
            .setSide(order.getSide().name())
            .setQuantity(order.getQuantity().toString())
            .build();

        try {
            return riskCheckServiceBlockingStub.checkOrderRisk(riskCheckRequest);
        } catch (StatusRuntimeException e) {
            switch (e.getStatus().getCode()) {
                case UNAVAILABLE, DEADLINE_EXCEEDED:
                    // fallback to allow the order in case of transient gRPC issues
                    log.warn("RiskCheckService unavailable or timed out, allowing order as fallback: {}", e.getStatus());
                    return RiskCheckResponse.newBuilder().setAllowed(false).setReason("grpc-fallback").build();
                default:
                    log.error("gRPC call to RiskCheckService failed with status: {}. Allowing order as fallback.", e.getStatus(), e);
                    return RiskCheckResponse.newBuilder().setAllowed(false).setReason("grpc-error-fallback").build();
            }
        }
    }

    public record OrderSavedEvent(OrderEntity order) {
    }

    @StandardException
    public static class OrderNotFoundException extends RuntimeException {
    }

    @StandardException
    public static class OrderRejectedException extends RuntimeException {
    }
}
