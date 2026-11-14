# 00 — Mimari ve Ortak Kurallar

Bu doküman tüm modüller için bağlayıcıdır. Modül planları burayı tekrar etmez; yalnızca buraya atıf yapar.

---

## §1 Mimari özeti

```
[Tarayıcı / PWA] ──HTTPS──> [nginx] ──/api, /ws──> [backend: Spring Boot :8080] ──> [PostgreSQL 18]
                                   └──/ (diğer)──> [frontend: Next.js :3000]      └──> [Disk: /data/files]
                                                                                   └──> [Okul SMTP] [Push servisleri]
```

- **Modüler monolit.** Tek backend uygulaması vardır; modüller paket olarak ayrılır. Mikroservis, kuyruk veya Redis yoktur.
- **Tek sunucu.** WebSocket broker'ı bellek içi çalışır (Spring simple broker). Zamanlanmış işler tek örnekte çalışır; ShedLock gerekmez.
- **Modüller birbirini doğrudan çağırmaz.** Çekirdek şu arayüzleri ve olayları yayınlar (`01-cekirdek.md` §8):
  - `UserDeactivationHook`
  - `SeasonActivationHook`
  - `BadgeProvider`
  - Grup olayları

  Modüller bunları uygular veya dinler. Bir modül başka bir modülün paketindeki sınıfı import edemez; istisnası `common` ve `core` paketleridir.

---

## §2 Sürümler

| Bileşen | Sürüm | Not |
|---|---|---|
| Java | 25 (LTS) | Temurin |
| Spring Boot | 4.x'in en güncel GA sürümü | Yazım tarihinde 4.1.x. 4.2 GA çıkmışsa 4.2.x kullanılır. |
| PostgreSQL | 18 | Docker imajı `postgres:18` (Debian tabanlı; ICU collation içerir) |
| Node.js | 24 LTS | |
| Next.js | 16.x'in en güncel sürümü | Yazım tarihinde 16.4.x |
| React | Next 16 ile gelen sürüm | |
| Tailwind CSS | 4.x | create-next-app kurar |
| Maven | Wrapper (`./mvnw`) | |

Spring Boot 4 ile gelen dikkat edilecek değişiklikler:

- **Jackson 3 kullanılır.** Paket adı `tools.jackson.databind.*` olur. Anotasyonların paketi değişmez: `com.fasterxml.jackson.annotation.*`.
- **Starter adları modülerleşti.** Bağımlılıklar start.spring.io'dan üretilir ve elle yeniden adlandırılmaz.
- **Testcontainers 2 kullanılır.** Artifact ve paket adları start.spring.io'nun ürettiği haliyle kalır.

---

## §3 Repo yapısı ve iskelet kurulumu

```
/ (repo kökü)
├── CLAUDE.md  PROGRESS.md  .gitignore  .env.example
├── .claude/ (settings.json, agents/, skills/)
├── docs/ (KARARLAR.md, plan/)
├── backend/      Spring Boot (Maven)
├── frontend/     Next.js
└── deploy/       docker-compose.yml, docker-compose.dev.yml, nginx/, backup.sh
```

### Backend iskeleti (K-01)

İskelet start.spring.io'dan indirilir:

```bash
curl -s https://start.spring.io/starter.zip \
  -d type=maven-project -d language=java -d javaVersion=25 \
  -d groupId=tr.okul -d artifactId=iletisim -d name=iletisim \
  -d packageName=tr.okul.iletisim \
  -d dependencies=web,security,data-jpa,data-jdbc,postgresql,flyway,validation,websocket,mail,session-jdbc,lombok,testcontainers \
  -o backend.zip && unzip -q backend.zip -d backend && rm backend.zip
```

- start.spring.io `bootVersion` parametresi olmadan en güncel GA sürümü verir.
- `data-jdbc` yalnızca `JdbcClient` için eklenir; Spring Data JDBC repository'si **kullanılmaz**.

`pom.xml` dosyasına elle eklenecek bağımlılıklar:

| Bağımlılık | Amaç |
|---|---|
| `org.apache.pdfbox:pdfbox:3.0.x` | PDF sayfa sayısını okumak (Fotokopi) |
| `org.apache.tika:tika-core:3.x` | Dosya içeriğinden tip tespiti |
| `nl.martijndwars:web-push:5.1.2` | Web Push gönderimi |
| `org.bouncycastle:bcprov-jdk18on` (güncel) | web-push kütüphanesi için şifreleme |
| `org.flywaydb:flyway-database-postgresql` | start.spring.io eklemediyse eklenir |

### Frontend iskeleti (K-02)

```bash
npx create-next-app@latest frontend --ts --tailwind --eslint --app --src-dir --import-alias "@/*" --use-npm --yes
cd frontend
npm i @tanstack/react-query react-hook-form zod @hookform/resolvers sonner lucide-react @stomp/stompjs date-fns browser-image-compression
npx shadcn@latest init -d
npx shadcn@latest add -y button input textarea label select checkbox switch radio-group dialog drawer sheet dropdown-menu popover command tooltip badge avatar card tabs table skeleton separator scroll-area alert alert-dialog sonner progress
```

`package.json` dosyasındaki script'lere şunlar eklenir:

- `"typecheck": "tsc --noEmit"`
- `"lint": "eslint"` (create-next-app ekler)

---

## §4 Backend kod kuralları

### 4.1 Paket yapısı

```
tr.okul.iletisim
├── IletisimApplication.java
├── common/
│   ├── error/        ApiError, ErrorCode, CommonErrorCode, BusinessException, GlobalExceptionHandler, ErrorResponseWriter, Messages
│   ├── web/          PageResponse, CursorResponse, TraceIdFilter, UserContextArgumentResolver, WebConfig
│   ├── security/     SecurityConfig, UserContext, UserContextFilter, SessionService, SeasonGuard
│   ├── file/         StoredFile, StoredFileRepository, FileStorageService, FilePurpose, FileController
│   ├── notify/       Notification…, PushSubscription…, NotificationService, WebPushSender, WebSocketConfig, EventPublisher(ws)
│   ├── audit/        AuditLog, AuditAction(interface), AuditService, AuditController
│   ├── mail/         MailService
│   └── util/         TextNormalizer, PhoneNormalizer, Csv
├── core/             auth/, user/, branch/, season/, group/, directory/, admin/, job/
├── feed/             (Akış)
├── print/            (Fotokopi)
├── chat/             (Mesajlaşma)
├── dms/              (Dokümanlar)
└── calendar/         (Takvim)
```

Her özellik paketi düz bir yapıdadır; alt paket açılmaz. Örnek (`core/season/`):

- `Season.java`: entity
- `SeasonStatus.java`: enum
- `SeasonRepository.java`
- `SeasonService.java`: iş kuralları, `@Transactional`
- `SeasonQueries.java`: JdbcClient ile okuma sorguları (yalnızca gerekirse)
- `SeasonController.java`
- `SeasonDtos.java`: tüm istek ve yanıt kayıtları, `public record` olarak tek dosyada
- `SeasonErrorCode.java`: modülün hata kodları

### 4.2 Entity kuralları

- Lombok kullanılır: `@Entity @Table(name="...") @Getter @Setter`. `@Data` kullanılmaz.
- Kimlik alanı: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;`. Tek istisna `stored_file` tablosudur; orada UUID kullanılır.
- **JPA ilişkileri kullanılmaz.** `@ManyToOne`, `@OneToMany` ve benzerleri yoktur. Yabancı anahtarlar `Long xxxId` alanı olarak tutulur. Bu sayede N+1 sorgu ve lazy-loading hataları oluşmaz.
- Enum alanları `@Enumerated(EnumType.STRING)` ile tutulur. Veritabanında `varchar` kolon ve `CHECK` kısıtı bulunur.
- Zaman alanları `OffsetDateTime` (kolon tipi `timestamptz`), tarih alanları `LocalDate` tipindedir. `Instant` ve `LocalDateTime` kullanılmaz.
- `created_at` ve `updated_at` için `@CreationTimestamp` ve `@UpdateTimestamp` kullanılır.
- Şema yalnızca Flyway ile değişir. Ayar `spring.jpa.hibernate.ddl-auto=validate` olarak kalır.

Flyway dosya adları önceden atanmıştır. Paralel çalışmada çakışma olmaması için değiştirilmez:

| Dosya | Modül |
|---|---|
| `V1__core.sql` | Çekirdek |
| `V2__feed.sql` | Akış |
| `V3__print.sql` | Fotokopi |
| `V4__chat.sql` | Mesajlaşma |
| `V5__dms.sql` | Dokümanlar |
| `V6__calendar.sql` | Takvim |

Bir modülde sonradan değişiklik gerekirse yeni dosya açılır: `V{modül}_{n}__aciklama.sql`. Örnek: `V3_1__print_add_x.sql`.

### 4.3 Okuma ve yazma ayrımı

- **Yazma** işlemleri JPA entity ve Spring Data repository ile yapılır.
- **Liste ve detay okumaları** birden çok tabloyu birleştiriyorsa `JdbcClient` ile düz SQL yazılır. Sonuç bir `record` tipine eşlenir:
  `jdbc.sql("...").param("x", x).query(Row.class).list()`
  - Kolon adları `snake_case`, record alanları `camelCase` olur; eşleme otomatik yapılır.
  - Zaman kolonları record içinde `OffsetDateTime` tipindedir.
- Tek tabloya yapılan basit okumalarda repository metotları kullanılır.
- Metin araması için `TextNormalizer.fold(q)` çağrılır ve `search_key LIKE :q` koşulu yazılır. Parametre `%` + fold + `%` olarak verilir; `%` ve `_` karakterleri kaçışlanır.
- Türkçe alfabetik sıralama için `ORDER BY last_name COLLATE "tr-x-icu", first_name COLLATE "tr-x-icu"` kullanılır.

### 4.4 Servis ve controller kuralları

- Controller metodu oturum açmış kullanıcıyı `UserContext me` parametresiyle alır; parametreyi resolver doldurur. Controller yalnızca çağırır, iş kuralı içermez.
- Servisin public metotları `UserContext me` değerini ilk parametre olarak alır. Event listener ve zamanlanmış işlerde kullanıcı bilgisi id ile, açık parametre olarak geçirilir.
- Yetki kontrolleri `@PreAuthorize` ile değil, servis içinde açıkça yapılır: `me.requireManager()` gibi. Böylece her yetki hatasının özel bir Türkçe mesajı olur.
- Yazma işlemi yapan metotlar `@Transactional` taşır; okuma metotları `@Transactional(readOnly = true)` taşır.
- **Durum geçişlerinde yarış önlenir.** İlgili entity'ye `@Version` eklenir ya da koşullu bir `UPDATE ... WHERE status = :beklenen` yazılır. Etkilenen satır sayısı 0 ise `CONCURRENT_MODIFICATION` hatası fırlatılır. Hangisinin kullanılacağını modül planı belirtir.
- DTO dönüşümü elle yapılır: record içinde `static XxxResponse from(Entity e)` metodu. MapStruct kullanılmaz.
- Hata kodları ve mesajlar her modülün kendi `XxxErrorCode` enum'unda tutulur.
- Mesajlardaki parametreler `{0}`, `{1}` biçimindedir ve `Messages.format()` ile doldurulur. **`MessageFormat` kullanılmaz**, çünkü Türkçe kesme işareti (') onu bozar.

### 4.5 Olaylar

- Modüller arası haberleşme Spring `ApplicationEventPublisher` ile, senkron ve aynı transaction içinde yapılır. Dinleyiciler `@EventListener` kullanır.
- WebSocket ve push gönderimi **transaction commit edildikten sonra** yapılır. `NotificationService` bunu kendisi halleder (§10).

### 4.6 Konfigürasyon

- Tüm ayarlar `application.yml` dosyasında `app.*` altında durur (§16). Gizli bilgiler ortam değişkeninden gelir.
- Ayarlar tek bir `AppProperties` sınıfında toplanır: `@ConfigurationProperties("app")` ve iç içe record'lar.

---

## §5 API kuralları

- Tüm uç noktalar `/api` altındadır. Yönetim uçları `/api/admin/**` altındadır.
- JSON alan adları `camelCase` yazılır.
- Zaman değerleri ISO-8601 formatında ve ofsetli gönderilir (`2026-11-14T14:20:00Z`). Tarih değerleri `2026-09-01` biçimindedir.
- Kimlikler sayı olarak döner. Tek istisna dosya kimliğidir; o UUID string'dir.
- HTTP metotları:

| Metot | Kullanım |
|---|---|
| `GET` | Okuma |
| `POST` | Oluşturma ve eylemler (`/{id}/activate` gibi) |
| `PUT` | Tam güncelleme |
| `DELETE` | Silme |

  `PATCH` kullanılmaz.
- Yanıt kodları:

| Kod | Ne zaman |
|---|---|
| 200 | Başarılı, gövdeli yanıt |
| 201 | Oluşturuldu; gövdede oluşturulan kayıt döner |
| 204 | Başarılı, gövdesiz yanıt |
| 4xx / 5xx | Hata; gövde her zaman `ApiError` formatındadır (§6) |

- **Sayfalı listeler.** Parametreler `?page=0&size=25` biçimindedir; `size` en fazla 100'dür, aşılırsa 100'e indirilir. Yanıt:
  `PageResponse<T> { items: T[], page: number, size: number, total: number }`
- **Kaydırmalı listeler** (akış, mesajlar, bildirimler). Parametreler `?before={id}&size=20` biçimindedir. Yanıt:
  `CursorResponse<T> { items: T[], nextCursor: number | null }`
- **Form alan adları DTO alan adlarıyla birebir aynıdır.** Bu sayede backend'den dönen alan hataları doğrudan forma bağlanır.

---

## §6 Hata yönetimi (KRİTİK)

Amaç: kullanıcı hiçbir durumda "bir hata oluştu" deyip bırakan bir mesaj görmez. Her hata şunları söyler:

1. Ne oldu
2. Neden oldu, biliniyorsa
3. Kullanıcının ne yapabileceği

### 6.1 Hata yanıt formatı

```json
{
  "code": "PRINT_COPIES_RANGE",
  "message": "Kopya adedi 1 ile 500 arasında olmalıdır.",
  "fieldErrors": [{ "field": "copies", "message": "Kopya adedi 1 ile 500 arasında olmalıdır." }],
  "details": ["3. satır: E-posta adresi geçersiz."],
  "traceId": "a1b2c3d4"
}
```

- `fieldErrors` ve `details` alanları boşsa yanıtta yer almaz (`@JsonInclude(NON_EMPTY)`).
- `traceId`, her istek için `TraceIdFilter` tarafından üretilir: 8 karakterlik hex. Bu değer MDC'ye ve `X-Trace-Id` yanıt başlığına yazılır, loglarda da görünür.

### 6.2 Backend bileşenleri

```java
public interface ErrorCode { String name(); HttpStatus status(); String message(); }

public class BusinessException extends RuntimeException {
  // new BusinessException(code, args...)                        -> genel hata
  // BusinessException.field(code, "email", args...)             -> alan hatası (fieldErrors'a da eklenir)
  // BusinessException.details(code, List<String> details, args) -> satır/madde listesi
}
```

`GlobalExceptionHandler` (`@RestControllerAdvice`) aşağıdaki eşlemeyi **eksiksiz** uygular:

| Yakalanan | HTTP | code | message |
|---|---|---|---|
| `BusinessException` | koddan gelir | koddan gelir | koddan gelir (parametreler doldurulmuş) |
| `MethodArgumentNotValidException`, `HandlerMethodValidationException`, `ConstraintViolationException` | 400 | `VALIDATION_FAILED` | "Formda düzeltilmesi gereken alanlar var." + her alan için `fieldErrors` |
| `HttpMessageNotReadableException` | 400 | `REQUEST_UNREADABLE` | "Gönderilen bilgiler okunamadı. Sayfayı yenileyip tekrar deneyin." |
| `MethodArgumentTypeMismatchException`, `MissingServletRequestParameterException` | 400 | `REQUEST_INVALID_PARAM` | "Geçersiz istek parametresi: {0}." |
| `MissingServletRequestPartException` | 400 | `FILE_MISSING` | "Dosya seçilmedi." |
| `MaxUploadSizeExceededException` | 413 | `FILE_TOO_LARGE` | "Dosya boyutu en fazla {0} MB olabilir." |
| `ObjectOptimisticLockingFailureException` | 409 | `CONCURRENT_MODIFICATION` | "Bu kayıt siz işlem yaparken başka biri tarafından değiştirildi. Sayfayı yenileyip tekrar deneyin." |
| `DataIntegrityViolationException`, unique ihlali (SQLState 23505) | 409 | `DUPLICATE_RECORD` | "Bu bilgilerle kayıtlı başka bir kayıt var." |
| `DataIntegrityViolationException`, FK ihlali (23503) | 409 | `RECORD_IN_USE` | "Bu kayıt başka kayıtlarla ilişkili olduğu için işlem yapılamıyor." |
| `NoResourceFoundException`, `HttpRequestMethodNotSupportedException` | 404 / 405 | `ROUTE_NOT_FOUND` | "İstenen adres bulunamadı." |
| `CannotGetJdbcConnectionException`, `DataAccessResourceFailureException` | 503 | `SERVICE_UNAVAILABLE` | "Sistem şu anda yanıt veremiyor. Birkaç dakika sonra tekrar deneyin." |
| `AccessDeniedException` | 403 | `FORBIDDEN` | "Bu işlem için yetkiniz yok." |
| Diğer her `Exception` | 500 | `INTERNAL_ERROR` | "Beklenmeyen bir hata oluştu. Lütfen tekrar deneyin. Sorun devam ederse sistem yöneticisine şu kodu iletin: {traceId}" |

Ek kurallar:

- **Unique ve FK ihlalleri.** Servisler, bilinen tekillik durumlarını kayıttan **önce** kontrol eder ve alan hatası fırlatır (ör. `USER_EMAIL_TAKEN`). `DataIntegrityViolationException` eşlemesi yalnızca yarış durumları için bir yedektir. Kısıt adı bilinen bir alanla eşleşiyorsa ilgili alan hatası döner. Eşlemeler `GlobalExceptionHandler` içinde bir `Map<String, ErrorCode>` olarak tutulur; modüller kendi kısıt adlarını burada değil, servis içi ön kontrolle ele alır.
- **Loglama.** 500 hataları `log.error` ile stack trace dahil, traceId ile yazılır. 4xx hataları `log.debug` ile yazılır.
- **Filtrelerde oluşan hatalar.** Security entry point, AccessDeniedHandler ve UserContextFilter içindeki hatalar da `ErrorResponseWriter` ile aynı JSON formatında yazılır.
- **Bean Validation mesajları.** Mesajlar anotasyonda doğrudan Türkçe yazılır: `@Size(min = 2, max = 50, message = "Ad 2–50 karakter olmalıdır.")`. Mesajlarda `{`, `}` ve `$` karakterleri **kullanılmaz**.

### 6.3 Ortak hata kodları (`CommonErrorCode`)

| code | HTTP | message |
|---|---|---|
| VALIDATION_FAILED | 400 | Formda düzeltilmesi gereken alanlar var. |
| REQUEST_UNREADABLE | 400 | Gönderilen bilgiler okunamadı. Sayfayı yenileyip tekrar deneyin. |
| REQUEST_INVALID_PARAM | 400 | Geçersiz istek parametresi: {0}. |
| UNAUTHENTICATED | 401 | Oturumunuz sona erdi. Lütfen tekrar giriş yapın. |
| FORBIDDEN | 403 | Bu işlem için yetkiniz yok. |
| CSRF_INVALID | 403 | Güvenlik doğrulaması başarısız oldu. Sayfayı yenileyip tekrar deneyin. |
| MEMBERSHIP_REQUIRED | 403 | Bu bölümü kullanabilmek için bu eğitim-öğretim yılına atanmış olmanız gerekir. |
| KVKK_REQUIRED | 403 | Devam etmek için aydınlatma metnini onaylamanız gerekiyor. |
| ROUTE_NOT_FOUND | 404 | İstenen adres bulunamadı. |
| NOT_FOUND | 404 | Aradığınız kayıt bulunamadı veya silinmiş. |
| CONCURRENT_MODIFICATION | 409 | Bu kayıt siz işlem yaparken başka biri tarafından değiştirildi. Sayfayı yenileyip tekrar deneyin. |
| DUPLICATE_RECORD | 409 | Bu bilgilerle kayıtlı başka bir kayıt var. |
| RECORD_IN_USE | 409 | Bu kayıt başka kayıtlarla ilişkili olduğu için işlem yapılamıyor. |
| SEASON_NONE_ACTIVE | 409 | Şu anda aktif bir eğitim-öğretim yılı yok. Bu işlem yapılamıyor. |
| SEASON_READ_ONLY | 409 | Bu kayıt geçmiş bir eğitim-öğretim yılına ait. Görüntüleyebilirsiniz ancak değiştiremezsiniz. |
| RATE_LIMITED | 429 | Çok fazla deneme yaptınız. Lütfen {0} dakika sonra tekrar deneyin. |
| FILE_MISSING | 400 | Dosya seçilmedi. |
| FILE_EMPTY | 400 | Seçilen dosya boş. Lütfen başka bir dosya seçin. |
| FILE_TOO_LARGE | 413 | Dosya boyutu en fazla {0} MB olabilir. |
| FILE_TYPE_NOT_ALLOWED | 400 | Bu dosya türü yüklenemez. İzin verilen türler: {0}. |
| FILE_CONTENT_MISMATCH | 400 | Dosyanın içeriği uzantısıyla uyuşmuyor. Dosyayı kontrol edip tekrar deneyin. |
| FILE_NOT_FOUND | 404 | Dosya bulunamadı veya silinmiş. |
| FILE_NOT_USABLE | 400 | Bu dosya kullanılamıyor. Dosyayı yeniden yükleyin. |
| FILE_STORAGE_ERROR | 500 | Dosya kaydedilemedi. Lütfen tekrar deneyin; sorun sürerse sistem yöneticisine bildirin. |
| SERVICE_UNAVAILABLE | 503 | Sistem şu anda yanıt veremiyor. Birkaç dakika sonra tekrar deneyin. |
| INTERNAL_ERROR | 500 | Beklenmeyen bir hata oluştu. Lütfen tekrar deneyin. Sorun devam ederse sistem yöneticisine şu kodu iletin: {0} |

### 6.4 Frontend hata akışı

Hata yönetimi `src/lib/api.ts` içindeki tek bir fonksiyonda toplanır:

```ts
export class ApiError extends Error {
  constructor(public status: number, public code: string, message: string,
              public fieldErrors: {field: string; message: string}[] = [],
              public details: string[] = [], public traceId?: string) { super(message); }
}
export async function api<T>(path: string, init?: RequestInit & { json?: unknown }): Promise<T>
```

`api()` fonksiyonunun davranışı:

1. `credentials: 'include'` ile istek atar. `json` verilmişse gövdeyi `JSON.stringify` ile yazar ve `Content-Type: application/json` başlığını ekler.
2. GET dışındaki isteklere `X-XSRF-TOKEN` başlığını ekler. Değeri `XSRF-TOKEN` çerezinden okur. Çerez yoksa önce `GET /api/auth/csrf` çağırır.
3. Ağ hatası (`fetch` reddedilirse) → `ApiError(0, 'NETWORK_ERROR', 'Sunucuya ulaşılamıyor. İnternet bağlantınızı kontrol edip tekrar deneyin.')`
4. Yanıt `ApiError` JSON'ı değilse (nginx 413/502/504 gibi) duruma göre mesaj üretir:

| Durum | Mesaj |
|---|---|
| 413 | "Dosya çok büyük. En fazla {MAX} MB yükleyebilirsiniz." |
| 502, 503, 504 | "Sistem şu anda yanıt vermiyor. Birkaç dakika sonra tekrar deneyin." |
| Diğer | "Beklenmeyen bir yanıt alındı (HTTP {status}). Sayfayı yenileyip tekrar deneyin." |

5. 204 yanıtında `undefined` döner.

Hatalar `src/lib/query-client.ts` içinde global olarak ele alınır:

- **Mutasyon hatası.** `MutationCache.onError` her hatada `toast.error(err.message)` gösterir. Mutasyon `meta: { silentError: true }` taşıyorsa toast gösterilmez.
- **Sorgu hatası.** `QueryCache.onError` aynı mesajı toast ile gösterir ve ekran `ErrorState` bileşenini çizer.
- **Oturum kapatan kodlar.** Kod `UNAUTHENTICATED`, `AUTH_ACCOUNT_PASSIVE`, `AUTH_NO_SEASON_MEMBERSHIP` veya `AUTH_NO_ACTIVE_SEASON` ise:
  1. `queryClient.clear()` çağrılır.
  2. `/giris?neden={code}` adresine gidilir.
  3. Giriş sayfası mesajı bir uyarı kutusunda gösterir.

  Bu kodlar için ayrıca toast gösterilmez.
- **KVKK.** Kod `KVKK_REQUIRED` ise `['me']` sorgusu yeniden çekilir ve KVKK penceresi açılır.

Formlarda hata gösterimi:

- `useApiForm` yardımcısı (`src/lib/form.ts`) gönderim hatasında `err.fieldErrors` listesindeki her alanı `form.setError(field, { message })` ile forma bağlar. Toast yine gösterilir.
- Ekranda ilk hatalı alana odaklanılır.

Başarı geri bildirimi:

- Her mutasyon başarıda kısa bir `toast.success("...")` gösterir. Metinler modül planlarında yazılıdır.

WebSocket bağlantı durumu:

- Bağlantı 5 saniyeden uzun koparsa üst kısımda ince sarı bir şerit çıkar: "Bağlantı yeniden kuruluyor…".
- Bağlantı geri gelince şerit kaybolur ve kritik sorgular (bildirim sayısı, rozetler, açık sohbet) yenilenir.

Toast ayarları (`<Toaster />`, sonner):

| Ayar | Değer |
|---|---|
| Konum, mobil | `top-center` |
| Konum, masaüstü | `bottom-right` |
| Hata süresi | 6 sn |
| Başarı süresi | 3 sn |
| Diğer | `richColors`, `closeButton` |

---

## §7 Ortak validasyon kuralları

Backend ve frontend bu tabloyu birebir uygular. Önce trim yapılır, birden fazla boşluk teke indirilir, sonra kurallar uygulanır.

| Alan | Kural | Hata mesajı |
|---|---|---|
| Ad / Soyad | Zorunlu, 2–50 karakter, regex `^[\p{L}][\p{L} .'-]*$` | "Ad 2–50 karakter olmalı ve yalnızca harf içermelidir." (Soyad için "Soyad …") |
| E-posta | Zorunlu, en fazla 254 karakter, regex `^[^\s@]+@[^\s@]+\.[^\s@]{2,}$`. Kayıttan önce `toLowerCase(Locale.ROOT)` uygulanır. | "Geçerli bir e-posta adresi girin." |
| Telefon | İsteğe bağlı. Rakam dışındaki karakterler silinir. Kabul edilen biçimler: `5XXXXXXXXX` (10 hane), `05XXXXXXXXX` (11 hane), `905XXXXXXXXX` (12 hane). `+905XXXXXXXXX` olarak saklanır. | "Telefon numarası 05XX XXX XX XX biçiminde olmalıdır." |
| Şifre | 8–64 karakter, en az bir harf ve bir rakam | "Şifre en az 8 karakter olmalı, en az bir harf ve bir rakam içermelidir." |
| Unvan | İsteğe bağlı, en fazla 100 karakter | "Unvan en fazla 100 karakter olabilir." |
| Branş | İsteğe bağlı, en fazla 100 karakter | "Branş en fazla 100 karakter olabilir." |
| Ad (şube, grup) | Zorunlu, 2–100 karakter | "Ad 2–100 karakter olmalıdır." |
| Açıklama (genel) | İsteğe bağlı, en fazla 500 karakter | "Açıklama en fazla 500 karakter olabilir." |
| Arama sorgusu `q` | En fazla 100 karakter; daha uzunsa kesilir (hata verilmez) | — |

Türkçe karakter tuzakları:

- **E-posta.** Küçük harfe çevirme her zaman `Locale.ROOT` ile yapılır. `tr` locale'i "I" harfini "ı" yapar ve e-postayı bozar.
- **Arama ve tekillik anahtarı.** `TextNormalizer.fold(s)` şu sırayla çalışır:
  1. `s.toLowerCase(Locale.forLanguageTag("tr"))`
  2. ı→i, ğ→g, ü→u, ş→s, ö→o, ç→c, â→a, î→i, û→u dönüşümleri
  3. NFD normalizasyonu ve `\p{M}` işaretlerinin silinmesi
  4. Fazla boşlukların teke indirilmesi
- Frontend'de aynı fonksiyon `src/lib/text.ts` içinde `fold()` adıyla bulunur. Yalnızca istemci tarafı filtrelemede kullanılır.

Frontend zod kuralları `src/lib/validation.ts` içinde ortak şemalar olarak tanımlanır: `nameSchema`, `emailSchema`, `phoneSchema`, `passwordSchema`, `optionalText(max)`. Modüller bunları kullanır.

---

## §8 Güvenlik

### Oturum

- Oturum Spring Session JDBC ile PostgreSQL'de tutulur. Çerez ayarları:

| Ayar | Değer |
|---|---|
| Ad | `OKUL_SESSION` |
| HttpOnly | Açık |
| Secure | prod profilinde açık |
| SameSite | Lax |
| Ömür | `server.servlet.session.cookie.max-age: 14d` (kalıcı çerez; telefonda oturum açık kalır) |
| Hareketsizlik zaman aşımı | `spring.session.timeout: 14d` |

- Principal adı kullanıcının id'sidir (`String.valueOf(userId)`). Bu sayede `FindByIndexNameSessionRepository.findByPrincipalName(id)` ile kullanıcının tüm oturumları bulunup silinebilir (`SessionService.invalidateAll(userId, exceptSessionId)`).

### Giriş akışı

Giriş, Spring Security'nin form login'i yerine özel bir uç noktayla yapılır (`01-cekirdek.md` §6.1). Başarılı girişte şu adımlar izlenir:

```java
request.getSession(true); request.changeSessionId();            // oturum sabitleme koruması
var auth = UsernamePasswordAuthenticationToken.authenticated(String.valueOf(user.getId()), null, List.of());
var ctx = SecurityContextHolder.createEmptyContext(); ctx.setAuthentication(auth);
SecurityContextHolder.setContext(ctx);
securityContextRepository.saveContext(ctx, request, response);  // HttpSessionSecurityContextRepository bean'i
```

- Şifreler `BCryptPasswordEncoder` (strength 10) ile saklanır.
- Kullanıcı bulunamazsa zamanlama saldırısını önlemek için yine `matches()` sabit bir sahte hash ile çağrılır.

### CSRF

- SPA yapılandırması kullanılır: `http.csrf(c -> c.spa())`. Bu, `XSRF-TOKEN` çerezini ve `X-XSRF-TOKEN` başlığını kullanır.
- `spa()` metodu mevcut değilse Spring Security referans dokümanındaki "Single-Page Applications" örneği uygulanır: `CookieCsrfTokenRepository.withHttpOnlyFalse()` ve `SpaCsrfTokenRequestHandler`.
- `GET /api/auth/csrf` uç noktası 204 döner ve token çerezinin oluşmasını sağlar.

### Yetkilendirme

- `SecurityConfig` yetkilendirme kuralları:
  - `permitAll`: `/api/auth/login`, `/api/auth/logout`, `/api/auth/csrf`, `/api/auth/forgot-password`, `/api/auth/password-token`, `/api/auth/password-token/**`, `/api/health`
  - `authenticated`: diğer tüm `/api/**` yolları ve `/ws/**`
- Hata yanıtları:
  - `AuthenticationEntryPoint` → 401 `UNAUTHENTICATED`
  - `AccessDeniedHandler` → `CsrfException` için 403 `CSRF_INVALID`, diğerleri için 403 `FORBIDDEN`
- `UserContextFilter` (`01-cekirdek.md` §5), `AuthorizationFilter`'dan sonra çalışır.

### WebSocket

- Yalnızca sunucudan istemciye olay iletmek için kullanılır. İstemci `SEND` yapmaz.
- Handshake HTTP oturum çereziyle doğrulanır; `/ws` yolu `authenticated` kuralındadır.
- `@EnableWebSocketSecurity` **kullanılmaz**. Bunun yerine bir `ChannelInterceptor` şu kuralları uygular:
  - `CONNECT`: principal yoksa reddedilir.
  - `SUBSCRIBE`: yalnızca `/user/queue/events` hedefine izin verilir.
  - `SEND`: her zaman reddedilir.
- Endpoint `registry.addEndpoint("/ws").setAllowedOriginPatterns(app.allowedOrigins)` ile tanımlanır. Origin kontrolü siteler arası bağlantı ele geçirmeyi engeller.

### HTTP başlıkları

nginx şu başlıkları ekler:

- `Strict-Transport-Security: max-age=31536000`
- `X-Content-Type-Options: nosniff`
- `Referrer-Policy: same-origin`
- `X-Frame-Options: DENY`

### Kaba kuvvet koruması

- 5 hatalı girişte hesap 15 dakika kilitlenir.
- Şifre sıfırlama isteği kullanıcı başına saatte en fazla 3 kez yapılabilir.
- Başka bir hız sınırlaması yoktur.

---

## §9 Dosya servisi (ortak)

Tüm modüller dosya yüklemeyi **iki adımda** yapar:

1. Kullanıcı dosyayı `POST /api/files` ile yükler ve bir `fileId` (UUID) alır.
2. Modülün form isteği bu `fileId` değerlerini gönderir. Modül, dosyayı `FileStorageService.attach(fileId, me, purpose)` ile sahiplenir.

`attach()` şu kontrolleri yapar ve `attached=true` olarak işaretler:

| Kontrol | Başarısızsa |
|---|---|
| Dosya bu kullanıcıya ait ve henüz bağlanmamış | `FILE_NOT_USABLE` |
| `purpose` değeri eşleşiyor | `FILE_NOT_USABLE` |
| Dosya silinmemiş | `FILE_NOT_USABLE` |

24 saat içinde bağlanmayan dosyalar gece işiyle silinir (`01-cekirdek.md` §9).

### `POST /api/files`

- İstek: multipart; `file` alanı ve `purpose` parametresi. `purpose` değerleri: `FEED`, `CHAT`, `PRINT`, `DMS`, `CALENDAR`.
- Yanıt 201: `{ id, originalName, contentType, sizeBytes }`

### Doğrulama

Kontroller bu sırayla yapılır:

| Sıra | Kontrol | Hata |
|---|---|---|
| 1 | Dosya boş mu | `FILE_EMPTY` |
| 2 | Boyut, amaca göre en büyük değeri aşıyor mu | `FILE_TOO_LARGE` |
| 3 | Uzantı beyaz listede mi | `FILE_TYPE_NOT_ALLOWED` (izin verilen uzantılar listelenir) |
| 4 | Tika'nın tespit ettiği tip uzantıyla uyumlu mu | `FILE_CONTENT_MISMATCH` |

Tip uyumu için kurallar:

- Görsel uzantılar `image/*` olmalıdır, `pdf` → `application/pdf`, video → `video/*`.
- Office dosyalarında Tika `application/x-tika-ooxml` veya `application/zip` döndürebilir; bu kabul edilir.
- `txt` ve `csv` için kontrol atlanır.

Amaca göre sınırlar:

| purpose | Uzantılar | En büyük boyut |
|---|---|---|
| FEED, CHAT | jpg, jpeg, png, webp, gif, pdf, doc, docx, xls, xlsx, ppt, pptx, odt, ods, odp, txt, csv, zip, mp4, mov, webm | Görsel 20 MB, video 100 MB, diğer 50 MB |
| PRINT | pdf, doc, docx, xls, xlsx, ppt, pptx, odt, ods, odp, jpg, jpeg, png | 50 MB |
| DMS | FEED listesi + rtf, rar, 7z, mp3 | 100 MB |
| CALENDAR | pdf, doc, docx, xls, xlsx, jpg, jpeg, png | 20 MB |

Boyut sınırları `app.files.*` ayarlarından gelir. Ayrıca `spring.servlet.multipart.max-file-size` 100MB, nginx `client_max_body_size` 110m olarak ayarlanır.

### Saklama

- Fiziksel yol: `${app.storage.root}/{yyyy}/{MM}/{uuid}`. Dosya uzantısız saklanır.
- Dosya önce `.part` adıyla yazılır, ardından `Files.move(ATOMIC_MOVE)` ile kalıcı adına taşınır.
- Dosya adı temizlenir: dizin kısmı atılır, kontrol karakterleri silinir, en fazla 255 karakter tutulur.

### İndirme ve önizleme

- Genel bir indirme uç noktası **yoktur**. Her modül kendi yetki kontrolünü yaptıktan sonra `FileStorageService.serve(file, inline)` çağırır. Bu metot `ResponseEntity<Resource>` döner.
- Yanıt başlıkları:
  - `Content-Type`
  - `Content-Disposition`: `inline` ya da `attachment`; dosya adı RFC 5987 `filename*=UTF-8''...` ile Türkçe karakter destekli yazılır
  - `Cache-Control: private, max-age=300`
  - `X-Content-Type-Options: nosniff`
- `inline` yalnızca pdf, görsel ve video için kullanılır.

### Görsel sıkıştırma (istemcide)

Akış ve Mesaj modüllerinde görseller yüklenmeden önce `browser-image-compression` ile küçültülür:

- `maxWidthOrHeight: 1920`
- `initialQuality: 0.8`
- `fileType: 'image/jpeg'`; gif için uygulanmaz

---

## §10 Bildirim, gerçek zamanlı olaylar ve Web Push (ortak altyapı)

### WebSocket (STOMP)

- Endpoint: `/ws` (native WebSocket, SockJS yok).
- Broker: `enableSimpleBroker("/queue")`.
- Kullanıcı hedefi: `/user/queue/events`.
- Mesaj zarfı:
  `{ "type": "NOTIFICATION" | "BADGES" | "<MODÜL_OLAYI>", "payload": { ... } }`
- `RealtimePublisher.toUsers(Collection<Long> userIds, String type, Object payload)` metodu, aktif transaction varsa gönderimi `TransactionSynchronization.afterCommit` anına erteler.

### Uygulama içi bildirim

`NotificationService` iki metot sunar:

```java
void notify(Collection<Long> userIds, String type, String title, String body, String link);
// notification satırı ekler (aynı transaction), commit sonrası WS "NOTIFICATION" ve Web Push gönderir.
void pushOnly(Collection<Long> userIds, String title, String body, String link, String tag);
// satır eklemez; yalnızca Web Push gönderir (ör. sohbet mesajları).
```

- `type` her modülün kendi sabitidir: `"PRINT_APPROVAL_REQUESTED"` gibi.
- `link`, frontend yoludur: `/fotokopi/123` gibi.

### Web Push

Gönderim (`WebPushSender`):

- `nl.martijndwars:web-push` kütüphanesi kullanılır. Uygulama açılışında `Security.addProvider(new BouncyCastleProvider())` çağrılır.
- `PushService` bean'i, `app.push.vapid-public-key`, `vapid-private-key` ve `subject` ayarlarıyla kurulur.
- Gönderim `@Async` ile yapılır. `spring.threads.virtual.enabled: true` ayarı açıktır.
- Yanıt 404 veya 410 ise abonelik silinir. Diğer hatalar `log.warn` ile yazılır ve kullanıcıya yansıtılmaz.
- Payload JSON'dur: `{ "title", "body", "link", "tag" }`. Toplam boyut 3 KB'ı aşmamalı; `body` 200 karakterle sınırlanır.
- VAPID anahtarları geliştirici makinesinde `npx web-push generate-vapid-keys` ile üretilir ve `.env` dosyasına yazılır.

Service worker (`frontend/public/sw.js`, elle yazılır, kütüphane yok):

| Olay | Davranış |
|---|---|
| `push` | `clients.matchAll({type:'window', includeUncontrolled:true})` çağrılır. Odaklı (`focused`) bir pencere varsa **hiçbir şey yapılmaz**; uygulama aynı olayı WebSocket üzerinden zaten alır ve gösterir. Odaklı pencere yoksa `showNotification(title, {body, tag, data:{link}, icon:'/icons/icon-192.png', badge:'/icons/badge-72.png'})` çağrılır. |
| `notificationclick` | Bildirim kapatılır. Açık bir pencere varsa `focus()` ve `navigate(link)` yapılır; yoksa `clients.openWindow(link)` çağrılır. |
| `install` | `self.skipWaiting()` |
| `activate` | `clients.claim()` |
| `fetch` | **Dinleyici eklenmez.** Çevrimdışı önbellek yoktur. |

---

## §11 Frontend mimarisi

### 11.1 Klasör yapısı

```
frontend/src/
├── app/
│   ├── layout.tsx                 kök: font, <Providers>, <Toaster/>, metadata, viewport
│   ├── manifest.ts                PWA manifest
│   ├── page.tsx                   "/" → me varsa /akis (üyelik yoksa /yonetim), yoksa /giris
│   ├── (auth)/giris/page.tsx, sifremi-unuttum/page.tsx, sifre-belirle/page.tsx, layout.tsx
│   └── (app)/layout.tsx           AppShell (oturum koruması, menü, WS, KVKK)
│       ├── akis/ mesajlar/ fotokopi/ dokumanlar/ takvim/     modül sayfaları
│       ├── bildirimler/page.tsx  profil/page.tsx
│       └── yonetim/layout.tsx, personel/, yillar/, yillar/[id]/, subeler/, gruplar/, gruplar/[id]/, denetim/
├── components/ui/                 shadcn (elle düzenlenmez; gerekirse sarmalayıcı yazılır)
├── components/common/             ortak bileşenler (§11.4)
├── components/layout/             AppShell, Sidebar, BottomNav, TopBar, nav-items.ts
├── features/<modül>/              api.ts (hook'lar), types.ts, schemas.ts, components/
└── lib/                           api.ts, query-client.ts, form.ts, validation.ts, text.ts, format.ts, ws.ts, push.ts, me.ts
```

- Modül başına klasör adları: `features/core`, `features/feed`, `features/print`, `features/chat`, `features/dms`, `features/calendar`.
- `nav-items.ts` çekirdek fazında **tüm modüllerle birlikte** yazılır ve sonra değiştirilmez. Her modülün başlangıçta "Bu bölüm hazırlanıyor" diyen yer tutucu bir sayfası olur; modüller yalnızca kendi sayfa dosyalarını değiştirir.

### 11.2 Sayfa kuralları

- `(app)` altındaki sayfalar istemci bileşenidir (`"use client"`). Veri TanStack Query ile çekilir. Server Components ile veri çekme yapılmaz; çerezli API çağrıları yalnızca istemciden yapılır.
- Dinamik parametreler `useParams()`, sorgu parametreleri `useSearchParams()` ile okunur.
  - `AppShell`, `{children}` öğesini `<Suspense fallback={<PageSkeleton/>}>` ile sarar. Bu yapılmazsa `useSearchParams` build hatası verir.
- API'den gelen görseller `next/image` ile değil, düz `<img>` ile gösterilir; çünkü optimizer çerez göndermez. ESLint'te `@next/next/no-img-element` kuralı kapatılır.
- Liste filtreleri ve sekmeler URL sorgu parametresinde tutulur. Böylece geri tuşu ve paylaşılan bağlantı çalışır.
- Next 16'da `middleware.ts` adı `proxy.ts` olarak değişti; bu projede ikisi de **kullanılmaz**. Oturum koruması `AppShell` içindedir.

`next.config.ts`:

```ts
const nextConfig = {
  output: 'standalone',
  poweredByHeader: false,
  async rewrites() { return process.env.NODE_ENV === 'development'
    ? [{ source: '/api/:path*', destination: 'http://localhost:8080/api/:path*' }] : []; },
  async headers() { return [{ source: '/sw.js', headers: [{ key: 'Cache-Control', value: 'no-cache' }] }]; },
};
```

- Geliştirmede WebSocket doğrudan `ws://localhost:8080/ws` adresine bağlanır. Çerezler porta bağlı olmadığı için oturum çerezi gider. Adres `NEXT_PUBLIC_WS_URL` ortam değişkeninden gelir.
- Prod'da adres `wss://{host}/ws` olarak türetilir.

### 11.3 Veri ve form kuralları

- **Sorgu anahtarı biçimi:** `[modül, kaynak, parametreler?]`. Örnek: `['print','requests',{status}]`. Mutasyon başarısında ilgili anahtarlar `invalidateQueries` ile yenilenir.
- **Varsayılanlar:** `staleTime: 30_000`, `retry: 1` (4xx hatalarında tekrar denenmez), `refetchOnWindowFocus: true`.
- **Formlar:** `react-hook-form` ve `zodResolver` kullanılır. Doğrulama modu `mode: 'onTouched'` olur. Gönderim sırasında buton devre dışı kalır ve spinner gösterir; böylece çift gönderim olmaz.
- **Oturum bilgisi:** `useMe()` hook'u `['me']` sorgusunu çeker ve `MeResponse` döner (`01-cekirdek.md` §6.1). Yetki kontrolleri arayüzde yalnızca `me.permissions.*` ile yapılır. Arayüz kontrolü bir kolaylıktır; asıl kontrol backend'dedir.
- **Rozetler:** `useBadges()` hook'u `GET /api/me/badges` adresini çeker. 60 saniyede bir ve WS `BADGES` olayında yenilenir.

### 11.4 Ortak bileşenler (`components/common/`)

| Bileşen | Görev |
|---|---|
| `PageHeader` | Başlık, opsiyonel açıklama ve sağda aksiyon butonları. Mobilde birincil aksiyon sağ altta FAB olarak gösterilir (`primaryAction` prop'u). |
| `EmptyState` | İkon, tek cümle açıklama ve opsiyonel buton |
| `ErrorState` | Hata mesajı ve "Tekrar dene" butonu (`refetch` çağırır) |
| `PageSkeleton`, `ListSkeleton` | Yükleme iskeletleri |
| `ConfirmDialog` | Başlık, açıklama, onay butonu metni ve `destructive` seçeneği. Mobilde Drawer, masaüstünde AlertDialog kullanır. |
| `ResponsiveDialog` | Masaüstünde (≥768px) `Dialog`, mobilde alttan açılan `Drawer`. Tüm form pencereleri bunu kullanır. |
| `UserAvatar` | Baş harfler (ad ve soyadın ilk harfi). Renk, userId'den 8 renklik paletten türetilir. Boyutlar sm / md / lg. Fotoğraf yoktur. |
| `UserPicker` | Tekli veya çoklu kişi seçimi (Command + Popover). En az 2 karakterle `GET /api/directory/search?kinds=user` çağırır. Seçilenler çip olarak görünür. |
| `TargetPicker` | Kişi, grup, şube ve "Tüm Kurum" hedeflerini seçer. `allowed` prop'u hangi türlerin seçilebileceğini belirler. Boş sorguda kullanıcının grupları ve şubeler listelenir. |
| `FileUploader` | Sürükle-bırak ve "Dosya seç" butonu. Dosya başına ilerleme çubuğu gösterir; yükleme XHR ile yapılır (fetch ilerleme vermez). Hatalar dosya satırında kırmızı metin ve toast olarak görünür. Prop'lar: `purpose`, `multiple`, `accept`, `maxSizeMb`, `compressImages`, `onChange(files)`. Seçilen dosya istemcide önce boyut ve uzantıya göre kontrol edilir. |
| `DateText` | Bugünse "Bugün 17:20", dünse "Dün 09:10"; bu yıl içindeyse "14 Kas 17:20", değilse "14 Kas 2025". `relative` prop'u varsa ve 1 saatten yeniyse "5 dk önce". `date-fns` ve `tr` locale kullanılır. |
| `InfiniteList` | `useInfiniteQuery` ile kaydırmalı liste. Sona yaklaşıldığında (IntersectionObserver) yeni sayfa yükler; altta "Daha fazla" butonu da yedek olarak bulunur. |
| `DataTable` | Basit tablo; sayfalama ve boş/yükleniyor durumlarıyla. Mobilde yatay kaydırılır. |
| `StatusBadge` | Renkli durum rozeti (`tone`: neutral / info / success / warning / danger) |
| `ArchiveBanner` | "Bu içerik {yıl} eğitim-öğretim yılına ait; yalnızca görüntülenebilir." |

---

## §12 UI tasarım sistemi ve UX kuralları

**Hedef:** kurumsal, sakin, modern bir görünüm. Kullanıcıların çoğu teknolojiye uzak öğretmenler ve idari personel olduğundan her ekran ilk bakışta anlaşılmalıdır.

### 12.1 Görsel dil

- **Yazı tipi:** Inter, `next/font/google` ile yüklenir; `subsets: ['latin','latin-ext']` Türkçe karakterler için gereklidir.
- **Renkler.** `globals.css` içinde shadcn değişkenleri aşağıdaki değerlerle ayarlanır. Tema yalnızca açık renklidir; karanlık tema yoktur.

```css
:root{
  --radius: 0.75rem;
  --background:#F5F7FA; --foreground:#0F172A;
  --card:#FFFFFF; --card-foreground:#0F172A; --popover:#FFFFFF; --popover-foreground:#0F172A;
  --primary:#2453C7; --primary-foreground:#FFFFFF;
  --secondary:#EEF2F8; --secondary-foreground:#1E293B;
  --muted:#F1F4F8; --muted-foreground:#5B6B80;
  --accent:#E8EEFB; --accent-foreground:#1E3A8A;
  --destructive:#DC2626; --border:#E3E8EF; --input:#D5DCE6; --ring:#2453C7;
  --success:#15803D; --success-bg:#ECFDF3; --warning:#B45309; --warning-bg:#FFF7E6;
  --important:#D97706; --important-bg:#FFF8E6; --info-bg:#EEF4FF;
}
```

- **Avatar paleti** (8 renk, arka plan / yazı): `#DBEAFE/#1E40AF`, `#DCFCE7/#166534`, `#FCE7F3/#9D174D`, `#FEF3C7/#92400E`, `#E0E7FF/#3730A3`, `#CCFBF1/#115E59`, `#FFE4E6/#9F1239`, `#EDE9FE/#5B21B6`.
- **Tipografi:**

| Öğe | Boyut / ağırlık |
|---|---|
| Sayfa başlığı | 22px/28 semibold (mobilde 20px) |
| Bölüm başlığı | 16px semibold |
| Gövde | 15px/22 (mobilde 16px; iOS'ta odaklanınca yakınlaştırmayı önler) |
| Küçük metin | 13px, `muted-foreground` |

- **Yüzeyler:** kartlar beyaz, 1px `border`, `rounded-xl`, gölge yalnızca `shadow-sm`. Sayfa arka planı `--background`. Boşluklar 4px ızgarasına göre verilir; kart iç boşluğu 16px (masaüstünde 20px).
- **İkonlar:** lucide-react, varsayılan 20px, çizgi kalınlığı 1.75. İkon tek başına anlam taşımaz; yanında metin veya `aria-label` bulunur.

### 12.2 Yerleşim

**Masaüstü (≥1024px):**

- Solda 248px genişliğinde sabit beyaz bir menü vardır:
  - Üstte logo (`/logo.svg`) ve kurum adı (`NEXT_PUBLIC_INSTITUTION_NAME`)
  - Menü öğeleri (ikon, etiket ve sağda sayı rozeti): Akış, Mesajlar, Fotokopi, Dokümanlar, Takvim
  - Bir ayraç, ardından Yönetim (yetkiliyse)
  - En altta kullanıcı kartı: avatar, ad ve unvan; tıklanınca Profilim ve Çıkış Yap seçenekleri açılır
- Sağ üstte yıl çipi ("2026-2027") ve bildirim zili durur.
- İçerik alanı en fazla 1200px genişliktedir.

**Tablet (768–1023px):** Menü yalnızca ikonlardan oluşur (72px); etiketler tooltip olarak görünür.

**Mobil (<768px):**

- Üst çubuk 56px'tir: sayfa başlığı solda, zil ve avatar sağda.
- Alt sekme çubuğu 64px'tir ve `env(safe-area-inset-bottom)` kadar ek boşluk alır. Beş öğe vardır; her birinde ikon, etiket ve rozet bulunur. Aktif öğe `primary` renktedir.
- Yönetim ve Profil, avatar menüsünden açılır.
- Birincil aksiyon sağ altta FAB olarak durur (56px yuvarlak, `primary` renk); sekme çubuğunun üstündedir.

**Yıla atanmamış Sistem Yöneticisi** yalnızca Yönetim ve Profil öğelerini görür.

### 12.3 UX kuralları (her ekranda geçerli)

1. **Ekran başına tek birincil aksiyon.** Bu aksiyon fiil içeren, net bir etiket taşır: "Talep Gönder", "Paylaş", "Personel Ekle". İkincil aksiyonlar `outline` veya `ghost` buton olur.
2. **Dokunma hedefi en az 44px** yüksekliktedir (mobilde butonlar `h-11`, masaüstünde `h-10`).
3. **Form alanları:**
   - Etiket alanın üstündedir; zorunlu alanlarda `*` işareti bulunur.
   - Gerekirse altta gri bir yardım metni olur. Hata metni alanın altında, kırmızı ve ikonla gösterilir.
   - Placeholder bir örnek gösterir; etiketin yerini tutmaz.
   - Mobilde form tek sütundur ve gönder butonu tam genişliktedir.
4. **Tarih ve saat seçimi** yerel `<input type="date">` / `type="datetime-local"` ile yapılır; telefonlarda alışılmış seçici açılır.
5. **Silme ve geri dönüşü olmayan işlemler** `ConfirmDialog` ile sorulur. Açıklama, sonucu açıkça yazar: "Talep iptal edilecek. Bu işlem geri alınamaz."
6. **Her işlemin sonucu görünür.** Başarıda kısa bir toast çıkar, hata §6.4'teki kurallarla gösterilir. Buton spinner'ı kullanıcıya "işleniyor" bilgisini verir.
7. **Boş liste asla boş beyaz alan değildir.** `EmptyState` ile ne olduğu ve ne yapılabileceği yazılır.
8. **Teknik terim kullanılmaz.** Arayüzde geçmeyecek sözcükler ve yerine kullanılacaklar:

| Kullanılmaz | Kullanılır |
|---|---|
| sezon | Eğitim-Öğretim Yılı / Yıl |
| DYS | Dokümanlar |
| bucket | Doküman Alanı |
| upload | Yükle |
| status | Durum |
| hata kodu, HTTP, null | — (gösterilmez) |

9. **Rozet sayıları** 99'u geçerse "99+" yazılır.
10. **Erişilebilirlik:** metin kontrastı WCAG AA seviyesindedir, odak halkası görünür, tüm ikon butonlarda `aria-label` bulunur.
11. **Yükleme durumu.** İlk yüklemede iskelet gösterilir; arka planda yenilemede iskelet gösterilmez.
12. **Arşiv içerik** (geçmiş yıl) gri bir `ArchiveBanner` ile işaretlenir ve tüm düzenleme butonları gizlenir.
13. **Yönetim ekranları masaüstü önceliklidir.** Mobilde tablolar yatay kaydırılır; ayrıca kart tasarımı yapılmaz.

---

## §13 PWA

`app/manifest.ts`:

| Alan | Değer |
|---|---|
| `name` | Kurum adı + " İletişim" |
| `short_name` | "İletişim" |
| `start_url` | "/akis" |
| `display` | "standalone" |
| `background_color` | "#F5F7FA" |
| `theme_color` | "#2453C7" |
| `lang` | "tr" |
| `icons` | 192, 512 ve 512 maskable (`public/icons/`) |

Kök `layout.tsx` ayarları:

- `metadata.appleWebApp = { capable: true, title: 'İletişim', statusBarStyle: 'default' }`
- `apple-touch-icon` (180px)
- `viewport.themeColor`

Service worker kaydı `AppShell` içinde yapılır: `navigator.serviceWorker.register('/sw.js')`; yalnızca `'serviceWorker' in navigator` ise.

### Ana ekrana ekleme kartı

- Görünme koşulu: mobil cihaz ve `display-mode: standalone` değil. Kart Akış sayfasının üstünde gösterilir ve kapatılabilir; kapatıldığı `localStorage: pwa-install-dismissed=1` ile hatırlanır.
- **Android / Chrome:** `beforeinstallprompt` olayı yakalanır; "Yükle" butonu `prompt()` çağırır.
- **iOS Safari:** Adımlar yazılır: "Alttaki Paylaş simgesine dokunun → 'Ana Ekrana Ekle'yi seçin → 'Ekle'ye dokunun." Paylaş ikonu da gösterilir.

### Push izni

İzin isteği (`Notification.requestPermission()`) yalnızca kullanıcı bir butona tıkladığında yapılır.

- **Giriş sonrası bir kez** bilgi kartı gösterilir: "Önemli duyuruları ve onay taleplerini kaçırmamak için bildirimleri açın. [Bildirimleri Aç] [Daha sonra]". "Daha sonra" seçimi 7 gün hatırlanır.
- **iOS'ta ve standalone değilse** kart şunu söyler: "Bildirim almak için önce uygulamayı ana ekrana ekleyin."
- **İzin verilirse** `pushManager.subscribe({ userVisibleOnly: true, applicationServerKey })` çağrılır ve sonuç `POST /api/push/subscriptions` ile kaydedilir.
- **İzin reddedilirse** Profil sayfası şunu yazar: "Bildirim izni tarayıcı ayarlarından kapatılmış. Açmak için tarayıcı/telefon ayarlarından bu siteye bildirim izni verin."

---

## §14 Test politikası

- **Yalnızca backend entegrasyon testleri yazılır**, yalnızca her modül planının "Zorunlu testler" bölümündeki senaryolar için.
- Altyapı: `@SpringBootTest` + MockMvc + Testcontainers PostgreSQL (`@ServiceConnection`, `postgres:18`). Tüm testler aynı container'ı paylaşır (`TestcontainersConfiguration`).
- Ortak taban sınıf `IntegrationTest`:
  - Yardımcılar: `loginAs(userId)` → `SecurityMockMvcRequestPostProcessors.user(String.valueOf(id))` ve `csrf()`; `json(obj)`.
  - Veri hazırlama yardımcıları: `TestData.user(...)`, `TestData.season(...)` ve benzerleri.
  - Her test kendi verisini kurar; testler birbirine bağımlı olmaz.
- **Frontend testi yazılmaz.** Kontrol `npm run typecheck`, `npm run lint` ve faz sonundaki `npm run build` ile yapılır.
- Birim testi, controller testi, DTO veya mapping testi **yazılmaz**.

---

## §15 Dağıtım

### Geliştirme (`deploy/docker-compose.dev.yml`)

| Servis | Ayrıntı |
|---|---|
| `db` | `postgres:18`, port 5432, DB/kullanıcı/şifre: `iletisim/iletisim/iletisim`, volume `pgdata-dev` |
| `mailpit` | `axllent/mailpit`; SMTP 1025, arayüz 8025. Geliştirmede gönderilen e-postalar burada görünür. |

- Backend `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` ile çalışır.
- Frontend `npm run dev` ile çalışır; adres `http://localhost:3000`.

### Prod (`deploy/docker-compose.yml`)

| Servis | Ayrıntı |
|---|---|
| `db` | `postgres:18`. Env: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`. Volume `pgdata`. Healthcheck `pg_isready`. Dışarıya port açılmaz. |
| `backend` | `backend/Dockerfile`: build aşaması `maven:3-eclipse-temurin-25` ile `./mvnw -q -DskipTests package`, çalışma aşaması `eclipse-temurin:25-jre` ile `java -XX:MaxRAMPercentage=75 -Duser.timezone=Europe/Istanbul -jar app.jar`. Env `.env` dosyasından gelir. Volume `files:/data/files`. `depends_on: db (healthy)`. |
| `frontend` | `frontend/Dockerfile`: `node:24-alpine`, build sonrası standalone çıktı (`node server.js`), port 3000. Build argümanları: `NEXT_PUBLIC_INSTITUTION_NAME`, `NEXT_PUBLIC_VAPID_PUBLIC_KEY`. |
| `nginx` | `nginx:stable`, port 80 ve 443. Yapılandırma `deploy/nginx/default.conf`; sertifikalar `deploy/nginx/certs/` klasörüne okul tarafından konur. |

`default.conf` içeriği:

- 80 → 443 yönlendirmesi
- `client_max_body_size 110m`
- `location /api/` → `proxy_pass http://backend:8080;` ve `X-Forwarded-For/Proto/Host` başlıkları
- `location /ws` → `proxy_pass http://backend:8080;`, `proxy_http_version 1.1`, `Upgrade` ve `Connection "upgrade"` başlıkları, `proxy_read_timeout 3600s`
- `location /` → `proxy_pass http://frontend:3000;`
- §8'deki güvenlik başlıkları

Backend ayarı: `server.forward-headers-strategy: native`. Bu sayede gerçek IP ve https bilgisi alınır.

### Ön koşullar (okul BT'si sağlar)

- Bir alan adı ve geçerli SSL sertifikası. PWA ve push bildirimleri HTTPS olmadan çalışmaz.
- Sunucunun internete HTTPS ile çıkışı olmalı. Push servisleri (Google FCM, Apple, Mozilla) ve SMTP için gereklidir.
- Docker ve Docker Compose v2.

### Yedekleme (`deploy/backup.sh`)

- Her gece çalışır: `docker compose exec -T db pg_dump -Fc -U $POSTGRES_USER $POSTGRES_DB > /backup/db-$(date +%F).dump`.
- Dosya volume'u `rsync -a` ile `/backup/files/` klasörüne kopyalanır.
- 14 günden eski dump dosyaları silinir.
- Sunucu crontab'ına şu satır eklenir: `30 2 * * * /opt/iletisim/deploy/backup.sh`.
- Geri yükleme adımları betiğin başında yorum olarak yazılır.

### `.env.example`

```
POSTGRES_DB=iletisim
POSTGRES_USER=iletisim
POSTGRES_PASSWORD=
APP_BASE_URL=https://iletisim.okuladi.k12.tr
APP_INSTITUTION_NAME=Okul Adı
SMTP_HOST=
SMTP_PORT=587
SMTP_USERNAME=
SMTP_PASSWORD=
MAIL_FROM=iletisim@okuladi.k12.tr
VAPID_PUBLIC_KEY=
VAPID_PRIVATE_KEY=
VAPID_SUBJECT=mailto:bt@okuladi.k12.tr
BOOTSTRAP_ADMIN_EMAIL=
BOOTSTRAP_ADMIN_PASSWORD=
```

---

## §16 `application.yml` (ortak bölüm)

```yaml
spring:
  application.name: iletisim
  datasource: { url: "jdbc:postgresql://${DB_HOST:localhost}:5432/${POSTGRES_DB:iletisim}", username: "${POSTGRES_USER:iletisim}", password: "${POSTGRES_PASSWORD:iletisim}" }
  jpa: { open-in-view: false, hibernate.ddl-auto: validate, properties.hibernate.jdbc.time_zone: UTC }
  flyway.enabled: true
  session: { jdbc.initialize-schema: never, timeout: 14d }
  servlet.multipart: { max-file-size: 100MB, max-request-size: 105MB }
  threads.virtual.enabled: true
  mail: { host: "${SMTP_HOST:localhost}", port: "${SMTP_PORT:1025}", username: "${SMTP_USERNAME:}", password: "${SMTP_PASSWORD:}",
          properties.mail.smtp: { auth: "${SMTP_AUTH:false}", starttls.enable: "${SMTP_STARTTLS:false}", connectiontimeout: 10000, timeout: 10000, writetimeout: 10000 } }
server:
  forward-headers-strategy: native
  servlet.session.cookie: { name: OKUL_SESSION, http-only: true, same-site: lax, max-age: 14d, secure: "${COOKIE_SECURE:false}" }
app:
  institution-name: "${APP_INSTITUTION_NAME:Okul}"
  base-url: "${APP_BASE_URL:http://localhost:3000}"
  allowed-origins: ["${APP_BASE_URL:http://localhost:3000}"]
  mail-from: "${MAIL_FROM:iletisim@localhost}"
  storage.root: "${STORAGE_ROOT:./data/files}"
  files: { image-max-mb: 20, video-max-mb: 100, document-max-mb: 50, dms-max-mb: 100, calendar-max-mb: 20 }
  kvkk.version: 1
  auth: { lock-threshold: 5, lock-minutes: 15, reset-token-minutes: 60, setup-token-hours: 72, forgot-limit-per-hour: 3 }
  push: { vapid-public-key: "${VAPID_PUBLIC_KEY:}", vapid-private-key: "${VAPID_PRIVATE_KEY:}", subject: "${VAPID_SUBJECT:mailto:admin@localhost}" }
  bootstrap-admin: { email: "${BOOTSTRAP_ADMIN_EMAIL:}", password: "${BOOTSTRAP_ADMIN_PASSWORD:}" }
  cleanup: { orphan-file-hours: 24, notification-retention-days: 90 }
```

- Modüller kendi ayarlarını `app.<modül>.*` altına ekler. Örnek: `app.print.file-retention-days: 30`.
- **Profiller:**
  - `dev`: `logging.level.tr.okul=DEBUG` ve `DevDataSeeder` açık.
  - `prod`: `COOKIE_SECURE=true` ortam değişkeninden gelir.
