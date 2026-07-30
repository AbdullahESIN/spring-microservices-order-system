package com.mikroservis.order.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
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
     * Token'i User Service'e dogrulatir.
     * Bu senkron (blocking) bir servisler-arasi cagri: cevap gelmeden siparis olusturamayiz.
     */
    public UserResponse validateToken(String authorizationHeader) {
        try {
            return client.get()
                    .uri("/api/auth/validate")
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(UserResponse.class);
        } catch (RestClientException e) {
            log.error("User Service dogrulama cagrisi basarisiz: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Kimlik dogrulanamadi (User Service'e ulasilamadi veya token gecersiz)");
        }
    }
}
