package com.winup.service.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winup.exception.BettingClosedException;
import com.winup.service.engine.BetPlacedEvent;
import com.winup.service.engine.MatchCreatedEvent;
import com.winup.service.engine.MatchEngine;
import com.winup.service.engine.MatchFinishedEvent;
import com.winup.repository.ProcessedEventRepository;
import com.winup.service.EventPublisher;
import com.winup.service.SettlementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaEventConsumerTest {
    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private SettlementService settlementService;

    @Mock
    private MatchEngine matchEngine;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private Acknowledgment acknowledgment;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_dispatch_match_created_to_the_engine() {
        KafkaEventConsumer consumer = consumer();
        UUID eventId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        consumer.consume(event(eventId, "MATCH_CREATED", """
                {"matchId":"%s","home":"Equipo A","away":"Equipo B"}
                """.formatted(matchId)), acknowledgment);

        ArgumentCaptor<MatchCreatedEvent> captor = ArgumentCaptor.forClass(MatchCreatedEvent.class);
        verify(matchEngine).onCreated(captor.capture());
        assertThat(captor.getValue().matchId().value()).isEqualTo(matchId);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void should_dispatch_bet_placed_to_the_engine() {
        KafkaEventConsumer consumer = consumer();
        UUID eventId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        consumer.consume(event(eventId, "BET_PLACED", """
                {"betId":"%s","userId":"user-1","matchId":"%s","selection":"LOCAL",
                 "stake":"100.00","odds":"2.00"}
                """.formatted(UUID.randomUUID(), matchId)), acknowledgment);

        verify(matchEngine).onBet(any(BetPlacedEvent.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void should_close_and_settle_on_match_finished() {
        KafkaEventConsumer consumer = consumer();
        UUID eventId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        consumer.consume(event(eventId, "MATCH_FINISHED", """
                {"matchId":"%s","winner":"LOCAL","homeScore":2,"awayScore":1}
                """.formatted(matchId)), acknowledgment);

        verify(matchEngine).onFinished(any(MatchFinishedEvent.class));
        verify(settlementService).settle(matchId, "LOCAL", 2, 1);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void should_not_ack_an_invalid_event() {
        KafkaEventConsumer consumer = consumer();

        assertThatThrownBy(() -> consumer.consume("{}", acknowledgment))
                .isInstanceOf(IllegalStateException.class);

        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void should_reject_a_late_bet_without_stopping_the_consumer() {
        KafkaEventConsumer consumer = consumer();
        UUID eventId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        doThrow(new BettingClosedException("cerrado")).when(matchEngine).onBet(any());

        consumer.consume(event(eventId, "BET_PLACED", """
                {"betId":"%s","userId":"user-1","matchId":"%s","selection":"LOCAL",
                 "stake":"100.00","odds":"2.00"}
                """.formatted(UUID.randomUUID(), matchId)), acknowledgment);

        verify(acknowledgment).acknowledge();
    }

    private KafkaEventConsumer consumer() {
        return new KafkaEventConsumer(
                objectMapper,
                processedEventRepository,
                settlementService,
                matchEngine,
                eventPublisher
        );
    }

    private String event(UUID eventId, String type, String payload) {
        return """
                {"eventId":"%s","eventType":"%s","payload":%s}
                """.formatted(eventId, type, payload);
    }
}
