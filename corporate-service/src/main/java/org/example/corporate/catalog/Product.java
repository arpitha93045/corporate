package org.example.corporate.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "in_stock", nullable = false)
    private boolean inStock;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public Category getCategory() { return category; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public long getPriceCents() { return priceCents; }
    public String getImageUrl() { return imageUrl; }
    public boolean isInStock() { return inStock; }
    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int v) { this.stockQuantity = v; }
    public Instant getCreatedAt() { return createdAt; }
}
