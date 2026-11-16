package com.winup.controller;

import com.winup.dto.NotificationDto;
import com.winup.dto.NotificationReadRequest;
import com.winup.service.NotificationCleanupService;
import com.winup.service.NotificationReadService;
import com.winup.service.NotificationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@ConditionalOnProperty(name = "winup.role", havingValue = "betting-api", matchIfMissing = true)
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final NotificationReadService notificationReadService;
    private final NotificationCleanupService cleanupService;

    public NotificationController(
            NotificationService notificationService,
            NotificationReadService notificationReadService,
            NotificationCleanupService cleanupService
    ) {
        this.notificationService = notificationService;
        this.notificationReadService = notificationReadService;
        this.cleanupService = cleanupService;
    }

    @GetMapping
    public List<NotificationDto> notifications(Authentication authentication) {
        return notificationService.list(authentication.getName());
    }

    @GetMapping("/read")
    public List<String> readIds(Authentication authentication) {
        return notificationReadService.readIds(authentication.getName());
    }

    @PostMapping("/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@RequestBody NotificationReadRequest request, Authentication authentication) {
        notificationReadService.markRead(authentication.getName(), request.ids());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(Authentication authentication) {
        cleanupService.clear(authentication.getName());
    }
}
