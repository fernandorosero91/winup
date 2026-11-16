package com.winup.controller;

import com.winup.dto.WithdrawalDecisionRequest;
import com.winup.dto.WithdrawalDto;
import com.winup.service.WithdrawalService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@ConditionalOnProperty(name = "winup.role", havingValue = "betting-api", matchIfMissing = true)
@RestController
@RequestMapping("/api/v1/admin/withdrawals")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWithdrawalController {
    private final WithdrawalService withdrawalService;

    public AdminWithdrawalController(WithdrawalService withdrawalService) {
        this.withdrawalService = withdrawalService;
    }

    @GetMapping
    public List<WithdrawalDto> list() {
        return withdrawalService.listAll();
    }

    @PostMapping("/{id}/approve")
    public WithdrawalDto approve(@PathVariable UUID id, Authentication authentication) {
        return withdrawalService.approve(id, authentication.getName());
    }

    @PostMapping("/{id}/reject")
    public WithdrawalDto reject(
            @PathVariable UUID id,
            @RequestBody(required = false) WithdrawalDecisionRequest request,
            Authentication authentication
    ) {
        String reason = request == null ? null : request.reason();
        return withdrawalService.reject(id, authentication.getName(), reason);
    }
}
