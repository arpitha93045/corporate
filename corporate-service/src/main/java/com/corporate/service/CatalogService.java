package com.corporate.service;

import com.corporate.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.corporate.dao.CategoryRepository;
import com.corporate.dao.ProductRepository;
import com.corporate.dto.CategoryDto;
import com.corporate.dto.ProductDto;
import com.corporate.entity.Product;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final CategoryRepository categoryRepo;
    private final ProductRepository productRepo;

    public CatalogService(CategoryRepository categoryRepo, ProductRepository productRepo) {
        this.categoryRepo = categoryRepo;
        this.productRepo = productRepo;
    }

    public List<CategoryDto> listCategories() {
        return categoryRepo.findAllByOrderByNameAsc().stream()
                .map(CategoryDto::from)
                .toList();
    }

    public List<ProductDto> listProducts(String categorySlug, String query) {
        String slug = (categorySlug == null || categorySlug.isBlank()) ? null : categorySlug;
        String q = query == null ? "" : query.trim();

        List<Product> products;
        if (q.isEmpty()) {
            products = (slug == null)
                    ? productRepo.findAllByOrderByNameAsc()
                    : productRepo.findAllByCategorySlugOrderByNameAsc(slug);
        } else {
            String needle = "%" + q.toLowerCase(java.util.Locale.ROOT)
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            products = productRepo.search(needle, slug);
        }
        return products.stream().map(ProductDto::from).toList();
    }

    public ProductDto getProduct(String slug) {
        return productRepo.findBySlug(slug)
                .map(ProductDto::from)
                .orElseThrow(() -> new NotFoundException("Product not found: " + slug));
    }
}
