package com.nuv.tfgrs.exception;
 
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
 
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
 
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Validation errors — returns { errors: [{ field, message }] }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::toMap)
            .toList();
        return ResponseEntity.badRequest().body(Map.of("errors", errors));
    }
 
    // 404 — resource not found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            Map.of("error", ex.getMessage(), "timestamp", LocalDateTime.now())
        );
    }
 
    // Catch-all for unexpected errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            Map.of("error", "An unexpected error occurred", "timestamp", LocalDateTime.now())
        );
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<String> handleOptimisticLock(OptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, Object>> handleAuth(AuthException ex) {
        return ResponseEntity.status(ex.getStatus()).body(
            Map.of(
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now()
            )
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArg(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(
            Map.of(
                "message", ex.getReason() != null ? ex.getReason() : "Solicitud inválida",
                "timestamp", LocalDateTime.now()
            )
        );
    }
 
    private Map<String, String> toMap(FieldError error) {
        return Map.of(
            "field", error.getField(),
            "message", error.getDefaultMessage() != null
                ? error.getDefaultMessage() : "Invalid value"
        );
    }
}
 
