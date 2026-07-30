package com.mikroservis.order.clients;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/** User Service'e yapilan REST cagrilarini kapsuller. */
@Component
public class UserClient {

    private static final Logger log = LoggerFactory.getLogger(UserClient.class);

    private final RestClient client;

    public UserClient(@Qualifier("userServiceClient") RestClient client) {
        this.client = client;
    }

    public record UserResponse(Long id, String username, String email) {
    }

    /**
     * Token'i User Service'e dogrulatir. Senkron cagri: cevap gelmeden siparis olusturamayiz.
     *
     * Neden @Retry YOK? Token gecersizse tekrar denemek ayni cevabi getirir, bosuna
     * beklemedir. Kimlik dogrulama basarisizligi gecici bir hata degildir.
     *
     * Neden fallback YOK? Kimlik dogrulanamiyorsa siparis KESINLIKLE olusmamali.
     * Guvenlikte "emin degilsen izin verme" kurali gecerlidir.
     */
    @CircuitBreaker(name = "userService")
    public UserResponse validateToken(String authorizationHeader) {
        try {
            return client.get()
                    .uri("/api/auth/validate")
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(UserResponse.class);

        } catch (HttpClientErrorException e) {
            // 401/403: servis saglikli, token gecersiz. Devre acilmamali.
            String reason = DownstreamErrors.messageFrom(e, "Token gecersiz veya suresi dolmus");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, reason);

        } catch (RestClientException e) {
            // Ag hatasi / User Service ayakta degil. Devre bunu ariza sayar.
            log.error("User Service'e ulasilamadi: {}", e.getMessage());
            throw new ServiceUnavailableException("user-service",
                    "Kimlik dogrulanamadi: User Service'e ulasilamiyor", e);
        }
    }
}
