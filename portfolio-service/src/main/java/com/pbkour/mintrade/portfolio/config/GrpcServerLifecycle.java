package com.pbkour.mintrade.portfolio.config;

import com.pbkour.mintrade.portfolio.services.RiskCheckServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.experimental.StandardException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class GrpcServerLifecycle implements SmartLifecycle {

    private final RiskCheckServiceImpl riskCheckService;
    private Server server;
    private volatile boolean running = false;

    @Value("${portfolio.grpc.port:8085}")
    private int port;

    public GrpcServerLifecycle(RiskCheckServiceImpl riskCheckService) {
        this.riskCheckService = riskCheckService;
    }

    @Override
    public void start() {
        try {
            server = ServerBuilder.forPort(port)
                .addService(riskCheckService)
                .build()
                .start();
            running = true;
            log.info("gRPC server started on port: {}", port);
        } catch (IOException e) {
            throw new GrpcServerException("Failed to start gRPC server", e);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.shutdown();
            try {
                if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                    server.shutdownNow();
                }
            } catch (InterruptedException e) {
                server.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return 0;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @StandardException
    public static class GrpcServerException extends RuntimeException {
    }
}

