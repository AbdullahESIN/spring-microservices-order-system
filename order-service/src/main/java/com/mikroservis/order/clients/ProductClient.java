package com.mikroservis.order.clients;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
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

    /**
     * Urun bilgisini okur.
     *
     * @Retry VAR: bu islem "idempotent" (ayni istegi 10 kez atsan da sonuc degismez).
     * Gecici bir ag hatasinda tekrar denemek guvenlidir ve basari sansini artirir.
     */
    @CircuitBreaker(name = "productService")
    @Retry(name = "productService")
    public ProductResponse getProduct(Long productId) {
        try {
            return client.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .body(ProductResponse.class);

        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Urun bulunamadi: " + productId);

        } catch (HttpClientErrorException e) {
            String reason = DownstreamErrors.messageFrom(e, "Urun bilgisi alinamadi");
            throw new ResponseStatusException(HttpStatus.valueOf(e.getStatusCode().value()), reason);

        } catch (RestClientException e) {
            log.error("Product Service'e ulasilamadi (urun {}): {}", productId, e.getMessage());
            throw new ServiceUnavailableException("product-service",
                    "Urun bilgisi alinamadi: Product Service'e ulasilamiyor", e);
        }
    }

    /**
     * Stogu dusurur.
     *
     * @Retry YOK — ve bu bilincli bir karardir!
     * Bu islem idempotent DEGILDIR: her cagri stogu bir kez daha dusurur.
     * Istek karsi tarafa ulasip cevap donerken ag koparsa, biz hata sanip
     * tekrar denersek stok IKI KEZ duser. Yani musteri 1 adet alir, stoktan 2 duser.
     *
     * Boyle islemlerde ya hic tekrar denenmez (buradaki secim), ya da
     * "idempotency key" ile karsi tarafin ayni istegi iki kez islemesi engellenir.
     */
    @CircuitBreaker(name = "productService")
    public ProductResponse reduceStock(Long productId, int quantity) {
        try {
            return client.post()
                    .uri("/api/products/{id}/reduce-stock", productId)
                    .body(new StockRequest(quantity))
                    .retrieve()
                    .body(ProductResponse.class);

        } catch (HttpClientErrorException e) {
            // Product Service'in kendi aciklamasini aynen tasiyoruz
            // ("Yetersiz stok. Mevcut: 7, istenen: 999" gibi)
            String reason = DownstreamErrors.messageFrom(e, "Stok dusurulemedi");
            log.warn("Stok dusurulemedi (urun {}): {}", productId, reason);
            throw new ResponseStatusException(HttpStatus.valueOf(e.getStatusCode().value()), reason);

        } catch (RestClientException e) {
            log.error("Product Service'e ulasilamadi (urun {}): {}", productId, e.getMessage());
            throw new ServiceUnavailableException("product-service",
                    "Stok dusurulemedi: Product Service'e ulasilamiyor", e);
        }
    }
}
