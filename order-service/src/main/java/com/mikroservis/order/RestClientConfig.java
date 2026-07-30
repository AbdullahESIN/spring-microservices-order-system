package com.mikroservis.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Her hedef servis icin ayri bir RestClient tanimliyoruz.
 * Timeout vermek onemli: karsi servis yavaslarsa/askida kalirsa
 * bizim servisimiz de sonsuza kadar beklemesin.
 */
@Configuration
public class RestClientConfig {

    private RestClient build(String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    @Bean
    public RestClient userServiceClient(@Value("${services.user.url}") String baseUrl) {
        return build(baseUrl);
    }

    @Bean
    public RestClient productServiceClient(@Value("${services.product.url}") String baseUrl) {
        return build(baseUrl);
    }

    @Bean
    public RestClient notificationServiceClient(@Value("${services.notification.url}") String baseUrl) {
        return build(baseUrl);
    }
}
