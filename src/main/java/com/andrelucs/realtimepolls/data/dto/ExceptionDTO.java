package com.andrelucs.realtimepolls.data.dto;

import java.time.LocalDateTime;

public record ExceptionDTO(String message, int statusCode, String requestPath, LocalDateTime timestamp) {
}
