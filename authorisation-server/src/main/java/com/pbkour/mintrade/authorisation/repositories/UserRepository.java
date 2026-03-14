package com.pbkour.mintrade.authorisation.repositories;

import com.pbkour.mintrade.authorisation.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    User findByUsername(String username);
}
