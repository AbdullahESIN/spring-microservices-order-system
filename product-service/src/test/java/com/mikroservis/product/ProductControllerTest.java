package com.mikroservis.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST uclarinin gercekten calistigini dogrulayan testler.
 * MockMvc sayesinde sunucuyu ayaga kaldirmadan HTTP isteklerini simule ederiz.
 * Veritabani olarak (src/test/resources/application.properties sayesinde) H2 kullanilir.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void temizle() {
        productRepository.deleteAll();
    }

    @Test
    void urun_olusturulabilir_ve_geri_okunabilir() throws Exception {
        Product yeni = new Product(null, "Mouse", "Kablosuz", 750.0, 20);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(yeni)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mouse"))
                .andExpect(jsonPath("$.stockQuantity").value(20));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void olmayan_urun_404_doner() throws Exception {
        mockMvc.perform(get("/api/products/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void gecersiz_urun_400_doner() throws Exception {
        // name alani bos -> @NotBlank dogrulamasi devreye girmeli
        Product gecersiz = new Product(null, "", "aciklama", 100.0, 5);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gecersiz)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void stok_dusurulur_ve_yetersizse_409_doner() throws Exception {
        Product kayitli = productRepository.save(new Product(null, "Klavye", "Mekanik", 1500.0, 10));

        // 10 stoktan 3 dusur -> 7 kalmali
        mockMvc.perform(post("/api/products/{id}/reduce-stock", kayitli.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(7));

        // kalan 7'den 99 dusurmeye calis -> 409 CONFLICT
        mockMvc.perform(post("/api/products/{id}/reduce-stock", kayitli.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":99}"))
                .andExpect(status().isConflict());
    }
}
