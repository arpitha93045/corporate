package com.corporate.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import com.corporate.entity.AppUser;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);
}
