package com.winup.service;

import com.winup.entity.PaymentIntentEntity;
import com.winup.entity.WalletEntity;
import com.winup.exception.ValidationException;
import com.winup.dto.DepositDto;
import com.winup.dto.DepositRequest;
import com.winup.service.payment.CheckoutSession;
import com.winup.service.payment.PaymentGateway;
import com.winup.service.payment.ProviderNotification;
import com.winup.service.payment.ProviderPayment;
import com.winup.repository.MovementRepository;
import com.winup.repository.PaymentEventRepository;
import com.winup.repository.PaymentIntentRepository;
import com.winup.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    private static final String USER = "user-1";
    private static final String KEY = "idem-1";
    private static final UUID INTENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private PaymentGateway gateway;

    @Mock
    private PaymentIntentRepository intentRepository;

    @Mock
    private PaymentEventRepository eventRepository;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private MovementRepository movementRepository;

    private PaymentService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-12T18:00:00Z"), ZoneOffset.UTC);
        service = new PaymentService(
                gateway,
                intentRepository,
                eventRepository,
                ledgerService,
                walletRepository,
                movementRepository,
                clock
        );
    }

    @Test
    void should_create_a_pending_deposit_with_checkout_url() {
        when(gateway.provider()).thenReturn("SIMULATOR");
        when(intentRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(gateway.createDeposit(any())).thenReturn(
                new CheckoutSession("sim-1", "/cartera?simular=" + INTENT_ID)
        );

        DepositDto deposit = service.createDeposit(USER, new DepositRequest("50000.00"), KEY);

        assertThat(deposit.status()).isEqualTo("PENDING");
        assertThat(deposit.amountCop()).isEqualTo("50000.00");
        assertThat(deposit.checkoutUrl()).contains(INTENT_ID.toString());
        verify(intentRepository, times(2)).save(any(PaymentIntentEntity.class));
    }

    @Test
    void should_return_the_existing_deposit_for_the_same_idempotency_key() {
        PaymentIntentEntity existing = pendingIntent();
        when(intentRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(existing));

        DepositDto deposit = service.createDeposit(USER, new DepositRequest("50000.00"), KEY);

        assertThat(deposit.id()).isEqualTo(INTENT_ID);
        verify(gateway, never()).createDeposit(any());
    }

    @Test
    void should_credit_an_approved_simulated_deposit_once() {
        PaymentIntentEntity intent = pendingIntent();
        when(intentRepository.findById(INTENT_ID)).thenReturn(Optional.of(intent));
        when(walletRepository.findWithLockByUserId(USER)).thenReturn(Optional.of(wallet()));
        when(gateway.provider()).thenReturn("SIMULATOR");

        DepositDto deposit = service.simulate(INTENT_ID, "APPROVED", USER);

        assertThat(deposit.status()).isEqualTo("PAID");
        assertThat(intent.getStatus()).isEqualTo("PAID");
        verify(walletRepository).save(any(WalletEntity.class));
        verify(movementRepository).save(any());
        verify(ledgerService).recordDeposit(any(), any(), any());
    }

    @Test
    void should_not_credit_a_failed_simulated_deposit() {
        PaymentIntentEntity intent = pendingIntent();
        when(intentRepository.findById(INTENT_ID)).thenReturn(Optional.of(intent));
        when(gateway.provider()).thenReturn("SIMULATOR");

        DepositDto deposit = service.simulate(INTENT_ID, "FAILED", USER);

        assertThat(deposit.status()).isEqualTo("FAILED");
        verify(walletRepository, never()).findWithLockByUserId(any());
        verify(ledgerService, never()).recordDeposit(any(), any(), any());
    }

    @Test
    void should_ignore_a_duplicate_webhook() {
        when(gateway.parseNotification("payload"))
                .thenReturn(new ProviderNotification("event-1", "123"));
        when(gateway.provider()).thenReturn("MERCADOPAGO");
        when(eventRepository.existsByProviderAndProviderEventId("MERCADOPAGO", "event-1"))
                .thenReturn(true);

        service.handleWebhook("payload");

        verify(gateway, never()).fetchPayment(any());
        verify(intentRepository, never()).findByExternalReference(any());
    }

    @Test
    void should_reject_a_webhook_with_a_different_amount() {
        PaymentIntentEntity intent = pendingIntent();
        when(gateway.parseNotification("payload"))
                .thenReturn(new ProviderNotification("event-2", "123"));
        when(gateway.provider()).thenReturn("MERCADOPAGO");
        when(eventRepository.existsByProviderAndProviderEventId("MERCADOPAGO", "event-2"))
                .thenReturn(false);
        when(gateway.fetchPayment("123")).thenReturn(new ProviderPayment(
                "123",
                INTENT_ID.toString(),
                new BigDecimal("10000.00"),
                "COP",
                "approved"
        ));
        when(intentRepository.findByExternalReference(INTENT_ID.toString()))
                .thenReturn(Optional.of(intent));

        assertThatThrownBy(() -> service.handleWebhook("payload"))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.code()).isEqualTo("PAYMENT_MISMATCH"));

        verify(walletRepository, never()).findWithLockByUserId(any());
    }

    @Test
    void should_credit_an_approved_mercado_pago_webhook() {
        PaymentIntentEntity intent = pendingIntent();
        when(gateway.parseNotification("payload"))
                .thenReturn(new ProviderNotification("event-3", "123"));
        when(gateway.provider()).thenReturn("MERCADOPAGO");
        when(eventRepository.existsByProviderAndProviderEventId("MERCADOPAGO", "event-3"))
                .thenReturn(false);
        when(gateway.fetchPayment("123")).thenReturn(new ProviderPayment(
                "123",
                INTENT_ID.toString(),
                new BigDecimal("50000.00"),
                "COP",
                "approved"
        ));
        when(intentRepository.findByExternalReference(INTENT_ID.toString()))
                .thenReturn(Optional.of(intent));
        when(walletRepository.findWithLockByUserId(USER)).thenReturn(Optional.of(wallet()));

        service.handleWebhook("payload");

        assertThat(intent.getStatus()).isEqualTo("PAID");
        verify(eventRepository).save(any());
        verify(walletRepository).save(any(WalletEntity.class));
    }

    private PaymentIntentEntity pendingIntent() {
        PaymentIntentEntity intent = new PaymentIntentEntity();
        intent.setId(INTENT_ID);
        intent.setUserId(USER);
        intent.setType("DEPOSIT");
        intent.setStatus("PENDING");
        intent.setAmountCop(new BigDecimal("50000.00"));
        intent.setProvider("SIMULATOR");
        intent.setExternalReference(INTENT_ID.toString());
        intent.setCheckoutUrl("/cartera?simular=" + INTENT_ID);
        return intent;
    }

    private WalletEntity wallet() {
        WalletEntity wallet = new WalletEntity();
        wallet.setUserId(USER);
        wallet.setBalance(new BigDecimal("100000.00"));
        return wallet;
    }
}
