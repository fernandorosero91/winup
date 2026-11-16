package com.winup.service;

import com.winup.repository.DeviceTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FcmMobilePushAdapterTest {
    @Mock
    private DeviceTokenRepository repository;

    @Test
    void should_skip_the_push_when_the_service_account_is_missing() {
        FcmMobilePushAdapter adapter = new FcmMobilePushAdapter(
                repository,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                Clock.fixed(Instant.parse("2026-11-12T22:00:00Z"), ZoneOffset.UTC),
                "",
                "",
                ""
        );

        adapter.push("user-1", "Apuesta ganada", "Gen.G vs T1");

        verify(repository, never()).findByUserId("user-1");
    }
}
