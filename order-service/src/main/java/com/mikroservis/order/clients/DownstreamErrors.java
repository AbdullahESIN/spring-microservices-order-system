package com.mikroservis.order.clients;

import org.springframework.web.client.HttpStatusCodeException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Baska bir servisten gelen hata govdesindeki "message" alanini cikarir.
 *
 * Neden onemli? Product Service "Yetersiz stok. Mevcut: 7, istenen: 999" diyor.
 * Bu aciklamayi yutup yerine "bir hata oldu" dersek, hatayi ayiklamak imkansiz
 * hale gelir. Mikroserviste hatanin kaynagini KAYBETMEMEK kritiktir.
 */
final class DownstreamErrors {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DownstreamErrors() {
    }

    static String messageFrom(HttpStatusCodeException ex, String fallback) {
        try {
            JsonNode node = MAPPER.readTree(ex.getResponseBodyAsString());
            JsonNode message = node.get("message");
            if (message != null && !message.isNull() && !message.asString().isBlank()) {
                return message.asString();
            }
        } catch (Exception ignored) {
            // Govde JSON degilse veya bos ise varsayilan mesaja duseriz
        }
        return fallback;
    }
}
