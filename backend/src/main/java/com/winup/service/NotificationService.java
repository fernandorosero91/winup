package com.winup.service;

import com.winup.entity.BetEntity;
import com.winup.entity.MatchEntity;
import com.winup.entity.NotificationEntity;
import com.winup.dto.NotificationDto;
import com.winup.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class NotificationService {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.of("America/Bogota"));
    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");

    private final NotificationRepository repository;
    private final Clock clock;

    public NotificationService(NotificationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<NotificationDto> list(String userId) {
        return repository.findByUserIdOrUserIdIsNullOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void recordPromotion(String title, String detail) {
        if (title == null || title.isBlank()) {
            return;
        }
        record(null, "PROMOCION", title, detail, null, null, null);
    }

    @Transactional
    public void recordPending(BetEntity bet, MatchEntity match) {
        record(
                bet.getUserId(),
                "PENDIENTE",
                "Apuesta en juego",
                matchName(match) + " · pendiente de liquidación",
                bet.getStake(),
                bet.getId(),
                match == null ? null : match.getId()
        );
    }

    @Transactional
    public void recordResult(
            String userId,
            UUID betId,
            UUID matchId,
            String matchName,
            boolean won,
            BigDecimal amount
    ) {
        String kind = won ? "GANADA" : "PERDIDA";
        String title = won ? "Apuesta ganada" : "Apuesta perdida";
        String detail = won
                ? matchName + " · ganaste " + money(amount) + ", ya está en tu cartera"
                : matchName + " · perdiste " + money(amount);
        record(userId, kind, title, detail, amount, betId, matchId);
    }

    private void record(
            String userId,
            String kind,
            String title,
            String detail,
            BigDecimal amount,
            UUID betId,
            UUID matchId
    ) {
        if (betId != null && repository.existsByBetIdAndKind(betId, kind)) {
            return;
        }
        NotificationEntity notification = new NotificationEntity();
        notification.setId(UUID.randomUUID());
        notification.setUserId(userId);
        notification.setKind(kind);
        notification.setTitle(title);
        notification.setDetail(detail);
        notification.setAmount(amount);
        notification.setBetId(betId);
        notification.setMatchId(matchId);
        notification.setCreatedAt(clock.instant());
        repository.save(notification);
    }

    private NotificationDto toDto(NotificationEntity notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getKind(),
                notification.getTitle(),
                notification.getDetail(),
                notification.getAmount(),
                TIME.format(notification.getCreatedAt()),
                notification.getBetId(),
                notification.getMatchId()
        );
    }

    private String matchName(MatchEntity match) {
        if (match == null) {
            return "Partido";
        }
        return match.getHomeTeam() + " vs " + match.getAwayTeam();
    }

    private String money(BigDecimal amount) {
        if (amount == null) {
            return "$ 0,00";
        }
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(COLOMBIA);
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        return "$ " + format.format(amount);
    }
}
