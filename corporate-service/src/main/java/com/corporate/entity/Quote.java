package com.corporate.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * An itemized, server-priced quote an admin issues in response to an enquiry.
 * Addressed publicly by an opaque {@code token}; the buyer fetches it by token
 * to accept or decline. Lines snapshot product name/price so the quote stays
 * correct if a product later changes — same pattern as order_item / draft_cart.
 */
@Entity
@Table(name = "quote")
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enquiry_id", nullable = false)
    private Enquiry enquiry;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "total_cents", nullable = false)
    private long totalCents;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuoteStatus status = QuoteStatus.SENT;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuoteLine> lines = new ArrayList<>();

    public void addLine(QuoteLine line) {
        line.setQuote(this);
        lines.add(line);
    }

    public Long getId() { return id; }
    public Enquiry getEnquiry() { return enquiry; }
    public void setEnquiry(Enquiry v) { this.enquiry = v; }
    public String getToken() { return token; }
    public void setToken(String v) { this.token = v; }
    public long getTotalCents() { return totalCents; }
    public void setTotalCents(long v) { this.totalCents = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate v) { this.validUntil = v; }
    public QuoteStatus getStatus() { return status; }
    public void setStatus(QuoteStatus v) { this.status = v; }
    public Instant getCreatedAt() { return createdAt; }
    public List<QuoteLine> getLines() { return lines; }
}
