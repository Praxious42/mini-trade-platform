package com.pbkour.mintrade.authorisation.services;

import com.pbkour.mintrade.authorisation.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullUnmarked;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    @NullUnmarked
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        var userEntity = ofNullable(userRepository.findByUsername(username)).orElseThrow(() -> new UsernameNotFoundException(username));
        return User.withUsername(userEntity.getUsername())
            .password("{noop}" + userEntity.getPassword())
            .roles(userEntity.getRole())
            .build();
    }
}
