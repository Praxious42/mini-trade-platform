package com.pbkour.mintrade.order.config;

import com.pbkour.mintrade.commons.RiskCheckServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {

    @Value("${portfolio.grpc.host:localhost}")
    private String host;

    @Value("${portfolio.grpc.port:8085}")
    private int port;

    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel portfolioChannel() {
        return ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext() // change to TLS config if you ever enable TLS
            .build();
    }

    @Bean
    public RiskCheckServiceGrpc.RiskCheckServiceBlockingStub riskCheckBlockingStub(ManagedChannel portfolioChannel) {
        return RiskCheckServiceGrpc.newBlockingStub(portfolioChannel);
    }
}

