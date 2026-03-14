package com.pbkour.mintrade.authorisation.controllers;

import com.pbkour.mintrade.authorisation.services.JwtService;
import com.pbkour.mintrade.authorisation.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private AuthenticationManager authenticationManager;
    private UserService userService;
    private JwtService jwtService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authenticationManager = Mockito.mock(AuthenticationManager.class);
        userService = Mockito.mock(UserService.class);
        jwtService = Mockito.mock(JwtService.class);

        AuthController controller = new AuthController(authenticationManager, userService, jwtService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void login_success_returnsToken() throws Exception {
        String username = "alice";
        String password = "password";
        String token = "token-123";

        UserDetails userDetails = User.withUsername(username).password("{noop}" + "password").roles("USER").build();

        when(userService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtService.generateToken(username)).thenReturn(token);

        mockMvc.perform(post("/auth/login").param("username", username).param("password", password))
            .andExpect(status().isOk())
            .andExpect(content().string(token));

        verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken(username, password));
        verify(userService).loadUserByUsername(username);
        verify(jwtService).generateToken(username);
    }

    @Test
    void login_withBadCredentials_propagates() throws Exception {
        String username = "alice";
        String password = "wrong";

        doThrow(new BadCredentialsException("bad creds")).when(authenticationManager).authenticate(any());

        mockMvc.perform(post("/auth/login").param("username", username).param("password", password))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void secureEndpoint_returnsAccessGranted() throws Exception {
        mockMvc.perform(get("/auth/secure"))
            .andExpect(status().isOk())
            .andExpect(content().string("Access granted"));
    }
}

