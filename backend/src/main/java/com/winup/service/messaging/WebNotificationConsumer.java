package com.winup.service.messaging;

import com.winup.config.RabbitConfig;
import com.winup.controller.websocket.DashboardSocketHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WebNotificationConsumer {
    private static final String UPDATE_SIGNAL = "{\"type\":\"NOTIFICATIONS_UPDATED\"}";

    private final DashboardSocketHandler socketHandler;

    public WebNotificationConsumer(DashboardSocketHandler socketHandler) {
        this.socketHandler = socketHandler;
    }

    @RabbitListener(queues = RabbitConfig.WEB_QUEUE)
    public void consume(Map<String, Object> payload) {
        socketHandler.broadcast(UPDATE_SIGNAL);
    }
}
