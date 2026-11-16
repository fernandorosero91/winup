package com.winup.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.winup.dto.AdminDashboardDto;
import com.winup.dto.AdminLedgerDto;
import com.winup.dto.AdminPaymentDto;
import com.winup.dto.AdminUserDto;
import com.winup.dto.MailTestRequest;
import com.winup.dto.MatchDto;
import com.winup.dto.MatchPageDto;
import com.winup.dto.CreateMatchRequest;
import com.winup.dto.CreatePromoRequest;
import com.winup.dto.PromoCampaignDto;
import com.winup.dto.PromoRequest;
import com.winup.dto.RoleRequest;
import com.winup.dto.UpdateOddsRequest;
import com.winup.service.AdminMatchService;
import com.winup.service.AdminService;
import com.winup.service.EmailPort;
import com.winup.service.PromoService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ConditionalOnProperty(name = "winup.role", havingValue = "betting-api", matchIfMissing = true)
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;
    private final AdminMatchService adminMatchService;
    private final PromoService promoService;
    private final EmailPort emailPort;

    public AdminController(
            AdminService adminService,
            AdminMatchService adminMatchService,
            PromoService promoService,
            EmailPort emailPort
    ) {
        this.adminService = adminService;
        this.adminMatchService = adminMatchService;
        this.promoService = promoService;
        this.emailPort = emailPort;
    }

    @GetMapping("/dashboard")
    public AdminDashboardDto dashboard() {
        return adminService.dashboard();
    }

    @GetMapping("/users")
    public List<AdminUserDto> users() {
        return adminService.users();
    }

    @PutMapping("/users/{id}/role")
    public AdminUserDto role(@PathVariable String id, @RequestBody RoleRequest request) {
        return adminService.updateRole(id, request.role());
    }

    @GetMapping("/payments")
    public List<AdminPaymentDto> payments() {
        return adminService.payments();
    }

    @GetMapping("/ledger")
    public List<AdminLedgerDto> ledger() {
        return adminService.ledger();
    }

    @GetMapping("/matches")
    public List<MatchDto> matches() {
        return adminService.matches();
    }

    @GetMapping("/matches/page")
    public MatchPageDto matchesPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "TODOS") String status,
            @RequestParam(defaultValue = "TODOS") String sport
    ) {
        return adminService.matchesPage(page, size, status, sport);
    }

    @PostMapping("/matches")
    @ResponseStatus(HttpStatus.CREATED)
    public MatchDto createMatch(@RequestBody CreateMatchRequest request) {
        return adminMatchService.create(request);
    }

    @PutMapping("/matches/{id}/odds")
    public MatchDto updateOdds(@PathVariable UUID id, @RequestBody UpdateOddsRequest request) {
        return adminMatchService.updateOdds(id, request);
    }

    @PostMapping("/matches/ensure-sports")
    public Map<String, Integer> ensureSports() {
        return Map.of("activeSports", adminService.ensureSports());
    }

    @PostMapping("/mail/test")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void testMail(@RequestBody MailTestRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "El correo de destino es obligatorio"
            );
        }
        String kind = request.kind() == null ? "PROMO" : request.kind().trim().toUpperCase();
        switch (kind) {
            case "WIN" -> emailPort.sendWin(
                    request.email(),
                    "Administrador",
                    "Aalborg vs Barcelona",
                    new java.math.BigDecimal("92500.00")
            );
            case "LOSS" -> emailPort.sendLoss(
                    request.email(),
                    "Administrador",
                    "Aalborg vs Barcelona",
                    new java.math.BigDecimal("50000.00")
            );
            default -> emailPort.sendPromotion(
                    request.email(),
                    "Administrador",
                    "Prueba de correo WINUP",
                    "El SMTP de Gmail esta enviando correos reales con la plantilla de WINUP."
            );
        }
    }

    @GetMapping("/promotions")
    public List<PromoCampaignDto> promotions() {
        return promoService.list();
    }

    @PostMapping("/promotions")
    @ResponseStatus(HttpStatus.CREATED)
    public PromoCampaignDto createPromotion(
            @RequestBody CreatePromoRequest request,
            Authentication authentication
    ) {
        return promoService.create(request, authentication.getName());
    }

    @PostMapping("/promotions/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelPromotion(@PathVariable UUID id) {
        promoService.cancel(id);
    }

    @PostMapping("/notifications/promo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void promotion(@RequestBody PromoRequest request) {
        adminService.promote(request.title(), request.detail());
    }

    @PostMapping("/matches/seed")
    public Map<String, Integer> seed(@RequestParam(defaultValue = "300") int count) {
        return Map.of("created", adminService.seed(count));
    }
}
