package com.pbkour.mintrade.order.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.contracts.db.OrderEntity;
import com.pbkour.mintrade.contracts.json.ObjectMapperFactory;
import com.pbkour.mintrade.order.repositories.OrdersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class OrderControllerIntegrationTest {

    private final ObjectMapper mapper = ObjectMapperFactory.objectMapper();

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private OrdersRepository ordersRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        ordersRepository.deleteAll();
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

        List<OrderEntity> entities = ordersRepository.findByAccountId(accountId, null).getContent();
        assertThat(entities).isNotNull().hasSizeGreaterThanOrEqualTo(1);

        OrderEntity created = entities.stream()
            .filter(e -> e.getSymbol().name().equals(node.get("symbol").asText()) && node.get("quantity").asLong() == e.getQuantity())
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
}

