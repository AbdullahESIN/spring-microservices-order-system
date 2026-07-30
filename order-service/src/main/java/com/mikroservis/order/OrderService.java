package com.mikroservis.order;

import com.mikroservis.order.clients.NotificationClient;
import com.mikroservis.order.clients.ProductClient;
import com.mikroservis.order.clients.UserClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Siparis olusturma akisinin tamami burada. Bu sinif projedeki
 * "mikroservisler birbiriyle nasil konusur" sorusunun cevabi:
 *
 *   1) User Service    -> token gecerli mi?          (REST GET)
 *   2) Product Service -> urun var mi, fiyati ne?    (REST GET)
 *   3) Product Service -> stogu dusur                (REST POST)
 *   4) kendi veritabanina siparisi kaydet
 *   5) Notification Service -> bildirim gonder       (REST POST, kritik degil)
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final UserClient userClient;
    private final ProductClient productClient;
    private final NotificationClient notificationClient;

    public OrderService(OrderRepository orderRepository,
                        UserClient userClient,
                        ProductClient productClient,
                        NotificationClient notificationClient) {
        this.orderRepository = orderRepository;
        this.userClient = userClient;
        this.productClient = productClient;
        this.notificationClient = notificationClient;
    }

    public Order createOrder(String authorizationHeader, Long productId, int quantity) {
        // 1) Kimlik dogrulama
        UserClient.UserResponse user = userClient.validateToken(authorizationHeader);
        log.info("Siparis istegi: kullanici={} urun={} adet={}", user.username(), productId, quantity);

        // 2) Urun bilgisi
        ProductClient.ProductResponse product = productClient.getProduct(productId);

        // 3) Stok dusurme (yetersizse burada 409 firlar ve siparis olusmaz)
        productClient.reduceStock(productId, quantity);

        // 4) Siparisi kendi veritabanimiza kaydet
        double total = product.price() * quantity;
        Order order = new Order(
                null,
                user.id(),
                user.username(),
                product.id(),
                product.name(),
                quantity,
                total,
                "CONFIRMED",
                LocalDateTime.now());

        Order saved = orderRepository.save(order);
        log.info("Siparis olusturuldu: id={} toplam={}", saved.getId(), total);

        // 5) Bildirim (basarisiz olsa da siparis gecerli kalir)
        //
        // Buradaki try-catch, Resilience4j'nin fallback'i VARKEN bile bilincli olarak durur.
        // Sebep: "bildirim hatasi siparisi bozmaz" bir IS KURALIDIR; bir anotasyonun
        // dogru yapilandirilmis olmasina bagli birakilamaz. Resilience4j'nin katkisi
        // farkli: cokmus servise istek atmayi tamamen keserek timeout beklemeyi onler.
        try {
            notificationClient.sendOrderConfirmation(
                    user.email(), saved.getId(), product.name(), quantity, total);
        } catch (Exception e) {
            log.warn("Bildirim adimi basarisiz oldu, siparis {} yine de gecerli: {}",
                    saved.getId(), e.getMessage());
        }

        return saved;
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public List<Order> findMyOrders(String authorizationHeader) {
        UserClient.UserResponse user = userClient.validateToken(authorizationHeader);
        return orderRepository.findByUserId(user.id());
    }
}
