package com.pbkour.mintrade.authorisation.repositories;

import com.pbkour.mintrade.authorisation.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}
