package com.corporate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * A bulk order pasted/uploaded as CSV rows of {productSlug, quantity}. The
 * server re-prices every line against the catalog (see DraftCartService /
 * AgentTools.estimateTotal) — nothing here is trusted for money or stock.
 * The batch is capped so a large paste can't hammer the pricing query.
 */
public record BulkOrderRequest(
        @NotEmpty @Size(max = MAX_LINES) @Valid List<Line> lines
) {
    public static final int MAX_LINES = 200;

    public record Line(
            @NotBlank String productSlug,
            @Min(1) int quantity
    ) {}
}
