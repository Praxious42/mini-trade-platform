package com.pbkour.mintrade.order.config;

import com.pbkour.mintrade.commons.RiskCheckServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {

    private static final Logger log = LoggerFactory.getLogger(GrpcClientConfig.class);

    @Value("${portfolio.grpc.host}")
    private String host;

    @Value("${portfolio.grpc.port}")
    private int port;

    @Bean(destroyMethod = "shutdown")
    public ManagedChannel portfolioChannel() {
        log.info("Creating gRPC channel to portfolio service target={}", host + ":" + port);
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build();
        return channel;
    }

    @Bean
    public RiskCheckServiceGrpc.RiskCheckServiceBlockingStub riskCheckServiceBlockingStub(ManagedChannel channel) {
        return RiskCheckServiceGrpc.newBlockingStub(channel);
    }
}
