package com.corporate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * Admin request to issue a quote for an enquiry. Deliberately carries no prices —
 * the server prices every line from the live catalog, mirroring checkout.
 */
public record CreateQuoteRequest(
        @NotEmpty @Valid List<Line> lines,
        @Size(max = 5000) String notes,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validUntil
) {
    public record Line(
            @NotNull Long productId,
            @Min(1) int quantity
    ) {}
}
