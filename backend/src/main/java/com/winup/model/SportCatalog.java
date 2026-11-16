package com.winup.model;

import java.util.List;

public final class SportCatalog {
    public static final String ALL = "TODOS";
    public static final List<String> SPORTS = List.of(
            "Fútbol",
            "AFL",
            "Béisbol",
            "Baloncesto",
            "Fórmula 1",
            "Balonmano",
            "Hockey",
            "MMA",
            "NBA",
            "NFL & NCAA",
            "Rugby",
            "Voleibol",
            "Tenis",
            "eSports"
    );

    private SportCatalog() {
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank() || ALL.equalsIgnoreCase(value)) {
            return ALL;
        }
        String candidate = value.trim();
        return SPORTS.stream()
                .filter(sport -> sport.equalsIgnoreCase(candidate))
                .findFirst()
                .orElse(candidate);
    }

    public static boolean hasDraw(String sport) {
        return switch (sport) {
            case "Baloncesto", "NBA", "Tenis", "eSports", "Béisbol", "Voleibol", "Fórmula 1", "MMA" -> false;
            default -> true;
        };
    }
}
