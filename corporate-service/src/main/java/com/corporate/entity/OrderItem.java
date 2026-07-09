package com.corporate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "unit_price_cents", nullable = false)
    private long unitPriceCents;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_total_cents", nullable = false)
    private long lineTotalCents;

    public Long getId() { return id; }
    public OrderEntity getOrder() { return order; }
    public void setOrder(OrderEntity v) { this.order = v; }
    public Product getProduct() { return product; }
    public void setProduct(Product v) { this.product = v; }
    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }
    public long getUnitPriceCents() { return unitPriceCents; }
    public void setUnitPriceCents(long v) { this.unitPriceCents = v; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int v) { this.quantity = v; }
    public long getLineTotalCents() { return lineTotalCents; }
    public void setLineTotalCents(long v) { this.lineTotalCents = v; }
}
