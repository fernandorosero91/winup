package com.winup.service.sports;

public record ProviderScore(int home, int away, int minute, String period) {
    public ProviderScore {
        if (home < 0 || away < 0 || minute < 0) {
            throw new IllegalArgumentException("El marcador no puede ser negativo");
        }
        if (period == null || period.isBlank()) {
            period = "NS";
        }
    }
}
