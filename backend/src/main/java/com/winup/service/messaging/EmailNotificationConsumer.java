package com.winup.service.messaging;

import com.winup.config.RabbitConfig;
import com.winup.entity.UserEntity;
import com.winup.repository.UserRepository;
import com.winup.service.EmailPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "winup.role", havingValue = "email", matchIfMissing = true)
public class EmailNotificationConsumer {
    private static final Logger log = LoggerFactory.getLogger(EmailNotificationConsumer.class);

    private final EmailPort emailPort;
    private final UserRepository userRepository;

    public EmailNotificationConsumer(EmailPort emailPort, UserRepository userRepository) {
        this.emailPort = emailPort;
        this.userRepository = userRepository;
    }

    @RabbitListener(queues = RabbitConfig.EMAIL_WIN_QUEUE)
    public void emailWin(Map<String, Object> payload) {
        BigDecimal winnings = decimal(payload.get("winnings"));
        user(payload).ifPresent(user -> emailPort.sendWin(
                user.getEmail(),
                user.getDisplayName(),
                text(payload.get("matchName")),
                winnings
        ));
        log.info("ENVIANDO EMAIL a {}: Ganaste {}!", payload.get("userId"), winnings);
    }

    @RabbitListener(queues = RabbitConfig.EMAIL_LOSS_QUEUE)
    public void emailLoss(Map<String, Object> payload) {
        BigDecimal lost = decimal(payload.get("lost"));
        user(payload).ifPresent(user -> emailPort.sendLoss(
                user.getEmail(),
                user.getDisplayName(),
                text(payload.get("matchName")),
                lost
        ));
        log.info("ENVIANDO EMAIL a {}: Perdiste {}", payload.get("userId"), lost);
    }

    @RabbitListener(queues = RabbitConfig.EMAIL_PROMO_QUEUE)
    public void emailPromotion(Map<String, Object> payload) {
        String title = text(payload.get("title"));
        String detail = text(payload.get("detail"));
        for (UserEntity user : userRepository.findAll()) {
            emailPort.sendPromotion(user.getEmail(), user.getDisplayName(), title, detail);
        }
        log.info("ENVIANDO PROMOCION A LOS USUARIOS: {}", title);
    }

    private Optional<UserEntity> user(Map<String, Object> payload) {
        Object userId = payload.get("userId");
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findById(userId.toString());
    }

    private String text(Object value) {
        return value == null ? "Partido" : value.toString();
    }

    private BigDecimal decimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }
}
