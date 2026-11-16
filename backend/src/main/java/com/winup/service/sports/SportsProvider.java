package com.winup.service.sports;

import java.util.List;
import java.util.Optional;

public interface SportsProvider {
    String id();

    List<ProviderFixture> fixtures(SportQuery query);

    Optional<ProviderFixture> fixture(String externalId);

    Optional<ProviderOdds> odds(String externalId);

    ProviderHealth health();
}
