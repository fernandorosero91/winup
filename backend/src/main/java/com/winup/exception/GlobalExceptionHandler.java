package com.winup.exception;

import com.winup.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ApiError(String code, String message, String traceId) {
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? "Datos inválidos" : error.getDefaultMessage())
                .orElse("Datos inválidos");
        return ResponseEntity.unprocessableEntity()
                .body(new ApiError("VALIDATION_ERROR", message, traceId(request)));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> domain(
            ValidationException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.unprocessableEntity()
                .body(new ApiError(exception.code(), exception.getMessage(), traceId(request)));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> status(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        HttpStatus resolved = status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
        String message = exception.getReason() == null ? "Error" : exception.getReason();
        return ResponseEntity.status(resolved)
                .body(new ApiError(codeFor(resolved), message, traceId(request)));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> conflict(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        log.warn("Conflicto de integridad: {}", exception.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("CONFLICT", "El recurso ya existe", traceId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> internal(Exception exception, HttpServletRequest request) {
        log.error("Error no controlado", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("INTERNAL", "Error interno", traceId(request)));
    }

    private String codeFor(HttpStatus status) {
        return switch (status) {
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case FORBIDDEN -> "FORBIDDEN";
            case NOT_FOUND -> "NOT_FOUND";
            case CONFLICT -> "CONFLICT";
            case UNPROCESSABLE_ENTITY -> "VALIDATION_ERROR";
            case TOO_MANY_REQUESTS -> "RATE_LIMITED";
            default -> "ERROR";
        };
    }

    private String traceId(HttpServletRequest request) {
        String correlationId = request.getHeader("X-Correlation-Id");
        return correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID().toString()
                : correlationId;
    }
}
