package com.winup.controller;

import com.winup.dto.WithdrawalDto;
import com.winup.dto.WithdrawalRequest;
import com.winup.service.WithdrawalService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@ConditionalOnProperty(name = "winup.role", havingValue = "betting-api", matchIfMissing = true)
@RestController
@RequestMapping("/api/v1")
public class WithdrawalController {
    private final WithdrawalService withdrawalService;

    public WithdrawalController(WithdrawalService withdrawalService) {
        this.withdrawalService = withdrawalService;
    }

    @PostMapping("/payments/withdrawals")
    public WithdrawalDto create(
            @RequestBody WithdrawalRequest request,
            Authentication authentication,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return withdrawalService.create(authentication.getName(), request, idempotencyKey);
    }

    @GetMapping("/payments/withdrawals")
    public List<WithdrawalDto> list(Authentication authentication) {
        return withdrawalService.list(authentication.getName());
    }

    @GetMapping("/payments/withdrawals/{id}")
    public WithdrawalDto find(@PathVariable UUID id, Authentication authentication) {
        return withdrawalService.find(id, authentication.getName());
    }
}
