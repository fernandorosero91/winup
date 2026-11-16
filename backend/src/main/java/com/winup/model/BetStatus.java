package com.winup.model;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum BetStatus {
    PENDING("PENDING", "PENDIENTE"),
    WON("WON", "GANADA"),
    LOST("LOST", "PERDIDA"),
    VOID("VOID", "ANULADA");

    private static final Map<String, BetStatus> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(BetStatus::code, Function.identity()));

    private final String code;
    private final String label;

    BetStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public boolean refunded() {
        return this == VOID;
    }

    public static BetStatus from(String code) {
        BetStatus status = BY_CODE.get(code);
        if (status == null) {
            throw new IllegalArgumentException("Estado de apuesta no válido: " + code);
        }
        return status;
    }
}
