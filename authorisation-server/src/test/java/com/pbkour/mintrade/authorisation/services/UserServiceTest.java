package com.pbkour.mintrade.authorisation.services;

import com.pbkour.mintrade.authorisation.entities.User;
import com.pbkour.mintrade.authorisation.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void loadUserByUsername_found() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername("bob");
        u.setPassword("secretpass");
        u.setRole("ADMIN");
        u.setCreatedAt(Instant.now());

        when(userRepository.findByUsername("bob")).thenReturn(u);

        UserDetails details = userService.loadUserByUsername("bob");
        assertThat(details.getUsername()).isEqualTo("bob");
        assertThat(details.getPassword()).isEqualTo("{noop}secretpass");
        assertThat(details.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Test
    void loadUserByUsername_notFound() {
        when(userRepository.findByUsername("nobody")).thenReturn(null);

        assertThatThrownBy(() -> userService.loadUserByUsername("nobody")).isInstanceOf(UsernameNotFoundException.class);
    }
}

