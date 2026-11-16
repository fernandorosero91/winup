package com.winup.service.sports;

import com.winup.dto.ProviderHealthDto;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class SportsProviderHealthService {
    private final ObjectProvider<SportsProvider> providerProvider;

    public SportsProviderHealthService(ObjectProvider<SportsProvider> providerProvider) {
        this.providerProvider = providerProvider;
    }

    public ProviderHealthDto health() {
        SportsProvider provider = providerProvider.getIfAvailable();
        if (provider == null) {
            return new ProviderHealthDto(
                    "NONE",
                    false,
                    0,
                    "Sin proveedor externo activo; modo random"
            );
        }
        ProviderHealth health = provider.health();
        return new ProviderHealthDto(
                health.provider(),
                health.available(),
                health.remainingQuota(),
                health.message()
        );
    }
}
