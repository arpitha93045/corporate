package com.corporate.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import com.corporate.entity.DraftCart;

public interface DraftCartRepository extends JpaRepository<DraftCart, Long> {
    Optional<DraftCart> findByToken(String token);
}
