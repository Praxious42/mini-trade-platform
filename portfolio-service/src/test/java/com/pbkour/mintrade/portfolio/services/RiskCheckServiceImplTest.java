package com.pbkour.mintrade.portfolio.services;

import com.pbkour.mintrade.commons.RiskCheckRequest;
import com.pbkour.mintrade.commons.RiskCheckResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RiskCheckServiceImplTest {
    private RiskService riskService;
    private RiskCheckServiceImpl service;

    @BeforeEach
    void setUp() {
        riskService = mock(RiskService.class);
        service = new RiskCheckServiceImpl(riskService);
    }

    @Test
    void whenValidRequest_thenRespondsAllowed() {
        RiskService.RiskCheckResult result = new RiskService.RiskCheckResult(true, "", new BigDecimal("10"), new BigDecimal("90"));
        when(riskService.riskCheck(any(), any(), any(), any())).thenReturn(result);

        RiskCheckRequest request = RiskCheckRequest.newBuilder()
            .setAccountId(UUID.randomUUID().toString())
            .setSymbol("EURUSD")
            .setQuantity("1000")
            .setSide("BUY")
            .build();

        @SuppressWarnings("unchecked")
        StreamObserver<RiskCheckResponse> observer = mock(StreamObserver.class);

        service.checkOrderRisk(request, observer);

        verify(observer, times(1)).onNext(any(RiskCheckResponse.class));
        verify(observer, times(1)).onCompleted();
        verify(observer, never()).onError(any());
    }

    @Test
    void whenBlankAccount_thenInvalidArgumentStatus() {
        RiskCheckRequest request = RiskCheckRequest.newBuilder()
            .setAccountId("")
            .setSymbol("EURUSD")
            .setQuantity("1000")
            .setSide("BUY")
            .build();

        @SuppressWarnings("unchecked")
        StreamObserver<RiskCheckResponse> observer = mock(StreamObserver.class);

        service.checkOrderRisk(request, observer);

        // capture error
        verify(observer, times(1)).onError(any(Throwable.class));
        // ensure argument is a StatusRuntimeException with INVALID_ARGUMENT
        verify(observer).onError(argThat(t -> {
            if (t instanceof StatusRuntimeException) {
                return ((StatusRuntimeException) t).getStatus().getCode().equals(Status.INVALID_ARGUMENT.getCode());
            }
            return false;
        }));
    }

    @Test
    void whenRiskServiceFails_thenInternalStatus() {
        when(riskService.riskCheck(any(), any(), any(), any())).thenThrow(new RiskCheckFailedException("boom"));

        RiskCheckRequest request = RiskCheckRequest.newBuilder()
            .setAccountId(UUID.randomUUID().toString())
            .setSymbol("EURUSD")
            .setQuantity("1000")
            .setSide("BUY")
            .build();

        @SuppressWarnings("unchecked")
        StreamObserver<RiskCheckResponse> observer = mock(StreamObserver.class);

        service.checkOrderRisk(request, observer);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer).onError(argThat(t -> t instanceof StatusRuntimeException &&
            ((StatusRuntimeException) t).getStatus().getCode().equals(Status.INTERNAL.getCode())));
    }
}

