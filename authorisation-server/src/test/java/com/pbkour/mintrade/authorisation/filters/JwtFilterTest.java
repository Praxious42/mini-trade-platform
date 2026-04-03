package com.pbkour.mintrade.authorisation.filters;

import com.pbkour.mintrade.authorisation.services.JwtService;
import com.pbkour.mintrade.authorisation.services.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void whenAuthorizationHeaderHasValidBearerToken_thenAuthenticationSet() throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        JwtService jwtService = mock(JwtService.class);
        UserService userService = mock(UserService.class);

        String token = "valid-token";
        String username = "alice";

        when(jwtService.extractUsername(token)).thenReturn(username);
        when(jwtService.validateToken(token, username)).thenReturn(true);

        UserDetails userDetails = User.withUsername(username)
            .password("secret")
            .authorities(new SimpleGrantedAuthority("ROLE_USER"))
            .build();
        when(userService.loadUserByUsername(username)).thenReturn(userDetails);

        JwtFilter filter = new JwtFilter(jwtService, userService);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse resp = new MockHttpServletResponse();

        FilterChain chain = (request, response) -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getName()).isEqualTo(username);
            assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
        };

        filter.doFilter(req, resp, chain);

        // cleanup
        SecurityContextHolder.clearContext();
        verify(jwtService, times(1)).extractUsername(token);
        verify(jwtService, times(1)).validateToken(token, username);
        verify(userService, times(1)).loadUserByUsername(username);
    }

    @Test
    void whenAuthorizationHeaderMissing_thenNoAuthenticationSet() throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        JwtService jwtService = mock(JwtService.class);
        UserService userService = mock(UserService.class);

        JwtFilter filter = new JwtFilter(jwtService, userService);

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        FilterChain chain = (request, response) -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNull();
        };

        filter.doFilter(req, resp, chain);

        verifyNoInteractions(jwtService);
        verifyNoInteractions(userService);
    }

    @Test
    void whenTokenInvalid_thenNoAuthenticationSet() throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        JwtService jwtService = mock(JwtService.class);
        UserService userService = mock(UserService.class);

        String token = "bad-token";
        String username = "bob";

        when(jwtService.extractUsername(token)).thenReturn(username);
        when(jwtService.validateToken(token, username)).thenReturn(false);

        UserDetails userDetails = User.withUsername(username)
            .password("secret")
            .authorities(new SimpleGrantedAuthority("ROLE_USER"))
            .build();
        when(userService.loadUserByUsername(username)).thenReturn(userDetails);

        JwtFilter filter = new JwtFilter(jwtService, userService);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse resp = new MockHttpServletResponse();

        FilterChain chain = (request, response) -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNull();
        };

        filter.doFilter(req, resp, chain);

        verify(jwtService, times(1)).extractUsername(token);
        verify(jwtService, times(1)).validateToken(token, username);
        verify(userService, times(1)).loadUserByUsername(username);
    }

    @Test
    void whenJwtServiceThrows_thenExceptionIsHandledAndChainContinues() throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        JwtService jwtService = mock(JwtService.class);
        UserService userService = mock(UserService.class);

        String token = "throws-token";
        when(jwtService.extractUsername(token)).thenThrow(new RuntimeException("parse error"));

        JwtFilter filter = new JwtFilter(jwtService, userService);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse resp = new MockHttpServletResponse();

        FilterChain chain = (request, response) -> {
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        };

        filter.doFilter(req, resp, chain);

        verify(jwtService, times(1)).extractUsername(token);
        verifyNoInteractions(userService);
    }
}



