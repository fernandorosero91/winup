package com.winup.service;

import com.winup.controller.websocket.DashboardSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "winup.push.provider", havingValue = "realtime", matchIfMissing = true)
public class RealtimeMobilePushAdapter implements MobilePushPort {
    private static final Logger log = LoggerFactory.getLogger(RealtimeMobilePushAdapter.class);
    private static final String UPDATE_SIGNAL = "{\"type\":\"NOTIFICATIONS_UPDATED\"}";

    private final DashboardSocketHandler socketHandler;

    public RealtimeMobilePushAdapter(DashboardSocketHandler socketHandler) {
        this.socketHandler = socketHandler;
    }

    @Override
    public void push(String userId, String title, String body) {
        socketHandler.broadcast(UPDATE_SIGNAL);
        log.info("PUSH MOVIL en tiempo real a {}: {} - {}", userId, title, body);
    }
}
