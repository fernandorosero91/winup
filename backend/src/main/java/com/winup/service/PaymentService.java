package com.winup.service;

import com.winup.entity.MovementEntity;
import com.winup.entity.PaymentEventEntity;
import com.winup.entity.PaymentIntentEntity;
import com.winup.entity.WalletEntity;
import com.winup.exception.ValidationException;
import com.winup.model.vo.DepositAmount;
import com.winup.model.vo.IdempotencyKey;
import com.winup.dto.DepositDto;
import com.winup.dto.DepositRequest;
import com.winup.service.payment.CheckoutSession;
import com.winup.service.payment.DepositOrder;
import com.winup.service.payment.PaymentGateway;
import com.winup.service.payment.ProviderNotification;
import com.winup.service.payment.ProviderPayment;
import com.winup.repository.MovementRepository;
import com.winup.repository.PaymentEventRepository;
import com.winup.repository.PaymentIntentRepository;
import com.winup.repository.WalletRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentGateway gateway;
    private final PaymentIntentRepository intentRepository;
    private final PaymentEventRepository eventRepository;
    private final LedgerService ledgerService;
    private final WalletRepository walletRepository;
    private final MovementRepository movementRepository;
    private final Clock clock;

    public PaymentService(
            PaymentGateway gateway,
            PaymentIntentRepository intentRepository,
            PaymentEventRepository eventRepository,
            LedgerService ledgerService,
            WalletRepository walletRepository,
            MovementRepository movementRepository,
            Clock clock
    ) {
        this.gateway = gateway;
        this.intentRepository = intentRepository;
        this.eventRepository = eventRepository;
        this.ledgerService = ledgerService;
        this.walletRepository = walletRepository;
        this.movementRepository = movementRepository;
        this.clock = clock;
    }

    @Transactional
    public DepositDto createDeposit(String userId, DepositRequest request, String idempotencyKey) {
        if (request == null || request.amount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos de depósito incompletos");
        }
        IdempotencyKey key = new IdempotencyKey(idempotencyKey);
        PaymentIntentEntity existing = intentRepository.findByIdempotencyKey(key.value()).orElse(null);
        if (existing != null) {
            return toDto(existing);
        }

        DepositAmount amount = DepositAmount.parse(request.amount());
        Instant now = clock.instant();
        PaymentIntentEntity intent = new PaymentIntentEntity();
        intent.setId(UUID.randomUUID());
        intent.setUserId(userId);
        intent.setType("DEPOSIT");
        intent.setStatus("CREATED");
        intent.setAmountCop(amount.amount());
        intent.setProvider(gateway.provider());
        intent.setExternalReference(intent.getId().toString());
        intent.setIdempotencyKey(key.value());
        intent.setCreatedAt(now);
        intent.setUpdatedAt(now);
        intentRepository.save(intent);

        CheckoutSession session = gateway.createDeposit(new DepositOrder(
                intent.getId(),
                amount.amount(),
                "COP",
                "Saldo WinUp",
                request.returnUrl()
        ));
        intent.setProviderRef(session.providerReference());
        intent.setCheckoutUrl(session.checkoutUrl());
        intent.setStatus("PENDING");
        intent.setUpdatedAt(clock.instant());
        intentRepository.save(intent);
        return toDto(intent);
    }

    @Transactional
    public void handleWebhook(String payload) {
        ProviderNotification notification = gateway.parseNotification(payload);
        if (eventRepository.existsByProviderAndProviderEventId(gateway.provider(), notification.eventId())) {
            return;
        }

        ProviderPayment payment = gateway.fetchPayment(notification.paymentReference());
        PaymentIntentEntity intent = intentRepository.findByExternalReference(payment.externalReference())
                .orElseThrow(() -> new ValidationException("PAYMENT_NOT_FOUND", "Intento de pago no encontrado"));

        if (payment.amount().compareTo(intent.getAmountCop()) != 0 || !"COP".equalsIgnoreCase(payment.currency())) {
            throw new ValidationException("PAYMENT_MISMATCH", "El importe o la moneda no coinciden");
        }

        if ("approved".equalsIgnoreCase(payment.status())) {
            credit(intent);
        } else {
            intent.setStatus(payment.status().toUpperCase());
            intent.setUpdatedAt(clock.instant());
            intentRepository.save(intent);
        }

        PaymentEventEntity event = new PaymentEventEntity();
        event.setId(UUID.randomUUID());
        event.setProvider(gateway.provider());
        event.setProviderEventId(notification.eventId());
        event.setSignatureValid(true);
        event.setPayloadHash(Integer.toHexString(payload.hashCode()));
        event.setReceivedAt(clock.instant());
        event.setProcessedAt(clock.instant());
        event.setResult(payment.status());
        eventRepository.save(event);
    }

    @Transactional
    public DepositDto simulate(UUID intentId, String outcome, String userId) {
        PaymentIntentEntity intent = intentRepository.findById(intentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Intento no encontrado"));
        if (!intent.getUserId().equals(userId) || !"SIMULATOR".equals(gateway.provider())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes simular este pago");
        }
        if ("APPROVED".equalsIgnoreCase(outcome)) {
            credit(intent);
        } else {
            intent.setStatus("FAILED");
            intent.setUpdatedAt(clock.instant());
            intentRepository.save(intent);
        }
        return toDto(intent);
    }

    public DepositDto find(UUID intentId, String userId) {
        PaymentIntentEntity intent = intentRepository.findById(intentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Intento no encontrado"));
        if (!intent.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes ver este pago");
        }
        return toDto(intent);
    }

    private void credit(PaymentIntentEntity intent) {
        if ("PAID".equals(intent.getStatus())) {
            return;
        }
        WalletEntity wallet = walletRepository.findWithLockByUserId(intent.getUserId())
                .orElseThrow(() -> new ValidationException("WALLET_NOT_FOUND", "Cartera no encontrada"));
        BigDecimal amount = intent.getAmountCop();
        wallet.setBalance(wallet.getBalance().add(amount).setScale(2, RoundingMode.HALF_EVEN));
        walletRepository.save(wallet);

        Instant now = clock.instant();
        MovementEntity movement = new MovementEntity();
        movement.setId(UUID.randomUUID());
        movement.setUserId(intent.getUserId());
        movement.setConcept("Depósito " + gateway.provider());
        movement.setAmount(amount);
        movement.setType("INGRESO");
        movement.setCreatedAt(now);
        movementRepository.save(movement);

        ledgerService.recordDeposit(intent, amount, now);

        intent.setStatus("PAID");
        intent.setUpdatedAt(now);
        intentRepository.save(intent);
    }

    private DepositDto toDto(PaymentIntentEntity intent) {
        return new DepositDto(
                intent.getId(),
                intent.getStatus(),
                intent.getAmountCop().toPlainString(),
                intent.getCheckoutUrl(),
                intent.getProvider()
        );
    }
}
