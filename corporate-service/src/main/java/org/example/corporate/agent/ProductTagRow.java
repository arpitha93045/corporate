package org.example.corporate.agent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * JPA-level view of the product_tag table. Uses a composite id (product_id,
 * tag) because the migration keys the row on the same pair. Kept internal to
 * the agent package — the rest of the app doesn't need to know tags exist.
 */
@Entity
@Table(name = "product_tag")
@IdClass(ProductTagId.class)
public class ProductTagRow {

    @Id
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Id
    @Column(nullable = false, length = 64)
    private String tag;

    public Long getProductId() { return productId; }
    public void setProductId(Long v) { this.productId = v; }
    public String getTag() { return tag; }
    public void setTag(String v) { this.tag = v; }
}
