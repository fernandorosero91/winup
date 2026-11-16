package com.winup.service.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.winup.config.RabbitConfig;
import com.winup.entity.MatchEntity;
import com.winup.repository.MatchRepository;
import com.winup.service.EventPublisher;
import com.winup.service.MatchResultSimulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@ConditionalOnProperty(name = "winup.role", havingValue = "simulator", matchIfMissing = true)
@Component
public class SimulationConsumer {
    private static final Logger log = LoggerFactory.getLogger(SimulationConsumer.class);

    private final MatchRepository matchRepository;
    private final EventPublisher eventPublisher;
    private final MatchResultSimulator resultSimulator;

    public SimulationConsumer(
            MatchRepository matchRepository,
            EventPublisher eventPublisher,
            MatchResultSimulator resultSimulator
    ) {
        this.matchRepository = matchRepository;
        this.eventPublisher = eventPublisher;
        this.resultSimulator = resultSimulator;
    }

    @RabbitListener(queues = RabbitConfig.SIMULATION_QUEUE)
    public void simulate(Map<String, Object> payload) {
        Object matchIdValue = payload.get("matchId");
        if (matchIdValue == null) {
            return;
        }
        UUID matchId = UUID.fromString(matchIdValue.toString());
        MatchEntity match = matchRepository.findById(matchId).orElse(null);
        if (match == null || "CLOSED".equals(match.getStatus())) {
            return;
        }

        MatchResultSimulator.Result result = resultSimulator.simulate(match.getSport());

        log.info(
                "Partido {} finalizado: {} {} - {}",
                matchId,
                result.winner(),
                result.homeScore(),
                result.awayScore()
        );
        eventPublisher.matchFinished(match, result.winner(), result.homeScore(), result.awayScore());
    }
}
