package com.corporate.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import com.corporate.entity.Quote;

public interface QuoteRepository extends JpaRepository<Quote, Long> {
    Optional<Quote> findByToken(String token);
    Optional<Quote> findByEnquiryId(Long enquiryId);
}
