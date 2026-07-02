package org.example.corporate.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByOrderByNameAsc();
    List<Product> findAllByCategorySlugOrderByNameAsc(String slug);
    Optional<Product> findBySlug(String slug);
}
