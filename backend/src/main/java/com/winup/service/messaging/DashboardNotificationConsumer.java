package com.winup.service.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winup.config.RabbitConfig;
import com.winup.controller.websocket.DashboardSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DashboardNotificationConsumer {
    private static final Logger log = LoggerFactory.getLogger(DashboardNotificationConsumer.class);

    private final DashboardSocketHandler socketHandler;
    private final ObjectMapper objectMapper;

    public DashboardNotificationConsumer(DashboardSocketHandler socketHandler, ObjectMapper objectMapper) {
        this.socketHandler = socketHandler;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitConfig.DASHBOARD_QUEUE)
    public void dashboard(Map<String, Object> payload) {
        try {
            socketHandler.broadcast(objectMapper.writeValueAsString(payload));
        } catch (Exception exception) {
            log.error("No se pudo enviar la actualizacion al dashboard", exception);
        }
    }
}
