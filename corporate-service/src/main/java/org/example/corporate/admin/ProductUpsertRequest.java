package org.example.corporate.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Admin write payload for products. Categories are addressed by slug —
 * the slug is stable and human-readable, avoiding brittle id-passing
 * from the UI.
 */
public record ProductUpsertRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 200) String slug,
        @NotBlank String description,
        @Min(0) long priceCents,
        String imageUrl,
        @Min(0) int stockQuantity,
        @NotBlank String categorySlug
) {}
