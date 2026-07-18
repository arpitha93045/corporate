package com.corporate.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import com.corporate.entity.Quote;
import com.corporate.entity.QuoteLine;

public record QuoteDto(
        String token,
        String status,
        long totalCents,
        String notes,
        LocalDate validUntil,
        Instant createdAt,
        String companyName,
        String contactName,
        String email,
        String occasion,
        List<Line> lines
) {
    public record Line(
            String productName,
            long unitPriceCents,
            int quantity,
            long lineTotalCents
    ) {}

    public static QuoteDto from(Quote q) {
        List<Line> lines = q.getLines().stream()
                .map(QuoteDto::lineFrom)
                .toList();
        var e = q.getEnquiry();
        return new QuoteDto(
                q.getToken(),
                q.getStatus().name(),
                q.getTotalCents(),
                q.getNotes(),
                q.getValidUntil(),
                q.getCreatedAt(),
                e.getCompanyName(),
                e.getName(),
                e.getEmail(),
                e.getOccasion(),
                lines
        );
    }

    private static Line lineFrom(QuoteLine l) {
        return new Line(l.getProductName(), l.getUnitPriceCents(), l.getQuantity(), l.getLineTotalCents());
    }
}
