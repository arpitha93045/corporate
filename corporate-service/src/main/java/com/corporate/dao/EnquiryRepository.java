package com.corporate.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import com.corporate.entity.Enquiry;

public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {
    List<Enquiry> findAllByOrderByCreatedAtDesc();
}
