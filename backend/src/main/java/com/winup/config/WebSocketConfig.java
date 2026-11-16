package com.winup.config;

import com.winup.controller.websocket.DashboardSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final DashboardSocketHandler dashboardSocketHandler;

    public WebSocketConfig(DashboardSocketHandler dashboardSocketHandler) {
        this.dashboardSocketHandler = dashboardSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(dashboardSocketHandler, "/ws/events").setAllowedOrigins("*");
    }
}
