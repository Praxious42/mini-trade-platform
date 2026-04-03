package com.pbkour.mintrade.portfolio.config;

import com.pbkour.mintrade.portfolio.services.RiskCheckServiceImpl;
import io.grpc.Server;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GrpcServerLifecycleTest {

    private static void setPrivateField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void stopInvokesServerShutdownAndAwaitTermination_whenAwaitReturnsTrue() throws Exception {
        RiskCheckServiceImpl riskCheckService = mock(RiskCheckServiceImpl.class);
        GrpcServerLifecycle lifecycle = new GrpcServerLifecycle(riskCheckService);

        Server server = mock(Server.class);
        when(server.awaitTermination(5, TimeUnit.SECONDS)).thenReturn(true);

        setPrivateField(lifecycle, "server", server);
        setPrivateField(lifecycle, "running", true);

        lifecycle.stop();

        verify(server, times(1)).shutdown();
        verify(server, times(1)).awaitTermination(5, TimeUnit.SECONDS);
        verify(server, never()).shutdownNow();
        assertThat(lifecycle.isRunning()).isFalse();
    }

    @Test
    void stopInvokesShutdownNow_whenAwaitReturnsFalse() throws Exception {
        RiskCheckServiceImpl riskCheckService = mock(RiskCheckServiceImpl.class);
        GrpcServerLifecycle lifecycle = new GrpcServerLifecycle(riskCheckService);

        Server server = mock(Server.class);
        when(server.awaitTermination(5, TimeUnit.SECONDS)).thenReturn(false);

        setPrivateField(lifecycle, "server", server);
        setPrivateField(lifecycle, "running", true);

        lifecycle.stop();

        verify(server, times(1)).shutdown();
        verify(server, times(1)).awaitTermination(5, TimeUnit.SECONDS);
        verify(server, times(1)).shutdownNow();
        assertThat(lifecycle.isRunning()).isFalse();
    }
}

