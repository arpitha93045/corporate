package org.example.corporate.agent;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A server-priced draft cart produced by the agent's create_draft_cart tool.
 * Addressed publicly by an opaque {@code token}; the frontend fetches it by
 * token and adopts the lines into its client-side cart. Line items snapshot
 * price/name so the draft stays correct if a product later changes.
 */
@Entity
@Table(name = "draft_cart")
public class DraftCart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "total_cents", nullable = false)
    private long totalCents;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "draftCart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DraftCartItem> items = new ArrayList<>();

    public void addItem(DraftCartItem item) {
        item.setDraftCart(this);
        items.add(item);
    }

    public Long getId() { return id; }
    public String getToken() { return token; }
    public void setToken(String v) { this.token = v; }
    public long getTotalCents() { return totalCents; }
    public void setTotalCents(long v) { this.totalCents = v; }
    public Instant getCreatedAt() { return createdAt; }
    public List<DraftCartItem> getItems() { return items; }
}
