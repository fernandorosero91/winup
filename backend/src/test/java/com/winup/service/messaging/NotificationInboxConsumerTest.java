package com.winup.service.messaging;

import com.winup.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationInboxConsumerTest {
    private static final UUID BET_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MATCH_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private NotificationService notificationService;

    @Test
    void should_store_the_win_notification_consumed_from_the_fanout() {
        new NotificationInboxConsumer(notificationService).consume(payload("WIN", "winnings", "9250.00"));

        verify(notificationService).recordResult(
                "user-1",
                BET_ID,
                MATCH_ID,
                "Gen.G vs T1",
                true,
                new BigDecimal("9250.00")
        );
    }

    @Test
    void should_store_the_loss_notification_consumed_from_the_fanout() {
        new NotificationInboxConsumer(notificationService).consume(payload("LOSS", "lost", "2000.00"));

        verify(notificationService).recordResult(
                "user-1",
                BET_ID,
                MATCH_ID,
                "Gen.G vs T1",
                false,
                new BigDecimal("2000.00")
        );
    }

    @Test
    void should_store_a_promotion_consumed_from_the_fanout() {
        new NotificationInboxConsumer(notificationService).consume(Map.of(
                "type", "PROMO",
                "title", "Bono de bienvenida",
                "detail", "Deposita y recibe un bono"
        ));

        verify(notificationService).recordPromotion("Bono de bienvenida", "Deposita y recibe un bono");
    }

    private Map<String, Object> payload(String result, String amountKey, String amount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("result", result);
        payload.put("userId", "user-1");
        payload.put("betId", BET_ID.toString());
        payload.put("matchId", MATCH_ID.toString());
        payload.put("matchName", "Gen.G vs T1");
        payload.put(amountKey, new BigDecimal(amount));
        return payload;
    }
}
