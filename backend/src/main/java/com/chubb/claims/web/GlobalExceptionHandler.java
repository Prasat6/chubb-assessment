package com.chubb.claims.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateTransitionException.class)
    public ResponseEntity<Object> handleIllegalTransition(IllegalStateTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenActionException.class)
    public ResponseEntity<Object> handleForbidden(ForbiddenActionException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body(message));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(body(ex.getReason()));
    }

    /** Malformed JSON, or a bad enum/date value (e.g. "type":"CAR") - previously a bare Spring 400 with no "message". */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(body("Request body is missing, malformed, or has an invalid value"));
    }

    /** Files over spring.servlet.multipart.max-file-size (10MB) are rejected before reaching the controller. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Object> handleTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body("File exceeds the 10MB upload limit"));
    }

    private Map<String, Object> body(String message) {
        // Map.of() throws NullPointerException on a null value, which turned e.g. a
        // ResponseStatusException without a reason into a confusing 500.
        return Map.of("timestamp", Instant.now().toString(), "message", message == null ? "Request failed" : message);
    }
}
