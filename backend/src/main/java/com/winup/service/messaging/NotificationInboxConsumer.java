package com.winup.service.messaging;

import com.winup.config.RabbitConfig;
import com.winup.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
public class NotificationInboxConsumer {
    private final NotificationService notificationService;

    public NotificationInboxConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitConfig.INBOX_QUEUE)
    public void consume(Map<String, Object> payload) {
        if ("PROMO".equals(payload.get("type"))) {
            notificationService.recordPromotion(
                    text(payload.get("title")),
                    text(payload.get("detail"))
            );
            return;
        }
        boolean won = "WIN".equals(payload.get("result"));
        Object amount = won ? payload.get("winnings") : payload.get("lost");
        notificationService.recordResult(
                text(payload.get("userId")),
                uuid(payload.get("betId")),
                uuid(payload.get("matchId")),
                text(payload.get("matchName")),
                won,
                decimal(amount)
        );
    }

    private String text(Object value) {
        return value == null ? "Partido" : value.toString();
    }

    private UUID uuid(Object value) {
        return value == null ? null : UUID.fromString(value.toString());
    }

    private BigDecimal decimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }
}
