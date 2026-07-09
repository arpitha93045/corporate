package com.corporate.dto;

import java.util.List;

/**
 * Slim, model-friendly view of a product. The agent uses this to decide which
 * items to suggest; the frontend never sees these directly — it fetches full
 * ProductDto via the existing catalog API. Prices in paise, matching the rest
 * of the app.
 */
public record AgentProductRef(
        long id,
        String slug,
        String name,
        String categorySlug,
        long priceCents,
        int stockQuantity,
        boolean inStock,
        String description,
        List<String> tags
) {}
