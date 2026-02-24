package com.pbkour.mintrade.order.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.commons.RiskCheckResponse;
import com.pbkour.mintrade.commons.RiskCheckServiceGrpc;
import com.pbkour.mintrade.commons.json.ObjectMapperFactory;
import com.pbkour.mintrade.order.entities.OrderEntity;
import com.pbkour.mintrade.order.repositories.OrdersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest("spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration")
class OrderControllerIntegrationTest {

    private final ObjectMapper mapper = ObjectMapperFactory.create();

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private OrdersRepository ordersRepository;

    // provide a mocked gRPC stub to satisfy OrderService dependency during integration test
    @MockitoBean
    private RiskCheckServiceGrpc.RiskCheckServiceBlockingStub riskCheckServiceBlockingStub;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        ordersRepository.deleteAll();

        // stub risk check to allow orders
        when(riskCheckServiceBlockingStub.checkOrderRisk(any())).thenReturn(RiskCheckResponse.newBuilder().setAllowed(true).build());
    }

    @Test
    void createThenGetOrder_returnsSameOrder() throws Exception {
        String json = new String(Objects.requireNonNull(getClass().getResourceAsStream("/order-request.json")).readAllBytes(), StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk());

        JsonNode node = mapper.readTree(json);
        UUID accountId = UUID.fromString(node.get("accountId").asText());

        // use a real PageRequest to query the repository
        List<OrderEntity> entities = ordersRepository.findByAccountId(accountId, PageRequest.of(0, 10)).getContent();
        assertThat(entities).isNotNull().hasSizeGreaterThanOrEqualTo(1);

        OrderEntity created = entities.stream()
            .filter(e -> e.getSymbol().name().equals(node.get("symbol").asText()) && node.get("quantity").decimalValue().compareTo(e.getQuantity()) == 0)
            .findFirst()
            .orElseThrow();

        UUID id = created.getId();

        MvcResult getResult = mockMvc.perform(get("/api/v1/orders/" + id))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode fetched = mapper.readTree(getResult.getResponse().getContentAsString());
        assertThat(fetched).isNotNull();
        assertThat(UUID.fromString(fetched.get("accountId").asText())).isEqualTo(accountId);
        assertThat(fetched.get("symbol").asText()).isEqualTo(node.get("symbol").asText());
        assertThat(fetched.get("quantity").asLong()).isEqualTo(node.get("quantity").asLong());
    }

//    @TestConfiguration
//    static class TestConfig {
//        @Bean
//        @Primary
//        public RiskCheckServiceGrpc.RiskCheckServiceBlockingStub testRiskCheckServiceBlockingStub() {
//            return Mockito.mock(RiskCheckServiceGrpc.RiskCheckServiceBlockingStub.class);
//        }
//    }
}
