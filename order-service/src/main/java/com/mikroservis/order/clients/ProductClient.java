package com.mikroservis.order.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/** Product Service'e yapilan REST cagrilarini kapsuller. */
@Component
public class ProductClient {

    private static final Logger log = LoggerFactory.getLogger(ProductClient.class);

    private final RestClient client;

    public ProductClient(@Qualifier("productServiceClient") RestClient client) {
        this.client = client;
    }

    public record ProductResponse(Long id, String name, String description, double price, int stockQuantity) {
    }

    public record StockRequest(int quantity) {
    }

    public ProductResponse getProduct(Long productId) {
        try {
            return client.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .body(ProductResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            // Urun gercekten yok -> istemcinin hatasi, 404 dondur
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Urun bulunamadi: " + productId);
        } catch (RestClientException e) {
            // Product Service cevap vermiyor/coktu -> bizim altyapi hatamiz, 502 dondur
            log.error("Urun {} alinamadi: {}", productId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Product Service'e ulasilamadi");
        }
    }

    /**
     * Stogu dusurur. Product Service stok yetmezse 409 doner; bunu asagida
     * anlamli bir hata mesajina cevirip siparisi iptal ediyoruz.
     */
    public ProductResponse reduceStock(Long productId, int quantity) {
        try {
            return client.post()
                    .uri("/api/products/{id}/reduce-stock", productId)
                    .body(new StockRequest(quantity))
                    .retrieve()
                    .body(ProductResponse.class);
        } catch (RestClientException e) {
            log.error("Stok dusurulemedi (urun {}): {}", productId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Stok dusurulemedi: yetersiz stok olabilir");
        }
    }
}
