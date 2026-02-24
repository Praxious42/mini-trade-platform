package com.pbkour.mintrade.order.controllers;

import com.pbkour.mintrade.commons.RiskCheckResponse;
import com.pbkour.mintrade.commons.RiskCheckServiceGrpc;
import com.pbkour.mintrade.commons.dto.Order;
import com.pbkour.mintrade.commons.orders.Side;
import com.pbkour.mintrade.commons.orders.Symbol;
import com.pbkour.mintrade.commons.orders.Type;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(properties = {"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"})
class OrderContainerIntegrationTest {
    private static final File INFRA_COMPOSE = new File("../infra/docker/docker-compose.yml");
    private static final File COMPOSE_FILE = prepareComposeFile();

    @Container
    public static ComposeContainer compose = new ComposeContainer(COMPOSE_FILE)
        .withExposedService("broker", 9092, Wait.forLogMessage(".*Kafka Server started.*\\n", 1))
        .withExposedService("mintrade-order", 5432, Wait.forLogMessage(".*database system is ready to accept connections.*\\n", 1));

    @MockitoSpyBean
    private OrderController orderController;

    @MockitoBean
    private RiskCheckServiceGrpc.RiskCheckServiceBlockingStub riskCheckServiceBlockingStub;

    // container_name in docker-compose is causing issues with Testcontainers
    private static File prepareComposeFile() {
        try {
            String content = Files.readString(INFRA_COMPOSE.toPath());
            String cleaned = content.replaceAll("(?m)^\\s*container_name\\s*:\\s*.*\\r?\\n", "");

            File tmp = Files.createTempFile("docker-compose-cleaned-", ".yml").toFile();
            Files.writeString(tmp.toPath(), cleaned);
            tmp.deleteOnExit();
            return tmp;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void happy_path_end_to_end() {
        Order order = Order.builder()
            .accountId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"))
            .symbol(Symbol.AAPL)
            .side(Side.BUY)
            .type(Type.LIMIT)
            .quantity(new BigDecimal("0.1"))
            .limitPrice(new BigDecimal("30000"))
            .build();


        when(riskCheckServiceBlockingStub.checkOrderRisk(any())).thenReturn(RiskCheckResponse.newBuilder().setAllowed(true).build());

        ResponseEntity<String> orderResponse = orderController.createOrder(order);

        ResponseEntity<List<Order>> listResponseEntity = orderController.listOrdersByAccount(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"), 0, 1);
        assert listResponseEntity.getBody() != null;
        Order order1 = listResponseEntity.getBody().stream().findFirst().orElseThrow(IllegalStateException::new);

        assertEquals(order.getAccountId(), order1.getAccountId());
        assertEquals(order.getSymbol(), order1.getSymbol());
        assertEquals(order.getSide(), order1.getSide());
        assertEquals(order.getType(), order1.getType());
        assertEquals(0, order1.getQuantity().compareTo(order.getQuantity()));
        assertEquals(0, order.getLimitPrice().compareTo(order1.getLimitPrice()));

        // orderResponse.getBody() ends with UUID I want that part of the string
        String substring = orderResponse.getBody().substring(orderResponse.getBody().lastIndexOf(" ") + 1);
        UUID uuid = UUID.fromString(substring);

        ResponseEntity<Order> order2 = orderController.getOrder(uuid);

        assert order2.getBody() != null;
        assertEquals(order.getAccountId(), order2.getBody().getAccountId());
        assertEquals(order.getSymbol(), order2.getBody().getSymbol());
        assertEquals(order.getSide(), order2.getBody().getSide());
        assertEquals(order.getType(), order2.getBody().getType());
        assertEquals(0, order2.getBody().getQuantity().compareTo(order.getQuantity()));
        assertEquals(0, order.getLimitPrice().compareTo(order2.getBody().getLimitPrice()));

        ResponseEntity<String> cancelResponse = orderController.cancelOrder(uuid);
        assertEquals(200, cancelResponse.getStatusCode().value());
    }
}
