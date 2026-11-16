package com.winup.mapper;

import com.winup.dto.WithdrawalDto;
import com.winup.entity.WithdrawalEntity;

import java.math.RoundingMode;

public final class WithdrawalMapper {
    private static final String PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String FAILED = "FAILED";
    private static final String REJECTED = "REJECTED";

    private WithdrawalMapper() {
    }

    public static WithdrawalDto toDto(WithdrawalEntity withdrawal) {
        return toDto(withdrawal, withdrawal.getStatus(), messageFor(withdrawal));
    }

    public static WithdrawalDto toDto(WithdrawalEntity withdrawal, String status, String message) {
        return new WithdrawalDto(
                withdrawal.getId(),
                withdrawal.getUserId(),
                status,
                withdrawal.getAmountCop().setScale(2, RoundingMode.HALF_EVEN).toPlainString(),
                withdrawal.getProvider(),
                message,
                withdrawal.getCreatedAt() == null ? null : withdrawal.getCreatedAt().toString()
        );
    }

    private static String messageFor(WithdrawalEntity withdrawal) {
        if (REJECTED.equals(withdrawal.getStatus()) && withdrawal.getDecisionReason() != null) {
            return withdrawal.getDecisionReason();
        }
        return switch (withdrawal.getStatus()) {
            case PENDING_APPROVAL -> "Tu retiro quedó en revisión.";
            case FAILED -> "No se pudo procesar el retiro. El saldo fue liberado.";
            default -> null;
        };
    }
}
