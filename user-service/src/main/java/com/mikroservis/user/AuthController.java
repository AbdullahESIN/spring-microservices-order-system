package com.mikroservis.user;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Dtos.UserResponse register(@Valid @RequestBody Dtos.RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu kullanici adi zaten alinmis");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu e-posta zaten kayitli");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));

        User saved = userRepository.save(user);
        return new Dtos.UserResponse(saved.getId(), saved.getUsername(), saved.getEmail());
    }

    @PostMapping("/login")
    public Dtos.AuthResponse login(@Valid @RequestBody Dtos.LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Kullanici adi veya parola hatali"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Kullanici adi veya parola hatali");
        }

        String token = jwtService.generateToken(user);
        return new Dtos.AuthResponse(token, user.getId(), user.getUsername());
    }

    /**
     * Servisler arasi dogrulama ucu: Order Service, elindeki token'in gecerli olup
     * olmadigini bu endpoint'e REST cagrisi yaparak ogrenir.
     */
    @GetMapping("/validate")
    public ResponseEntity<Dtos.UserResponse> validate(@RequestHeader("Authorization") String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization header eksik veya hatali");
        }

        String token = authorization.substring("Bearer ".length());
        Long userId;
        try {
            userId = jwtService.parseToken(token).get("userId", Long.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token gecersiz veya suresi dolmus");
        }

        return userRepository.findById(userId)
                .map(u -> ResponseEntity.ok(new Dtos.UserResponse(u.getId(), u.getUsername(), u.getEmail())))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Kullanici bulunamadi"));
    }
}
