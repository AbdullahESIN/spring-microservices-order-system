package com.mikroservis.order.clients;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Notification Service'e yapilan REST cagrilarini kapsuller. */
@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final RestClient client;

    public NotificationClient(@Qualifier("notificationServiceClient") RestClient client) {
        this.client = client;
    }

    public record NotificationRequest(String recipient, String subject, String message) {
    }

    /**
     * Bildirim gonderir.
     *
     * Bu servis "kritik olmayan" bir yan islemdir; bu yuzden tek servis
     * FALLBACK'i olan cagri budur. Bildirim gonderilemezse siparis IPTAL OLMAZ.
     *
     * Circuit breaker burada asil degerini gosterir: Notification Service coktugunde
     * devre acilir ve sonraki siparisler 5 saniyelik timeout'u BEKLEMEDEN gecer.
     * Yani cokmus bir yan servis, ana is akisini yavaslatmayi da birakir.
     */
    @CircuitBreaker(name = "notificationService", fallbackMethod = "bildirimGonderilemedi")
    @Retry(name = "notificationService")
    public void sendOrderConfirmation(String recipient, Long orderId, String productName, int quantity, double total) {
        try {
            client.post()
                    .uri("/api/notifications")
                    .body(new NotificationRequest(
                            recipient,
                            "Siparisiniz alindi (#" + orderId + ")",
                            String.format("%s x%d -- Toplam: %.2f TL", productName, quantity, total)))
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientException e) {
            // Ham RestClientException'i firlatirsak devre kesici onu ARIZA SAYMAZ
            // (record-exceptions yalnizca ServiceUnavailableException'i dinliyor) ve
            // cokmus servisi hic fark etmez. Donusturmek zorunlu.
            throw new ServiceUnavailableException("notification-service",
                    "Bildirim gonderilemedi: Notification Service'e ulasilamiyor", e);
        }
    }

    /**
     * Fallback: imzasi orijinal metotla ayni olmali, sonuna Throwable eklenir.
     * Resilience4j, cagri basarisiz oldugunda veya devre acikken burayi calistirir.
     */
    @SuppressWarnings("unused")
    private void bildirimGonderilemedi(String recipient, Long orderId, String productName,
                                       int quantity, double total, Throwable t) {
        log.warn("Bildirim gonderilemedi (siparis {} yine de gecerli): {}", orderId, t.getMessage());
    }
}
