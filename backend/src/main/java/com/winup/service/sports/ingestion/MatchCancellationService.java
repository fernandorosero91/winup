package com.winup.service.sports.ingestion;

import com.winup.entity.ProviderMatchEntity;
import com.winup.repository.ProviderMatchRepository;
import com.winup.service.SettlementService;
import com.winup.service.sports.ProviderFixture;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Service
public class MatchCancellationService {
    private final ProviderMatchRepository providerMatchRepository;
    private final SettlementService settlementService;
    private final Clock clock;

    public MatchCancellationService(
            ProviderMatchRepository providerMatchRepository,
            SettlementService settlementService,
            Clock clock
    ) {
        this.providerMatchRepository = providerMatchRepository;
        this.settlementService = settlementService;
        this.clock = clock;
    }

    @Transactional
    public boolean voidCancelled(String providerId, ProviderFixture fixture) {
        Optional<ProviderMatchEntity> mapping = providerMatchRepository
                .findByProviderAndExternalId(providerId, fixture.externalId());
        if (mapping.isEmpty()) {
            return false;
        }
        ProviderMatchEntity providerMatch = mapping.get();
        providerMatch.setLastSyncedAt(clock.instant());
        providerMatchRepository.save(providerMatch);
        return settlementService.voidMatch(providerMatch.getMatchId());
    }
}
