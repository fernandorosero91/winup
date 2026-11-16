package com.winup.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.winup.dto.BetDto;
import com.winup.mapper.MatchMapper;
import com.winup.dto.MatchDto;
import com.winup.dto.MatchPageDto;
import com.winup.dto.PlaceBetRequest;
import com.winup.dto.ProviderHealthDto;
import com.winup.dto.SportSummaryDto;
import com.winup.dto.WalletDto;
import com.winup.service.BetService;
import com.winup.service.MatchService;
import com.winup.service.sports.SportsProviderHealthService;
import com.winup.controller.websocket.DashboardSocketHandler;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ConditionalOnProperty(name = "winup.role", havingValue = "betting-api", matchIfMissing = true)
@RestController
@RequestMapping("/api/v1")
public class ApiController {
    private final MatchService matchService;
    private final BetService betService;
    private final DashboardSocketHandler socketHandler;
    private final MatchMapper matchMapper;
    private final SportsProviderHealthService providerHealthService;

    public ApiController(
            MatchService matchService,
            BetService betService,
            DashboardSocketHandler socketHandler,
            MatchMapper matchMapper,
            SportsProviderHealthService providerHealthService
    ) {
        this.matchService = matchService;
        this.betService = betService;
        this.socketHandler = socketHandler;
        this.matchMapper = matchMapper;
        this.providerHealthService = providerHealthService;
    }

    @GetMapping("/matches")
    public MatchPageDto matches(
            @RequestParam(defaultValue = "TODOS") String status,
            @RequestParam(defaultValue = "TODOS") String sport,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        return matchService.page(status, sport, page, size);
    }

    @GetMapping("/matches/{id}")
    public MatchDto match(@PathVariable UUID id) {
        return matchService.find(id)
                .map(matchMapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partido no encontrado"));
    }

    @GetMapping("/sports")
    public List<SportSummaryDto> sports() {
        return matchService.sports();
    }

    @GetMapping("/bets")
    public List<BetDto> bets(Authentication authentication) {
        return betService.list(authentication.getName());
    }

    @PostMapping({"/bets", "/bet"})
    public BetDto placeBet(
            @RequestBody PlaceBetRequest request,
            Authentication authentication,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return betService.place(request, authentication.getName(), idempotencyKey);
    }

    @GetMapping("/wallet")
    public WalletDto wallet(Authentication authentication) {
        return betService.wallet(authentication.getName());
    }

    @GetMapping("/health/providers")
    public ProviderHealthDto providerHealth() {
        return providerHealthService.health();
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "matches", matchService.all().size(),
                "websocket", socketHandler.connections()
        );
    }
}
