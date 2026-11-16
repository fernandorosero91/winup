package com.winup.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "provider_quota")
@IdClass(ProviderQuotaId.class)
public class ProviderQuotaEntity {
    @Id
    @Column(nullable = false)
    private String provider;

    @Id
    @Column(name = "quota_day", nullable = false)
    private LocalDate quotaDay;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "quota_limit", nullable = false)
    private int quotaLimit;

    private Integer remaining;

    @Column(name = "open_until")
    private Instant openUntil;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public LocalDate getQuotaDay() {
        return quotaDay;
    }

    public void setQuotaDay(LocalDate quotaDay) {
        this.quotaDay = quotaDay;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(int usedCount) {
        this.usedCount = usedCount;
    }

    public int getQuotaLimit() {
        return quotaLimit;
    }

    public void setQuotaLimit(int quotaLimit) {
        this.quotaLimit = quotaLimit;
    }

    public Integer getRemaining() {
        return remaining;
    }

    public void setRemaining(Integer remaining) {
        this.remaining = remaining;
    }

    public Instant getOpenUntil() {
        return openUntil;
    }

    public void setOpenUntil(Instant openUntil) {
        this.openUntil = openUntil;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
