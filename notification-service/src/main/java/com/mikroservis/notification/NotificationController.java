package com.mikroservis.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Gercek bir e-posta/SMS saglayicisi yerine bildirimi loglayan ve bellekte tutan
 * sahte (mock) servis. Order Service siparis olusunca buraya REST cagrisi yapar.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final List<Notification> sent = new ArrayList<>();

    public record NotificationRequest(
            @NotBlank String recipient,
            @NotBlank String subject,
            @NotBlank String message) {
    }

    public record Notification(
            String recipient,
            String subject,
            String message,
            LocalDateTime sentAt) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Notification send(@Valid @RequestBody NotificationRequest request) {
        Notification notification = new Notification(
                request.recipient(), request.subject(), request.message(), LocalDateTime.now());

        sent.add(notification);
        log.info("BILDIRIM GONDERILDI -> alici={} konu='{}' mesaj='{}'",
                notification.recipient(), notification.subject(), notification.message());

        return notification;
    }

    /** Gonderilen bildirimleri gormek icin (test/debug amacli). */
    @GetMapping
    public List<Notification> getAll() {
        return sent;
    }
}
