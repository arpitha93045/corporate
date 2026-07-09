package com.corporate.dto;

import java.util.List;

/**
 * Result of estimate_total. Server-priced — the model does not do arithmetic
 * on money. Per-line totals help the model explain its choices back to the
 * user; the outer total is what the frontend would show at checkout.
 */
public record AgentCartTotal(
        List<Line> lines,
        long totalCents,
        List<String> warnings
) {
    public record Line(
            String productSlug,
            String productName,
            int quantity,
            long unitPriceCents,
            long lineTotalCents,
            boolean inStock,
            int stockAvailable
    ) {}
}
