package com.winup.service;

import com.winup.config.WithdrawalOptions;
import com.winup.dto.WithdrawalDto;
import com.winup.dto.WithdrawalRequest;
import com.winup.entity.WalletEntity;
import com.winup.entity.WithdrawalEntity;
import com.winup.exception.ValidationException;
import com.winup.mapper.WithdrawalMapper;
import com.winup.model.vo.IdempotencyKey;
import com.winup.model.vo.WithdrawalAmount;
import com.winup.repository.WalletRepository;
import com.winup.repository.WithdrawalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class WithdrawalService {
    private static final String PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String PROCESSING = "PROCESSING";
    private static final String REJECTED = "REJECTED";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final WithdrawalRepository withdrawalRepository;
    private final WalletRepository walletRepository;
    private final WithdrawalPayoutExecutor payoutExecutor;
    private final WithdrawalOptions options;
    private final Clock clock;

    public WithdrawalService(
            WithdrawalRepository withdrawalRepository,
            WalletRepository walletRepository,
            WithdrawalPayoutExecutor payoutExecutor,
            WithdrawalOptions options,
            Clock clock
    ) {
        this.withdrawalRepository = withdrawalRepository;
        this.walletRepository = walletRepository;
        this.payoutExecutor = payoutExecutor;
        this.options = options;
        this.clock = clock;
    }

    @Transactional
    public WithdrawalDto create(String userId, WithdrawalRequest request, String idempotencyKey) {
        if (request == null || request.amount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos de retiro incompletos");
        }
        IdempotencyKey key = new IdempotencyKey(idempotencyKey);
        WithdrawalEntity existing = withdrawalRepository.findByIdempotencyKey(key.value()).orElse(null);
        if (existing != null) {
            return WithdrawalMapper.toDto(existing);
        }

        WithdrawalAmount amount = parseAmount(request.amount());
        WalletEntity wallet = lockWallet(userId);
        ensureDailyLimit(userId, amount.amount());
        ensureBalance(wallet, amount.amount());

        WithdrawalEntity withdrawal = newWithdrawal(userId, amount, request, key.value());
        payoutExecutor.hold(wallet, withdrawal);
        withdrawalRepository.save(withdrawal);

        if (amount.amount().compareTo(options.approvalThreshold()) > 0) {
            withdrawal.setStatus(PENDING_APPROVAL);
            withdrawalRepository.save(withdrawal);
            return WithdrawalMapper.toDto(withdrawal);
        }
        WithdrawalPayoutExecutor.PayoutOutcome outcome = payoutExecutor.pay(withdrawal);
        return WithdrawalMapper.toDto(withdrawal, outcome.status(), outcome.message());
    }

    @Transactional
    public WithdrawalDto approve(UUID id, String adminId) {
        WithdrawalEntity withdrawal = lockWithdrawal(id);
        requirePendingApproval(withdrawal);
        withdrawal.setApprovedBy(adminId);
        withdrawal.setStatus(PROCESSING);
        withdrawal.setUpdatedAt(clock.instant());
        withdrawalRepository.save(withdrawal);
        WithdrawalPayoutExecutor.PayoutOutcome outcome = payoutExecutor.pay(withdrawal);
        return WithdrawalMapper.toDto(withdrawal, outcome.status(), outcome.message());
    }

    @Transactional
    public WithdrawalDto reject(UUID id, String adminId, String reason) {
        WithdrawalEntity withdrawal = lockWithdrawal(id);
        requirePendingApproval(withdrawal);
        withdrawal.setApprovedBy(adminId);
        withdrawal.setDecisionReason(reason == null || reason.isBlank() ? "Rechazado por finanzas" : reason);
        WithdrawalPayoutExecutor.PayoutOutcome outcome = payoutExecutor.release(
                withdrawal,
                REJECTED,
                withdrawal.getDecisionReason()
        );
        return WithdrawalMapper.toDto(withdrawal, outcome.status(), outcome.message());
    }

    public List<WithdrawalDto> list(String userId) {
        return withdrawalRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(WithdrawalMapper::toDto)
                .toList();
    }

    public List<WithdrawalDto> listAll() {
        return withdrawalRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(WithdrawalMapper::toDto)
                .toList();
    }

    public WithdrawalDto find(UUID id, String userId) {
        WithdrawalEntity withdrawal = withdrawalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Retiro no encontrado"));
        if (!withdrawal.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes ver este retiro");
        }
        return WithdrawalMapper.toDto(withdrawal);
    }

    private WithdrawalEntity newWithdrawal(
            String userId,
            WithdrawalAmount amount,
            WithdrawalRequest request,
            String idempotencyKey
    ) {
        Instant now = clock.instant();
        WithdrawalEntity withdrawal = new WithdrawalEntity();
        withdrawal.setId(UUID.randomUUID());
        withdrawal.setUserId(userId);
        withdrawal.setAmountCop(amount.amount());
        withdrawal.setStatus(PROCESSING);
        withdrawal.setIdempotencyKey(idempotencyKey);
        withdrawal.setDestination(request.destination());
        withdrawal.setCreatedAt(now);
        withdrawal.setUpdatedAt(now);
        return withdrawal;
    }

    private void ensureBalance(WalletEntity wallet, BigDecimal amount) {
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Saldo insuficiente para el retiro");
        }
    }

    private void ensureDailyLimit(String userId, BigDecimal amount) {
        if (options.dailyLimit().signum() <= 0) {
            return;
        }
        Instant since = LocalDate.now(clock).atStartOfDay(ZoneOffset.UTC).toInstant();
        BigDecimal used = withdrawalRepository.sumActiveSince(userId, since);
        BigDecimal total = (used == null ? ZERO : used).add(amount);
        if (total.compareTo(options.dailyLimit()) > 0) {
            throw new ValidationException("WITHDRAWAL_LIMIT", "Superaste el límite diario de retiros");
        }
    }

    private void requirePendingApproval(WithdrawalEntity withdrawal) {
        if (!PENDING_APPROVAL.equals(withdrawal.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El retiro no está pendiente de aprobación");
        }
    }

    private WalletEntity lockWallet(String userId) {
        return walletRepository.findWithLockByUserId(userId)
                .orElseThrow(() -> new ValidationException("WALLET_NOT_FOUND", "Cartera no encontrada"));
    }

    private WithdrawalEntity lockWithdrawal(UUID id) {
        return withdrawalRepository.findWithLockById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Retiro no encontrado"));
    }

    private WithdrawalAmount parseAmount(String value) {
        try {
            return new WithdrawalAmount(new BigDecimal(value));
        } catch (NumberFormatException exception) {
            throw new ValidationException("INVALID_WITHDRAWAL", "El importe no es válido");
        }
    }
}
