package com.winup.controller;

import com.winup.dto.DeviceTokenRequest;
import com.winup.service.DeviceTokenService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnProperty(name = "winup.role", havingValue = "betting-api", matchIfMissing = true)
@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {
    private final DeviceTokenService deviceTokenService;

    public DeviceController(DeviceTokenService deviceTokenService) {
        this.deviceTokenService = deviceTokenService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@RequestBody DeviceTokenRequest request, Authentication authentication) {
        deviceTokenService.register(authentication.getName(), request.token(), request.platform());
    }

    @DeleteMapping("/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregister(@PathVariable String token) {
        deviceTokenService.unregister(token);
    }
}
