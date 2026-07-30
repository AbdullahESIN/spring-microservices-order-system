package com.mikroservis.order;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_order")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Dikkat: Kullanici ve urun bilgilerini iliski (foreign key) ile degil,
     * sadece id + o anki degerlerin kopyasi olarak tutuyoruz. Mikroserviste
     * her servisin kendi veritabani vardir; tablolar arasi JOIN yapilamaz.
     */
    private Long userId;
    private String username;

    private Long productId;
    private String productName;

    private int quantity;
    private double totalPrice;

    private String status;

    private LocalDateTime createdAt;
}
