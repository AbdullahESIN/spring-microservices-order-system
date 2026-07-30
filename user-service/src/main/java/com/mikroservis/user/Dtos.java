package com.mikroservis.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Servisin disariya actigi istek/yanit modelleri (DTO).
 * Entity'yi dogrudan disariya vermiyoruz ki parola alani hic sizmasin.
 */
public class Dtos {

    public record RegisterRequest(
            @NotBlank String username,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6) String password) {
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password) {
    }

    public record AuthResponse(String token, Long userId, String username) {
    }

    public record UserResponse(Long id, String username, String email) {
    }
}
