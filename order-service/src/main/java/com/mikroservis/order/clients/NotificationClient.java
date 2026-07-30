package com.mikroservis.order.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
     * Bildirim gonderir. Onemli tasarim karari: bildirim gonderilemezse
     * siparisi IPTAL ETMIYORUZ, sadece logluyoruz. Cunku bildirim
     * "kritik olmayan" bir yan islem. Bu tur islemler icin ilerleyen
     * asamada RabbitMQ ile asenkron mesajlasmaya gecilebilir.
     */
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
        } catch (Exception e) {
            log.warn("Bildirim gonderilemedi (siparis {} yine de gecerli): {}", orderId, e.getMessage());
        }
    }
}
