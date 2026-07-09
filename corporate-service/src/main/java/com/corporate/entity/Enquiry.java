package com.corporate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "enquiry")
public class Enquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(name = "company_name")
    private String companyName;

    private String phone;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "estimated_quantity")
    private Integer estimatedQuantity;

    private String occasion;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "budget_range")
    private String budgetRange;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnquiryStatus status = EnquiryStatus.NEW;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String v) { this.companyName = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }
    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }
    public Integer getEstimatedQuantity() { return estimatedQuantity; }
    public void setEstimatedQuantity(Integer v) { this.estimatedQuantity = v; }
    public String getOccasion() { return occasion; }
    public void setOccasion(String v) { this.occasion = v; }
    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate v) { this.eventDate = v; }
    public String getBudgetRange() { return budgetRange; }
    public void setBudgetRange(String v) { this.budgetRange = v; }
    public EnquiryStatus getStatus() { return status; }
    public void setStatus(EnquiryStatus v) { this.status = v; }
    public Instant getCreatedAt() { return createdAt; }
}
