package com.winup.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.winup.dto.UserDto;
import com.winup.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnProperty(name = "winup.role", havingValue = "betting-api", matchIfMissing = true)
@RestController
@RequestMapping("/api/v1")
public class MeController {
    private final AuthService authService;

    public MeController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public UserDto me(Authentication authentication) {
        return authService.me(authentication.getName());
    }
}
