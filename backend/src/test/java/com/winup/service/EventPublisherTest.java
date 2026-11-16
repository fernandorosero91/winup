package com.winup.service;

import com.winup.entity.MatchEntity;
import com.winup.service.outbox.OutboxWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventPublisherTest {
    private static final long WINDOW_MS = 60000L;

    @Mock
    private OutboxWriter outbox;

    @Test
    void should_spread_simulation_delays_between_window_and_half_extra() {
        EventPublisher publisher = new EventPublisher(outbox, WINDOW_MS);

        publisher.matchCreated(match("0f8d1a52-0000-4000-8000-000000000001"));
        publisher.matchCreated(match("7c1e2b34-0000-4000-8000-000000000002"));

        ArgumentCaptor<Long> delays = ArgumentCaptor.forClass(Long.class);
        verify(outbox, org.mockito.Mockito.times(2)).rabbit(
                anyString(),
                eq("match.simulate"),
                anyMap(),
                delays.capture()
        );

        assertThat(delays.getAllValues())
                .allSatisfy(delay -> assertThat(delay)
                        .isGreaterThanOrEqualTo(WINDOW_MS)
                        .isLessThan(WINDOW_MS + WINDOW_MS / 2));
        assertThat(delays.getAllValues().get(0)).isEqualTo(
                WINDOW_MS + Math.floorMod(
                        java.util.UUID.fromString("0f8d1a52-0000-4000-8000-000000000001").hashCode(),
                        30000
                )
        );
        assertThat(delays.getAllValues().get(0)).isNotEqualTo(delays.getAllValues().get(1));
    }

    @Test
    void should_publish_user_notifications_to_topic_and_fanout() {
        EventPublisher publisher = new EventPublisher(outbox, WINDOW_MS);
        MatchEntity match = match("0f8d1a52-0000-4000-8000-000000000001");
        com.winup.entity.BetEntity bet = new com.winup.entity.BetEntity();
        bet.setId(java.util.UUID.fromString("33333333-3333-3333-3333-333333333333"));
        bet.setUserId("user-1");
        bet.setStake(new java.math.BigDecimal("1000.00"));
        bet.setOdds(new java.math.BigDecimal("2.00"));

        publisher.notifyEmailWin(bet, match, new java.math.BigDecimal("2000.00"));

        verify(outbox).rabbit(
                eq("notifications"),
                eq("notify.email.win"),
                org.mockito.ArgumentMatchers.<Map<String, Object>>any(),
                anyLong()
        );
        verify(outbox).rabbit(
                eq("notifications.fanout"),
                eq(""),
                org.mockito.ArgumentMatchers.<Map<String, Object>>any(),
                anyLong()
        );
    }

    @Test
    void should_publish_promotions_to_topic_and_fanout() {
        new EventPublisher(outbox, WINDOW_MS).promotion("Bono", "Deposita y recibe un bono");

        verify(outbox).rabbit(
                eq("notifications"),
                eq("notify.promo"),
                org.mockito.ArgumentMatchers.<Map<String, Object>>any(),
                anyLong()
        );
        verify(outbox).rabbit(
                eq("notifications.fanout"),
                eq(""),
                org.mockito.ArgumentMatchers.<Map<String, Object>>any(),
                anyLong()
        );
    }

    @Test
    void should_publish_match_created_on_the_delayed_exchange() {        EventPublisher publisher = new EventPublisher(outbox, WINDOW_MS);

        publisher.matchCreated(match("0f8d1a52-0000-4000-8000-000000000001"));

        verify(outbox).rabbit(
                eq("match.delayed"),
                eq("match.simulate"),
                org.mockito.ArgumentMatchers.<Map<String, Object>>any(),
                anyLong()
        );
    }

    private MatchEntity match(String id) {
        MatchEntity match = new MatchEntity();
        match.setId(java.util.UUID.fromString(id));
        match.setLeague("Premier League");
        match.setHomeTeam("Manchester City");
        match.setAwayTeam("Liverpool");
        return match;
    }
}
