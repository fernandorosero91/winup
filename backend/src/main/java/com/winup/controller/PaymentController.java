package com.winup.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.winup.dto.DepositDto;
import com.winup.dto.DepositRequest;
import com.winup.service.PaymentService;
import com.winup.service.payment.MercadoPagoSignatureValidator;
import com.winup.service.payment.PaymentReturnPage;
import com.winup.service.payment.ReturnUrlPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@ConditionalOnProperty(name = "winup.role", havingValue = "betting-api", matchIfMissing = true)
@RestController
@RequestMapping("/api/v1")
public class PaymentController {
    private final PaymentService paymentService;
    private final MercadoPagoSignatureValidator signatureValidator;
    private final PaymentReturnPage returnPage;
    private final ReturnUrlPolicy returnUrlPolicy;

    public PaymentController(
            PaymentService paymentService,
            MercadoPagoSignatureValidator signatureValidator,
            PaymentReturnPage returnPage,
            ReturnUrlPolicy returnUrlPolicy
    ) {
        this.paymentService = paymentService;
        this.signatureValidator = signatureValidator;
        this.returnPage = returnPage;
        this.returnUrlPolicy = returnUrlPolicy;
    }

    @PostMapping("/payments/deposits")
    public DepositDto deposit(
            @RequestBody DepositRequest request,
            Authentication authentication,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return paymentService.createDeposit(authentication.getName(), request, idempotencyKey);
    }

    @GetMapping("/payments/intents/{id}")
    public DepositDto intent(@PathVariable UUID id, Authentication authentication) {
        return paymentService.find(id, authentication.getName());
    }

    @GetMapping(value = "/payments/return", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> paymentReturn() {
        return html(returnPage.render(null));
    }

    @GetMapping(value = "/payments/return/{target}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> paymentReturnTarget(@PathVariable String target) {
        String redirect = "web".equalsIgnoreCase(target) ? returnUrlPolicy.webReturnUrl() : null;
        return html(returnPage.render(redirect));
    }

    private ResponseEntity<String> html(String body) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(body);
    }

    @PostMapping("/webhooks/mercadopago")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void mercadoPagoWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "x-signature", required = false) String signature,
            @RequestHeader(value = "x-request-id", required = false) String requestId
    ) {
        signatureValidator.validate(payload, signature, requestId);
        paymentService.handleWebhook(payload);
    }

    @PostMapping("/dev/payments/{id}/simulate")
    public DepositDto simulate(
            @PathVariable UUID id,
            @RequestParam String outcome,
            Authentication authentication
    ) {
        return paymentService.simulate(id, outcome, authentication.getName());
    }
}
