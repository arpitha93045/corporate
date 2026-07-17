package com.corporate.dao;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import com.corporate.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByOrderByNameAsc();
    List<Product> findAllByCategorySlugOrderByNameAsc(String slug);
    Optional<Product> findBySlug(String slug);

    // Case-insensitive substring search over name + description, with an optional
    // category filter. LOWER(...) LIKE stays portable across Postgres (prod) and
    // H2 (local/test) — no to_tsvector, which H2 lacks. Callers pass a
    // pre-lowercased needle already wrapped in %...%; user-supplied % _ ! are
    // escaped with ! so they can't act as wildcards.
    @Query("""
            SELECT p FROM Product p
            WHERE (:categorySlug IS NULL OR p.category.slug = :categorySlug)
              AND (LOWER(p.name) LIKE :needle ESCAPE '!' OR LOWER(p.description) LIKE :needle ESCAPE '!')
            ORDER BY p.name ASC
            """)
    List<Product> search(String needle, String categorySlug);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(Long id);
}
