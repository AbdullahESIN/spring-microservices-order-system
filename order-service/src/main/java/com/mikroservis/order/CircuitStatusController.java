package com.mikroservis.order;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/**
 * Devre kesicilerin anlik durumunu web arayuzune tasir.
 *
 * Actuator zaten /actuator/circuitbreakers ucunu sunuyor, ama o Gateway'in
 * /api/** yonlendirmesinin disinda kalir. Burasi mevcut route uzerinden
 * erisilebilen sade bir ozet verir.
 */
@RestController
@RequestMapping("/api/orders/circuits")
public class CircuitStatusController {

    private final CircuitBreakerRegistry registry;

    public CircuitStatusController(CircuitBreakerRegistry registry) {
        this.registry = registry;
    }

    /**
     * state: CLOSED (normal) | OPEN (istekler durduruldu) | HALF_OPEN (deneme izni)
     */
    public record CircuitStatus(
            String name,
            String state,
            float failureRate,
            int bufferedCalls,
            int failedCalls,
            long notPermittedCalls) {
    }

    @GetMapping
    public List<CircuitStatus> getAll() {
        return registry.getAllCircuitBreakers().stream()
                .map(cb -> {
                    CircuitBreaker.Metrics m = cb.getMetrics();
                    return new CircuitStatus(
                            cb.getName(),
                            cb.getState().name(),
                            m.getFailureRate(),
                            m.getNumberOfBufferedCalls(),
                            m.getNumberOfFailedCalls(),
                            m.getNumberOfNotPermittedCalls());
                })
                .sorted(Comparator.comparing(CircuitStatus::name))
                .toList();
    }
}
