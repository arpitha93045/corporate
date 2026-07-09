package com.corporate.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.corporate.dto.CategoryDto;
import com.corporate.dto.ProductDto;
import com.corporate.service.CatalogService;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/categories")
    public List<CategoryDto> categories() {
        return catalog.listCategories();
    }

    @GetMapping("/products")
    public List<ProductDto> products(@RequestParam(name = "category", required = false) String categorySlug) {
        return catalog.listProducts(categorySlug);
    }

    @GetMapping("/products/{slug}")
    public ProductDto product(@PathVariable String slug) {
        return catalog.getProduct(slug);
    }
}
