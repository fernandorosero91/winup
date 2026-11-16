package com.winup.service.messaging;

import com.winup.service.MobilePushPort;
import com.winup.controller.websocket.DashboardSocketHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChannelNotificationConsumerTest {
    @Mock
    private DashboardSocketHandler socketHandler;

    @Mock
    private MobilePushPort mobilePushPort;

    @Test
    void should_signal_web_clients_when_a_notification_is_delivered() {
        new WebNotificationConsumer(socketHandler).consume(Map.of("result", "WIN"));

        verify(socketHandler).broadcast(contains("NOTIFICATIONS_UPDATED"));
    }

    @Test
    void should_push_a_promotion_to_every_mobile_client() {
        new MobileNotificationConsumer(mobilePushPort).consume(Map.of(
                "type", "PROMO",
                "title", "Bono de bienvenida",
                "detail", "Deposita y recibe un bono"
        ));

        verify(mobilePushPort).push(
                "todos",
                "Bono de bienvenida",
                "Deposita y recibe un bono"
        );
    }

    @Test
    void should_push_the_result_to_the_mobile_channel() {
        new MobileNotificationConsumer(mobilePushPort).consume(Map.of(
                "result", "LOSS",
                "userId", "user-1",
                "matchName", "Gen.G vs T1",
                "lost", new java.math.BigDecimal("50000.00")
        ));

        verify(mobilePushPort).push(
                "user-1",
                "Apuesta perdida",
                "Gen.G vs T1 · perdiste $ 50.000,00"
        );
    }
}
