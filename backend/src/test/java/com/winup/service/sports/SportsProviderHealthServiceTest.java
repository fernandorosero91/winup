package com.winup.service.sports;

import com.winup.dto.ProviderHealthDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SportsProviderHealthServiceTest {
    @Mock
    private ObjectProvider<SportsProvider> providerProvider;

    @Mock
    private SportsProvider provider;

    @Test
    void should_report_no_provider_when_random_is_active() {
        when(providerProvider.getIfAvailable()).thenReturn(null);

        ProviderHealthDto health = new SportsProviderHealthService(providerProvider).health();

        assertThat(health.provider()).isEqualTo("NONE");
        assertThat(health.available()).isFalse();
        assertThat(health.message()).contains("random");
    }

    @Test
    void should_map_the_active_provider_health() {
        when(providerProvider.getIfAvailable()).thenReturn(provider);
        when(provider.health()).thenReturn(new ProviderHealth(
                "API_FOOTBALL",
                true,
                87,
                "Cuota disponible"
        ));

        ProviderHealthDto health = new SportsProviderHealthService(providerProvider).health();

        assertThat(health.provider()).isEqualTo("API_FOOTBALL");
        assertThat(health.available()).isTrue();
        assertThat(health.remainingQuota()).isEqualTo(87);
        assertThat(health.message()).isEqualTo("Cuota disponible");
    }
}
