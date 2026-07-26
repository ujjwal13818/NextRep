package com.nextset.auth.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String name;

    @Column(name = "picture_url", length = 512)
    private String pictureUrl;

    @Column(name = "google_id", nullable = false, unique = true)
    private String googleId;

    @Column(name = "bodyweight_kg", precision = 5, scale = 2)
    private BigDecimal bodyweightKg;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public User() {}

    public User(String email, String name, String pictureUrl, String googleId) {
        this.email = email;
        this.name = name;
        this.pictureUrl = pictureUrl;
        this.googleId = googleId;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and setters

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public BigDecimal getBodyweightKg() {
        return bodyweightKg;
    }

    public void setBodyweightKg(BigDecimal bodyweightKg) {
        this.bodyweightKg = bodyweightKg;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}