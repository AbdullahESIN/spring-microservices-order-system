package com.mikroservis.order;

import com.mikroservis.order.clients.NotificationClient;
import com.mikroservis.order.clients.ProductClient;
import com.mikroservis.order.clients.ServiceUnavailableException;
import com.mikroservis.order.clients.UserClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Siparis akisinin testleri.
 *
 * Diger servislere gercek HTTP cagrisi yapmiyoruz; uc client'i taklit (mock)
 * ediyoruz. Bu sayede testler CI'da hicbir servis ayakta olmadan calisir --
 * mikroserviste birim testinin nasil yazilmasi gerektiginin ta kendisi budur.
 */
@SpringBootTest
class OrderServiceTest {

    private static final String TOKEN = "Bearer test-token";

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private UserClient userClient;

    @MockitoBean
    private ProductClient productClient;

    @MockitoBean
    private NotificationClient notificationClient;

    private final UserClient.UserResponse user =
            new UserClient.UserResponse(1L, "abdullah", "abdullah@example.com");

    private final ProductClient.ProductResponse product =
            new ProductClient.ProductResponse(5L, "Klavye", "Mekanik", 1500.0, 10);

    @BeforeEach
    void hazirla() {
        orderRepository.deleteAll();
        when(userClient.validateToken(anyString())).thenReturn(user);
        when(productClient.getProduct(anyLong())).thenReturn(product);
        when(productClient.reduceStock(anyLong(), anyInt())).thenReturn(product);
    }

    @Test
    void siparis_olusur_ve_ucu_servis_de_dogru_sirayla_cagrilir() {
        Order order = orderService.createOrder(TOKEN, 5L, 3);

        assertThat(order.getId()).isNotNull();
        assertThat(order.getUsername()).isEqualTo("abdullah");
        assertThat(order.getProductName()).isEqualTo("Klavye");
        assertThat(order.getTotalPrice()).isEqualTo(4500.0);   // 1500 x 3
        assertThat(order.getStatus()).isEqualTo("CONFIRMED");

        // Sira onemli: once kimlik, sonra urun, sonra stok, en son bildirim
        InOrder sira = inOrder(userClient, productClient, notificationClient);
        sira.verify(userClient).validateToken(TOKEN);
        sira.verify(productClient).getProduct(5L);
        sira.verify(productClient).reduceStock(5L, 3);
        sira.verify(notificationClient).sendOrderConfirmation(
                eq("abdullah@example.com"), anyLong(), eq("Klavye"), eq(3), eq(4500.0));
    }

    @Test
    void bildirim_servisi_cokse_bile_siparis_gecerli_kalir() {
        // Notification Service ulasilamaz durumda
        doThrow(new ServiceUnavailableException("notification-service", "ulasilamiyor", null))
                .when(notificationClient)
                .sendOrderConfirmation(anyString(), anyLong(), anyString(), anyInt(), anyDouble());

        // Yine de siparis olusmali: bildirim kritik olmayan bir yan islem
        Order order = orderService.createOrder(TOKEN, 5L, 1);

        assertThat(order.getId()).isNotNull();
        assertThat(orderRepository.findAll()).hasSize(1);
    }

    @Test
    void stok_yetersizse_siparis_KAYDEDILMEZ() {
        when(productClient.reduceStock(anyLong(), anyInt()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Yetersiz stok"));

        assertThatThrownBy(() -> orderService.createOrder(TOKEN, 5L, 999))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Yetersiz stok");

        // Kritik dogrulama: veritabanina yarim kalmis bir siparis yazilmamali
        assertThat(orderRepository.findAll()).isEmpty();
        verify(notificationClient, never())
                .sendOrderConfirmation(anyString(), anyLong(), anyString(), anyInt(), anyDouble());
    }

    @Test
    void token_gecersizse_urun_servisine_hic_gidilmez() {
        when(userClient.validateToken(anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token gecersiz"));

        assertThatThrownBy(() -> orderService.createOrder(TOKEN, 5L, 1))
                .isInstanceOf(ResponseStatusException.class);

        // Hizli basarisizlik: kimlik dogrulanamadiysa bosuna baska servis yorulmaz
        verifyNoInteractions(productClient);
        verifyNoInteractions(notificationClient);
        assertThat(orderRepository.findAll()).isEmpty();
    }

    @Test
    void kullanicinin_kendi_siparisleri_filtrelenir() {
        orderService.createOrder(TOKEN, 5L, 1);
        orderService.createOrder(TOKEN, 5L, 2);

        List<Order> benimkiler = orderService.findMyOrders(TOKEN);

        assertThat(benimkiler).hasSize(2);
        assertThat(benimkiler).allMatch(o -> o.getUserId().equals(1L));
    }
}
