# Mikroservis Siparis Yonetim Sistemi

[![CI](https://github.com/AbdullahESIN/spring-microservices-order-system/actions/workflows/ci.yml/badge.svg)](https://github.com/AbdullahESIN/spring-microservices-order-system/actions/workflows/ci.yml)
[![Docker Image Yayinla](https://github.com/AbdullahESIN/spring-microservices-order-system/actions/workflows/docker-publish.yml/badge.svg)](https://github.com/AbdullahESIN/spring-microservices-order-system/actions/workflows/docker-publish.yml)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-brightgreen)](https://spring.io/projects/spring-boot)

Spring Boot ile yazilmis, servisler arasi REST iletisimi ve CI/CD pratigi icin gelistirilmis
mikroservis mimarisi ornegi.

## Mimari

```
                        ┌──────────────────┐
   istemci  ───────────▶│   API Gateway    │  :8080   (disariya acilan tek kapi)
                        └────────┬─────────┘
                                 │
          ┌──────────────┬───────┴───────┬──────────────────┐
          ▼              ▼               ▼                  ▼
   ┌────────────┐ ┌────────────┐  ┌────────────┐   ┌──────────────────┐
   │  User      │ │  Product   │  │  Order     │   │  Notification    │
   │  Service   │ │  Service   │  │  Service   │   │  Service         │
   │  :8082     │ │  :8081     │  │  :8083     │   │  :8084           │
   └─────┬──────┘ └─────┬──────┘  └─────┬──────┘   └────────┬─────────┘
         │              │               │                   │
      ┌──▼───┐      ┌───▼───┐       ┌───▼────┐          (veritabani yok)
      │userdb│      │productdb│     │orderdb │
      └──────┘      └────────┘      └────────┘

   Order Service siparis olustururken:
     1. User Service'e     ──▶ token gecerli mi?      (GET  /api/auth/validate)
     2. Product Service'e  ──▶ urun ve fiyat bilgisi  (GET  /api/products/{id})
     3. Product Service'e  ──▶ stogu dusur            (POST /api/products/{id}/reduce-stock)
     4. kendi DB'sine siparisi kaydeder
     5. Notification Service'e ──▶ bildirim gonder    (POST /api/notifications)
```

### Neden bu sekilde?

- **Her servisin kendi veritabani var.** Servisler birbirinin tablosuna erisemez, sadece
  REST API uzerinden konusur. Tablolar arasi JOIN yoktur; bu yuzden `Order` entity'si
  kullanici ve urun bilgisinin bir kopyasini tutar.
- **API Gateway tek giris noktasidir.** Docker Compose'da sadece 8080 portu disariya acilir;
  diger servisler yalnizca Docker agi icinden erisilebilir.
- **Bildirim kritik degildir.** Notification Service cevap vermezse siparis yine de gecerli
  kalir (sadece log'a uyari dusulur). Kimlik dogrulama ve stok kontrolu ise kritiktir;
  onlar basarisiz olursa siparis olusmaz.

## Teknolojiler

| Katman | Teknoloji |
|---|---|
| Dil / Runtime | Java 21 (Temurin) |
| Framework | Spring Boot 4.1, Spring Cloud Gateway |
| Veritabani | PostgreSQL 16 (testlerde H2) |
| Kimlik dogrulama | JWT (jjwt), BCrypt |
| Konteyner | Docker, Docker Compose |
| CI/CD | GitHub Actions, GHCR |

## Calistirma

### Secenek 1 — Docker Compose (onerilen)

Tum servisleri ve veritabanlarini tek komutla ayaga kaldirir:

```bash
docker compose up --build
```

Ilk calistirmada imajlar indirilecegi icin biraz surer. Sonrasinda:

```bash
docker compose up -d      # arka planda baslat
docker compose logs -f    # loglari izle
docker compose down       # durdur
docker compose down -v    # durdur + veritabani verilerini sil
```

### Secenek 2 — Yerelde tek tek

Once bir PostgreSQL calistirin:

```bash
docker run --name mikroservis-db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:16
```

Sonra `productdb`, `userdb`, `orderdb` veritabanlarini olusturun ve her servisi ayri
terminalde baslatin:

```bash
cd product-service && ./mvnw spring-boot:run
```

## API Kullanimi

Asagidaki ornekler Gateway (`:8080`) uzerinden gider.

### 1. Kullanici kaydi

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"abdullah","email":"abdullah@example.com","password":"parola123"}'
```

### 2. Giris yap ve token al

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"abdullah","password":"parola123"}'
```

Donen `token` degerini asagidaki isteklerde kullanin.

### 3. Urun olustur

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Mekanik Klavye","description":"RGB switch","price":1500,"stockQuantity":10}'
```

### 4. Siparis olustur (token gerekli)

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"productId":1,"quantity":3}'
```

### Tum uclar

| Metot | Yol | Aciklama | Token |
|---|---|---|---|
| POST | `/api/auth/register` | Kullanici kaydi | – |
| POST | `/api/auth/login` | Giris, JWT doner | – |
| GET | `/api/auth/validate` | Token dogrulama (servisler arasi) | ✔ |
| GET | `/api/products` | Tum urunler | – |
| GET | `/api/products/{id}` | Tek urun | – |
| POST | `/api/products` | Urun ekle | – |
| PUT | `/api/products/{id}` | Urun guncelle | – |
| DELETE | `/api/products/{id}` | Urun sil | – |
| POST | `/api/products/{id}/reduce-stock` | Stok dusur (servisler arasi) | – |
| POST | `/api/orders` | Siparis olustur | ✔ |
| GET | `/api/orders` | Tum siparisler | – |
| GET | `/api/orders/me` | Kendi siparislerim | ✔ |
| GET | `/api/orders/{id}` | Tek siparis | – |
| GET | `/api/notifications` | Gonderilen bildirimler | – |

### Hata kodlari

| Durum | Kod |
|---|---|
| Token gecersiz / eksik | 401 |
| Urun bulunamadi | 404 |
| Yetersiz stok | 409 |
| Kullanici adi/e-posta zaten kayitli | 409 |
| Product Service'e ulasilamiyor | 502 |

## Testler

```bash
cd product-service && ./mvnw verify
```

Testler H2 bellek-ici veritabani kullanir; calistirmak icin PostgreSQL gerekmez.

## CI/CD

`.github/workflows/` altinda iki pipeline var:

**`ci.yml`** — her push ve pull request'te calisir:
- 5 servisi **paralel** olarak derler (matrix stratejisi)
- testleri calistirir
- test raporlarini artifact olarak yukler

**`docker-publish.yml`** — sadece `main` dalina push olunca calisir:
- her servisin Docker imajini olusturur
- GitHub Container Registry'ye (`ghcr.io`) gonderir
- `latest` ve commit SHA'si ile etiketler

## Yol haritasi

- [x] 4 mikroservis + API Gateway
- [x] Servisler arasi senkron REST iletisimi
- [x] JWT ile kimlik dogrulama
- [x] Docker Compose ile tek komutla ayaga kalkma
- [x] GitHub Actions ile CI/CD
- [ ] RabbitMQ ile asenkron bildirim (sync vs async farkini gormek icin)
- [ ] Resilience4j ile circuit breaker ve retry
- [ ] Service discovery (Eureka / Consul)
- [ ] Prometheus + Grafana ile izleme
- [ ] Kubernetes'e deploy
