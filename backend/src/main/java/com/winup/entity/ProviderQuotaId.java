package com.winup.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class ProviderQuotaId implements Serializable {
    private String provider;
    private LocalDate quotaDay;

    public ProviderQuotaId() {
    }

    public ProviderQuotaId(String provider, LocalDate quotaDay) {
        this.provider = provider;
        this.quotaDay = quotaDay;
    }

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

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProviderQuotaId id)) {
            return false;
        }
        return Objects.equals(provider, id.provider) && Objects.equals(quotaDay, id.quotaDay);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, quotaDay);
    }
}
