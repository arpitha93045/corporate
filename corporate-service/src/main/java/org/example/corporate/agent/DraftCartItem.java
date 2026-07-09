package org.example.corporate.agent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One priced line of a {@link DraftCart}. Snapshots slug/name/unit price. */
@Entity
@Table(name = "draft_cart_item")
public class DraftCartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "draft_cart_id", nullable = false)
    private DraftCart draftCart;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_slug", nullable = false)
    private String productSlug;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "unit_price_cents", nullable = false)
    private long unitPriceCents;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_total_cents", nullable = false)
    private long lineTotalCents;

    public Long getId() { return id; }
    public DraftCart getDraftCart() { return draftCart; }
    public void setDraftCart(DraftCart v) { this.draftCart = v; }
    public Long getProductId() { return productId; }
    public void setProductId(Long v) { this.productId = v; }
    public String getProductSlug() { return productSlug; }
    public void setProductSlug(String v) { this.productSlug = v; }
    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }
    public long getUnitPriceCents() { return unitPriceCents; }
    public void setUnitPriceCents(long v) { this.unitPriceCents = v; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int v) { this.quantity = v; }
    public long getLineTotalCents() { return lineTotalCents; }
    public void setLineTotalCents(long v) { this.lineTotalCents = v; }
}
