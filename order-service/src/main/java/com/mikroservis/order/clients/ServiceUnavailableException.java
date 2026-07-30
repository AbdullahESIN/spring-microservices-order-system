package com.mikroservis.order.clients;

/**
 * Karsi servise ULASILAMADIGINDA firlatilir (ag hatasi, timeout, 5xx).
 *
 * Neden ayri bir tip? Circuit breaker'in "ariza" saymasi gereken tek durum budur.
 * Karsi servisin 404 veya 409 donmesi ariza DEGILDIR — servis saglikli calisiyor
 * ve bize dogru cevabi veriyor. Bu ikisini ayirmazsak, kullanicilar olmayan urun
 * istedigi icin devre acilir ve saglikli servise trafik gitmeyi keser.
 */
public class ServiceUnavailableException extends RuntimeException {

    private final String serviceName;

    public ServiceUnavailableException(String serviceName, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
