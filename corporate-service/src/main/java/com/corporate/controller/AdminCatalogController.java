package com.corporate.controller;

import jakarta.validation.Valid;
import com.corporate.dto.CategoryDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.corporate.dto.AdminProductDto;
import com.corporate.dto.CategoryUpsertRequest;
import com.corporate.dto.ProductUpsertRequest;
import com.corporate.service.AdminCatalogService;

@RestController
@RequestMapping("/api/admin")
public class AdminCatalogController {

    private final AdminCatalogService service;

    public AdminCatalogController(AdminCatalogService service) {
        this.service = service;
    }

    @GetMapping("/products")
    public List<AdminProductDto> listProducts() {
        return service.listProducts();
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminProductDto createProduct(@Valid @RequestBody ProductUpsertRequest req) {
        return service.createProduct(req);
    }

    @PutMapping("/products/{id}")
    public AdminProductDto updateProduct(@PathVariable Long id,
                                         @Valid @RequestBody ProductUpsertRequest req) {
        return service.updateProduct(id, req);
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id) {
        service.deleteProduct(id);
    }

    @GetMapping("/categories")
    public List<CategoryDto> listCategories() {
        return service.listCategories();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@Valid @RequestBody CategoryUpsertRequest req) {
        return service.createCategory(req);
    }
}
