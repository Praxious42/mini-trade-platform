package com.pbkour.mintrade.order.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbkour.mintrade.order.services.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerAdviceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new OrderControllerAdvice())
            .build();
    }

    @Test
    void whenOrderRejected_thenAdviceReturns422WithJsonBody() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/test/reject")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is(422))
            .andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode node = mapper.readTree(content);

        assertThat(node.get("timestamp").asText()).isNotEmpty();
        assertThat(node.get("status").asInt()).isEqualTo(422);
        assertThat(node.get("error").asText()).isEqualTo("Unprocessable Entity");
        assertThat(node.get("message").asText()).contains("risk check failed");

        JsonNode pathNode = node.get("path");
        if (pathNode != null && !pathNode.isNull() && !pathNode.asText().isBlank()) {
            assertThat(pathNode.asText()).contains("/test/reject");
        }
    }

    @RestController
    static class TestController {
        @PostMapping("/test/reject")
        public void reject() {
            throw new OrderService.OrderRejectedException("risk check failed: limit");
        }
    }
}
