package com.winup.service.sports.ingestion;

import com.winup.service.sports.ProviderOdds;
import com.winup.service.sports.SportsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProviderOddsFetcher {
    private static final Logger log = LoggerFactory.getLogger(ProviderOddsFetcher.class);

    public ProviderOdds fetch(SportsProvider provider, String externalId) {
        try {
            return provider.odds(externalId).orElse(null);
        } catch (RuntimeException exception) {
            log.warn("No se pudieron obtener cuotas de {}: {}", externalId, exception.getMessage());
            return null;
        }
    }
}
