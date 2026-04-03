package com.pbkour.mintrade.order.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class MdcFilterTest {
    private final MdcFilter filter = new MdcFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void whenRequestHasTraceHeader_thenUseProvidedTraceIdAndEchoToResponse() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String provided = "trace-123";
        request.addHeader("X-Trace-Id", provided);

        FilterChain chain = (req, res) -> {
            assertThat(MDC.get("traceId")).isEqualTo(provided);
            assertThat(MDC.get("SERVICE_ID")).isEqualTo("order-service");
            assertThat(response.getHeader("X-Trace-Id")).isEqualTo(provided);
        };

        filter.doFilter(request, response, chain);

        assertThat(MDC.get("traceId")).isNull();
        MDC.remove("SERVICE_ID");
    }

    @Test
    void whenNoTraceHeader_thenGenerateTraceIdAndEchoToResponse() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            String mdcTrace = MDC.get("traceId");
            assertThat(mdcTrace).isNotNull().isNotEmpty();

            String header = response.getHeader("X-Trace-Id");

            assertThat(header).isNotNull().isNotEmpty();
            assertThat(header).isEqualTo(mdcTrace);
            assertThat(MDC.get("SERVICE_ID")).isEqualTo("order-service");
        };

        filter.doFilter(request, response, chain);

        assertThat(MDC.get("traceId")).isNull();
        MDC.remove("SERVICE_ID");
    }
}

