package org.example.corporate.catalog;

public record CategoryDto(Long id, String name, String slug) {
    public static CategoryDto from(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getSlug());
    }
}
