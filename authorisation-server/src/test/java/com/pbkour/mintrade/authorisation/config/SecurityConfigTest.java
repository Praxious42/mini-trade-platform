package com.pbkour.mintrade.authorisation.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    @Test
    void authenticationManagerDelegatesToAuthenticationConfiguration() {
        var jwtFilter = mock(com.pbkour.mintrade.authorisation.filters.JwtFilter.class);
        SecurityConfig cfg = new SecurityConfig(jwtFilter);

        AuthenticationManager expected = mock(AuthenticationManager.class);
        AuthenticationConfiguration ac = mock(AuthenticationConfiguration.class);
        when(ac.getAuthenticationManager()).thenReturn(expected);

        AuthenticationManager actual = cfg.authenticationManager(ac);
        assertThat(actual).isSameAs(expected);
    }
}

