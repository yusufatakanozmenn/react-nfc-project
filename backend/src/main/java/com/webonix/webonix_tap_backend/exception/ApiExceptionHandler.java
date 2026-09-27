package com.webonix.webonix_tap_backend.exception;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> concurrentUpdate() {
        return ResponseEntity.status(409).body(Map.of("message", "Kart başka bir işlemde değiştirildi. Sayfayı yenileyip tekrar deneyin."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handle(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(Map.of("message", exception.getReason() == null ? "İşlem gerçekleştirilemedi." : exception.getReason()));
    }
}
