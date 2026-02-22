package com.pbkour.mintrade.portfolio.services;

import com.pbkour.mintrade.commons.RiskCheckRequest;
import com.pbkour.mintrade.commons.RiskCheckResponse;
import com.pbkour.mintrade.commons.RiskCheckServiceGrpc;
import com.pbkour.mintrade.commons.orders.Side;
import com.pbkour.mintrade.commons.orders.Symbol;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.experimental.StandardException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

import static java.util.Optional.of;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskCheckServiceImpl extends RiskCheckServiceGrpc.RiskCheckServiceImplBase {
    private final RiskService riskService;

    @Override
    public void checkOrderRisk(
        RiskCheckRequest request, StreamObserver<RiskCheckResponse> responseObserver) {
        log.info("checkOrderRisk start");
        try {
            UUID accountId = of(request.getAccountId())
                .filter(s -> !s.isBlank())
                .map(UUID::fromString)
                .orElseThrow(() -> new RiskCheckServiceException("Account ID is blank"));

            Symbol symbol = of(request.getSymbol())
                .filter(s -> !s.isBlank())
                .map(Symbol::valueOf)
                .orElseThrow(() -> new RiskCheckServiceException("Symbol is blank"));

            BigDecimal quantity = of(request.getQuantity())
                .filter(s -> !s.isBlank())
                .map(BigDecimal::new)
                .orElseThrow(() -> new RiskCheckServiceException("Quantity is blank"));

            Side side = of(request.getSide())
                .filter(s -> !s.isBlank())
                .map(Side::valueOf)
                .orElseThrow(() -> new RiskCheckServiceException("Side is blank"));

            RiskService.RiskCheckResult riskCheckResult = riskService.riskCheck(accountId, symbol, quantity, side);


            RiskCheckResponse response = RiskCheckResponse.newBuilder()
                .setAllowed(riskCheckResult.allowed())
                .setReason(riskCheckResult.reason())
                .setRequiredMargin(riskCheckResult.requiredMargin().toString())
                .setAvailableMargin(riskCheckResult.availableMargin().toString())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage());
            responseObserver.onError(new RiskCheckServiceException(e.getMessage()));
        } catch (RiskCheckServiceException e) {
            log.error("Risk check failed for accountId={}: {}", request.getAccountId(), e.getMessage());
            responseObserver.onError(new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription(e.getMessage())));
        } catch (RiskCheckFailedException e) {
            log.error("Risk check failed internally for accountId={}: {}", request.getAccountId(), e.getMessage());
            responseObserver.onError(new StatusRuntimeException(Status.INTERNAL.withDescription(e.getMessage())));
        } catch (Exception e) {
            log.error("Unexpected error during risk check for accountId={}: {}", request.getAccountId(), e.getMessage());
            responseObserver.onError(new StatusRuntimeException(Status.INTERNAL.withDescription("Unexpected error: " + e.getMessage())));
        }

    }

    @StandardException
    public static class RiskCheckServiceException extends RuntimeException {
    }
}
