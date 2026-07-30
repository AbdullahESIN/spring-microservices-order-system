package com.mikroservis.product;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<Product> getAll() {
        return productRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody Product product) {
        return productRepository.save(product);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @Valid @RequestBody Product updated) {
        return productRepository.findById(id)
                .map(existing -> {
                    updated.setId(existing.getId());
                    return ResponseEntity.ok(productRepository.save(updated));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Stok dusurme ucu. Order Service siparis olustururken bu endpoint'i cagirir.
     * Stok yetersizse 409 CONFLICT doner ki siparis olusmasin.
     */
    @PostMapping("/{id}/reduce-stock")
    public Product reduceStock(@PathVariable Long id, @RequestBody StockRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Urun bulunamadi: " + id));

        if (request.quantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adet 0'dan buyuk olmali");
        }
        if (product.getStockQuantity() < request.quantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Yetersiz stok. Mevcut: " + product.getStockQuantity() + ", istenen: " + request.quantity());
        }

        product.setStockQuantity(product.getStockQuantity() - request.quantity());
        return productRepository.save(product);
    }

    public record StockRequest(int quantity) {
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
