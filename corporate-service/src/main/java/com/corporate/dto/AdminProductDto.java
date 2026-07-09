package com.corporate.dto;

import com.corporate.entity.Product;

/**
 * Admin view of a product — includes stock_quantity and raw in_stock flag
 * that the public ProductDto hides. Prices are in cents.
 */
public record AdminProductDto(
        Long id,
        String name,
        String slug,
        String description,
        long priceCents,
        String imageUrl,
        boolean inStock,
        int stockQuantity,
        String categoryName,
        String categorySlug
) {
    public static AdminProductDto from(Product p) {
        return new AdminProductDto(
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.getDescription(),
                p.getPriceCents(),
                p.getImageUrl(),
                p.isInStock(),
                p.getStockQuantity(),
                p.getCategory().getName(),
                p.getCategory().getSlug()
        );
    }
}
