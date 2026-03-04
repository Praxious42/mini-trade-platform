package bdd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.bdd.clients.ClientFactory;
import com.pbkour.mintrade.bdd.clients.OrderClient;
import com.pbkour.mintrade.commons.dto.Order;
import com.pbkour.mintrade.commons.json.ObjectMapperFactory;
import com.pbkour.mintrade.commons.orders.Side;
import com.pbkour.mintrade.commons.orders.Status;
import com.pbkour.mintrade.commons.orders.Symbol;
import com.pbkour.mintrade.commons.orders.Type;
import com.pbkour.mintrade.commons.responses.OrderResponse;
import feign.Response;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.experimental.StandardException;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.core.ConditionTimeoutException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.pbkour.mintrade.commons.orders.Status.NEW;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
public class StepDefinitions {
    public final OrderClient orderClient = new ClientFactory().createOrderClient();
    ObjectMapper objectMapper = ObjectMapperFactory.create();
    HashMap<String, Object> context = new HashMap<>();

    @When("a market order is submitted with quantity {int} and return status code {int}")
    public void aMarketOrderIsSubmitted(int quantity, int code) {
        final Order order = Order.builder()
            .accountId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"))
            .symbol(Symbol.AAPL)
            .side(Side.BUY)
            .type(Type.MARKET)
            .quantity(BigDecimal.valueOf(quantity))
            .build();
        try (Response createOrderResponse = orderClient.createOrder(order)) {

            assertEquals(code, createOrderResponse.status());

            try {
                String body = new String(createOrderResponse.body().asInputStream().readAllBytes());
                OrderResponse orderResponse = objectMapper.readValue(body, OrderResponse.class);
                assertEquals(order.getAccountId(), orderResponse.getAccountId());
                assertEquals(order.getSymbol(), orderResponse.getSymbol());
                assertEquals(order.getSide(), orderResponse.getSide());
                assertEquals(order.getType(), orderResponse.getType());
                assertEquals(NEW, orderResponse.getStatus());
                assertEquals(0, order.getQuantity().compareTo(orderResponse.getQuantity()));

                context.put("createdOrderId", orderResponse.getOrderId());
            } catch (Exception e) {
                fail("Failed to read order creation response body", e);
            }
        }
    }

    @Then("the order is filled")
    public void theOrderIsAccepted() {
        UUID id = (UUID) context.get("createdOrderId");
        // use the extracted helper to wait for the FILLED status
        waitForOrderStatus(id, Status.FILLED, 5, 5000, 200);
    }


    @And("the order fails due to insufficient margin")
    public void theOrderFailsDueToInsufficientMargin() {
        Map<String, Object> errorResponse = (Map<String, Object>) context.get("errorResponse");
        String message = errorResponse.get("message").toString();
        assertEquals("Order rejected by risk check: REQUIRED_MARGIN", message);
    }

    private void waitForOrderStatus(UUID orderId, Status expectedStatus, int maxAttempts, long backoffMillis, int statusCode) {
        long timeoutMillis = maxAttempts * backoffMillis;
        try {
            await()
                .pollInterval(Duration.ofMillis(backoffMillis))
                .atMost(Duration.ofMillis(timeoutMillis))
                .until(() -> {
                    try (Response getOrderResponse = orderClient.getOrder(orderId)) {
                        if (getOrderResponse.status() != statusCode) {
                            log.info("Order endpoint returned status {} (expected {})", getOrderResponse.status(), statusCode);
                            return false;
                        }

                        OrderResponse orderResponse = objectMapper.readValue(getOrderResponse.body().asInputStream(), OrderResponse.class);
                        Status status = orderResponse.getStatus();
                        boolean matches = status.name().equals(expectedStatus.name());
                        if (!matches) {
                            log.info("Order status is {}, waiting for {}", status.name(), expectedStatus.name());
                        }
                        return matches;
                    } catch (Exception e) {
                        log.warn("Failed to read order response body or status not yet '{}': attempt will retry", expectedStatus.name(), e);
                        return false;
                    }
                });
        } catch (ConditionTimeoutException e) {
            fail("Failed to get order status " + expectedStatus.name() + " after multiple attempts");
        }
    }

    @When("a market order is submitted with quantity {int} and errors with status code {int}")
    public void aMarketOrderIsSubmittedWithQuantityAndErrorsWithStatusCode(int quantity, int code) {
        final Order order = Order.builder()
            .accountId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"))
            .symbol(Symbol.AAPL)
            .side(Side.BUY)
            .type(Type.MARKET)
            .quantity(BigDecimal.valueOf(quantity))
            .build();
        try (Response createOrderResponse = orderClient.createOrder(order)) {
            assertEquals(code, createOrderResponse.status());

            String body = new String(createOrderResponse.body().asInputStream().readAllBytes());
            Map<String, Object> errorResponse = objectMapper.readValue(body, Map.class);
            context.put("errorResponse", errorResponse);

        } catch (Exception e) {
            fail("Failed to create order", e);
        }
    }

    @StandardException
    public static class StepDefinitionException extends RuntimeException {
    }
}
