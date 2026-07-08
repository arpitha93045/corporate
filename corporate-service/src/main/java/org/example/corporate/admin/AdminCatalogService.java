package org.example.corporate.admin;

import org.example.corporate.catalog.Category;
import org.example.corporate.catalog.CategoryRepository;
import org.example.corporate.catalog.CategoryDto;
import org.example.corporate.catalog.Product;
import org.example.corporate.catalog.ProductRepository;
import org.example.corporate.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminCatalogService {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;

    public AdminCatalogService(ProductRepository productRepo, CategoryRepository categoryRepo) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
    }

    @Transactional(readOnly = true)
    public List<AdminProductDto> listProducts() {
        return productRepo.findAllByOrderByNameAsc().stream()
                .map(AdminProductDto::from)
                .toList();
    }

    @Transactional
    public AdminProductDto createProduct(ProductUpsertRequest req) {
        Category category = categoryRepo.findBySlug(req.categorySlug())
                .orElseThrow(() -> new NotFoundException("Category not found: " + req.categorySlug()));
        Product p = new Product();
        applyRequest(p, req, category);
        return AdminProductDto.from(productRepo.save(p));
    }

    @Transactional
    public AdminProductDto updateProduct(Long id, ProductUpsertRequest req) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
        Category category = categoryRepo.findBySlug(req.categorySlug())
                .orElseThrow(() -> new NotFoundException("Category not found: " + req.categorySlug()));
        applyRequest(p, req, category);
        return AdminProductDto.from(p);
    }

    @Transactional
    public void deleteProduct(Long id) {
        // Historical orders snapshot product_name/unit_price on their line items,
        // so deleting a product doesn't invalidate them. JPA will null the FK on
        // order_item.product_id via ON DELETE SET NULL if the DB enforces it —
        // otherwise the DB rejects and we surface that as a 409 later. For now
        // rely on the schema; if line items block deletion we'll add a soft-delete
        // flag instead of touching order history.
        if (!productRepo.existsById(id)) {
            throw new NotFoundException("Product not found: " + id);
        }
        productRepo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> listCategories() {
        return categoryRepo.findAllByOrderByNameAsc().stream()
                .map(c -> new CategoryDto(c.getId(), c.getName(), c.getSlug()))
                .toList();
    }

    @Transactional
    public CategoryDto createCategory(CategoryUpsertRequest req) {
        Category c = new Category();
        c.setName(req.name());
        c.setSlug(req.slug());
        Category saved = categoryRepo.save(c);
        return new CategoryDto(saved.getId(), saved.getName(), saved.getSlug());
    }

    private void applyRequest(Product p, ProductUpsertRequest req, Category category) {
        p.setCategory(category);
        p.setName(req.name());
        p.setSlug(req.slug());
        p.setDescription(req.description());
        p.setPriceCents(req.priceCents());
        p.setImageUrl(req.imageUrl());
        p.setStockQuantity(req.stockQuantity());
        // Public catalog filters on stockQuantity > 0 as well, but keep the flag
        // in sync so admins that set stock=0 also hide the product on the site.
        p.setInStock(req.stockQuantity() > 0);
    }
}
