package com.winup.dto;

import java.util.List;

public record NotificationReadRequest(
        List<String> ids
) {
}
