package com.winup.service.messaging;

import com.winup.entity.UserEntity;
import com.winup.repository.UserRepository;
import com.winup.service.EmailPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailNotificationConsumerTest {
    @Mock
    private EmailPort emailPort;

    @Mock
    private UserRepository userRepository;

    @Test
    void should_send_the_win_email_with_the_match_and_amount() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user("user-1", "demo@winup.app")));

        new EmailNotificationConsumer(emailPort, userRepository).emailWin(payload("winnings", "9250.00"));

        verify(emailPort).sendWin("demo@winup.app", "Alex Martínez", "Gen.G vs T1", new BigDecimal("9250.00"));
    }

    @Test
    void should_send_the_loss_email_with_the_stake() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user("user-1", "demo@winup.app")));

        new EmailNotificationConsumer(emailPort, userRepository).emailLoss(payload("lost", "2000.00"));

        verify(emailPort).sendLoss("demo@winup.app", "Alex Martínez", "Gen.G vs T1", new BigDecimal("2000.00"));
    }

    @Test
    void should_send_the_promotion_email_to_every_user() {
        when(userRepository.findAll()).thenReturn(List.of(
                user("user-1", "demo@winup.app"),
                user("user-2", "otro@winup.app")
        ));

        new EmailNotificationConsumer(emailPort, userRepository).emailPromotion(Map.of(
                "title", "Bono de bienvenida",
                "detail", "Deposita y recibe un bono"
        ));

        verify(emailPort, times(2)).sendPromotion(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("Bono de bienvenida"),
                org.mockito.ArgumentMatchers.eq("Deposita y recibe un bono")
        );
    }

    private Map<String, Object> payload(String amountKey, String amount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", "user-1");
        payload.put("matchName", "Gen.G vs T1");
        payload.put(amountKey, new BigDecimal(amount));
        return payload;
    }

    private UserEntity user(String id, String email) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(email);
        user.setDisplayName("Alex Martínez");
        user.setRole("USER");
        user.setStatus("ACTIVE");
        return user;
    }
}
