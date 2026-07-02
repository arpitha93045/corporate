package org.example.corporate.catalog;

import org.example.corporate.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public List<ProductDto> listProducts(String categorySlug) {
        List<Product> products = (categorySlug == null || categorySlug.isBlank())
                ? productRepo.findAllByOrderByNameAsc()
                : productRepo.findAllByCategorySlugOrderByNameAsc(categorySlug);
        return products.stream().map(ProductDto::from).toList();
    }

    public ProductDto getProduct(String slug) {
        return productRepo.findBySlug(slug)
                .map(ProductDto::from)
                .orElseThrow(() -> new NotFoundException("Product not found: " + slug));
    }
}
