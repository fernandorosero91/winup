package com.winup.service.sports;

import java.util.Objects;

public record ProviderHealth(String provider, boolean available, int remainingQuota, String message) {
    public ProviderHealth {
        Objects.requireNonNull(provider, "El proveedor es obligatorio");
        if (message == null) {
            message = "";
        }
    }
}
