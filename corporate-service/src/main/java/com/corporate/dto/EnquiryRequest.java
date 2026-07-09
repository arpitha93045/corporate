package com.corporate.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record EnquiryRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Email @Size(max = 200) String email,
        @Size(max = 200) String companyName,
        @Size(max = 60) String phone,
        @NotBlank @Size(max = 5000) String message,
        @Min(1) Integer estimatedQuantity,
        @Size(max = 200) String occasion,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
        @Size(max = 80) String budgetRange
) {}
