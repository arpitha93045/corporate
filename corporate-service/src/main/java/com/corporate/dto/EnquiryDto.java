package com.corporate.dto;

import java.time.Instant;
import java.time.LocalDate;
import com.corporate.entity.Enquiry;

public record EnquiryDto(
        Long id,
        String name,
        String email,
        String companyName,
        String phone,
        String message,
        Integer estimatedQuantity,
        String occasion,
        LocalDate eventDate,
        String budgetRange,
        String status,
        Instant createdAt
) {
    public static EnquiryDto from(Enquiry e) {
        return new EnquiryDto(
                e.getId(), e.getName(), e.getEmail(), e.getCompanyName(),
                e.getPhone(), e.getMessage(), e.getEstimatedQuantity(),
                e.getOccasion(), e.getEventDate(), e.getBudgetRange(),
                e.getStatus().name(), e.getCreatedAt()
        );
    }
}
