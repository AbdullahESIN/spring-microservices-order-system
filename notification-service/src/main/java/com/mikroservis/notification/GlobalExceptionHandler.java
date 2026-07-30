package com.mikroservis.notification;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tum hatalari tek bir formatta dondurur.
 *
 * Neden gerekli? Spring varsayilan olarak ResponseStatusException'daki aciklamayi
 * govdeye KOYMAZ (guvenlik gerekcesiyle). Bu yuzden istemci "409" gorur ama nedenini
 * goremez. Burasi o aciklamayi geri kazandirir.
 *
 * "service" alani mikroserviste ayrica degerlidir: hatanin hangi servisten
 * ciktigini tek bakista soyler.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String SERVICE_NAME = "notification-service";

    public record ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path,
            String service,
            Map<String, String> fieldErrors) {
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return ResponseEntity.status(status).body(new ErrorResponse(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                ex.getReason(), request.getRequestURI(), SERVICE_NAME, null));
    }

    /** @Valid dogrulamasi basarisiz oldugunda hangi alanin neden reddedildigini dondurur. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fields.put(fe.getField(), fe.getDefaultMessage()));

        return ResponseEntity.badRequest().body(new ErrorResponse(
                LocalDateTime.now(), 400, "Bad Request",
                "Gonderilen veri gecersiz", request.getRequestURI(), SERVICE_NAME, fields));
    }

    /** Beklenmedik hatalar: ayrintiyi log'a birak, istemciye ic detay sizdirma. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        return ResponseEntity.internalServerError().body(new ErrorResponse(
                LocalDateTime.now(), 500, "Internal Server Error",
                "Beklenmedik bir hata olustu", request.getRequestURI(), SERVICE_NAME, null));
    }
}
