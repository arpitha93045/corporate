package com.corporate.dto;
import com.corporate.entity.Product;

public record ProductDto(
        Long id,
        String name,
        String slug,
        String description,
        long priceCents,
        String imageUrl,
        boolean inStock,
        String categoryName,
        String categorySlug
) {
    public static ProductDto from(Product p) {
        return new ProductDto(
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.getDescription(),
                p.getPriceCents(),
                p.getImageUrl(),
                p.isInStock() && p.getStockQuantity() > 0,
                p.getCategory().getName(),
                p.getCategory().getSlug()
        );
    }
}
