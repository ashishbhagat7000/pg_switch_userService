package com.vol.pgswitch.config;

import com.vol.pgswitch.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream().map(e -> e.getField() + ": " + e.getDefaultMessage()).toList();
        Map<String, Object> data = new HashMap<>();
        data.put("errors", errors);
        ApiResponse<Map<String, Object>> body = new ApiResponse<>(
                "error",
                "VALIDATION_ERROR",
                "One or more fields are invalid.",
                data,
                defaultMeta()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleIllegalArg(IllegalArgumentException ex) {
        Map<String, Object> data = Map.of("detail", ex.getMessage());
        ApiResponse<Map<String, Object>> body = new ApiResponse<>(
                "error",
                "BAD_REQUEST",
                ex.getMessage(),
                data,
                defaultMeta()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleResponseStatus(ResponseStatusException ex) {
        Map<String, Object> data = Map.of("detail", ex.getReason());
        ApiResponse<Map<String, Object>> body = new ApiResponse<>(
                "error",
                ex.getStatusCode().is4xxClientError() ? "CLIENT_ERROR" : "SERVER_ERROR",
                ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString(),
                data,
                defaultMeta()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleGeneric(Exception ex) {
        Map<String, Object> data = Map.of("detail", ex.getMessage());
        ApiResponse<Map<String, Object>> body = new ApiResponse<>(
                "error",
                "UNEXPECTED_ERROR",
                "Unexpected error occurred. Contact support with requestId.",
                data,
                defaultMeta()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private Map<String, Object> defaultMeta() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("requestId", UUID.randomUUID().toString());
        meta.put("timestamp", Instant.now().toString());
        return meta;
    }
}
