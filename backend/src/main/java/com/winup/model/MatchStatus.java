package com.winup.model;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum MatchStatus {
    OPEN("OPEN"),
    CLOSED("CLOSED"),
    VOID("VOID");

    private static final Map<String, MatchStatus> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(MatchStatus::code, Function.identity()));

    private final String code;

    MatchStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public String label(int minute) {
        return switch (this) {
            case OPEN -> minute > 0 ? "EN VIVO" : "PRÓXIMO";
            case CLOSED -> "FINALIZADO";
            case VOID -> "ANULADO";
        };
    }

    public static MatchStatus from(String code) {
        MatchStatus status = BY_CODE.get(code);
        if (status == null) {
            throw new IllegalArgumentException("Estado de partido no válido: " + code);
        }
        return status;
    }
}
