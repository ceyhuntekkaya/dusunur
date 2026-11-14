# 01 — Çekirdek

Kimlik doğrulama, eğitim-öğretim yılı, personel, şube, grup, hedef çözümleme, bildirim, denetim kaydı ve yönetim ekranları.

Ortak kurallar `00-mimari.md` dosyasındadır; bu doküman onları tekrar etmez.

---

## §1 Kapsam

### Var

- E-posta ve şifreyle giriş, şifre belirleme ve sıfırlama (e-posta bağlantısıyla), şifre değiştirme, KVKK onayı
- Eğitim-öğretim yılı yönetimi: oluşturma, personel atama, kopyalama, toplu düzenleme, aktifleştirme
- Personel yönetimi: ekleme, düzenleme, CSV ile toplu ekleme, pasifleştirme (devralan seçimiyle), yeniden aktifleştirme
- Şube ve grup yönetimi
- Ortak hedef seçici ve hedef çözümleme servisi
- Uygulama içi bildirim, Web Push, WebSocket olay kanalı, menü rozetleri
- Denetim kaydı ve yönetim ekranı
- Profil sayfası, PWA kurulumu, push izni

### Yok

Aşağıdakiler **yapılmaz**:

- Profil fotoğrafı
- Kullanıcının kendi bilgilerini düzenlemesi (yalnızca şifre değiştirebilir)
- Organizasyon şeması
- Rol/izin matrisi
- Global arama
- E-posta bildirimi
- İki aşamalı doğrulama
- Oturum/cihaz listesi ekranı

---

## §2 İş kuralları

### 2.1 Roller ve yetkiler

| Yetki | Kimde | Kaynak |
|---|---|---|
| Sistem Yöneticisi | `app_user.system_admin = true` | Kullanıcıya aittir, yıldan bağımsızdır |
| Yönetici | `season_membership.is_manager` | Yıllıktır |
| Fotokopi Sorumlusu | `season_membership.is_print_operator` | Yıllıktır; kendi şubesinin kuyruğunu yönetir |
| Doküman alanı açabilir | `season_membership.can_create_dms_space` veya Yönetici | Yıllıktır |
| Fotokopi onaylayıcısı | `season_membership.print_approver_id` | Yıllıktır; kişinin taleplerini onaylayacak kişidir |
| Personel | Aktif yıla ataması olan herkes | — |

- **Sistem Yöneticisi**, `/yonetim` altındaki tüm ekranlara erişir. Yıla atanmamışsa modülleri kullanamaz.
- **Grupları** Yönetici veya Sistem Yöneticisi yönetir.
- **Unvan** serbest metindir ve yalnızca görüntülenir; yetki vermez.

### 2.2 Giriş kuralları

`POST /api/auth/login` isteği aşağıdaki sırayla kontrol edilir. İlk başarısız adımda işlem durur.

1. E-posta normalize edilir (trim, `toLowerCase(Locale.ROOT)`) ve kullanıcı aranır.
2. Kullanıcı varsa ve `locked_until > now` ise → `AUTH_ACCOUNT_LOCKED` (kalan dakika yukarı yuvarlanır).
3. Kullanıcı yoksa → sahte hash ile `matches()` çalıştırılır, ardından `AUTH_INVALID_CREDENTIALS`.
4. `password_hash IS NULL` ise → `AUTH_PASSWORD_NOT_SET`.
5. Şifre yanlışsa:
   - `failed_login_count` 1 artırılır.
   - Sayı 5'e ulaştıysa `locked_until = now + 15 dk` yapılır, sayaç sıfırlanır, `AUTH_ACCOUNT_LOCKED(15)` döner.
   - Ulaşmadıysa `AUTH_INVALID_CREDENTIALS` döner.
   - Bu güncelleme, istisna fırlatılsa bile kaydedilmelidir. Bu yüzden `LoginAttemptService` içinde `@Transactional(propagation = REQUIRES_NEW)` ile yapılır.
6. `status = PASSIVE` ise → `AUTH_ACCOUNT_PASSIVE`.
7. Kullanıcı Sistem Yöneticisi değilse:
   - Aktif yıl yoksa → `AUTH_NO_ACTIVE_SEASON`
   - Kullanıcının aktif yılda ataması yoksa → `AUTH_NO_SEASON_MEMBERSHIP(yılAdı)`
8. Başarılı giriş:
   - `failed_login_count = 0`, `locked_until = null`, `last_login_at = now`
   - Oturum oluşturulur (`00-mimari.md` §8)
   - Denetim kaydı yazılır
   - `MeResponse` döner

Hatalı her deneme `AUTH_LOGIN_FAILED` olarak denetim kaydına yazılır; detayda `{ "email": "..." }` bulunur.

### 2.3 Her istekte kontrol (`UserContextFilter`)

Oturum açık olsa bile her `/api/**` ve `/ws` isteğinde aşağıdakiler yeniden kontrol edilir (§5):

| Durum | Sonuç |
|---|---|
| Kullanıcı yok veya pasif | Oturum silinir → 401 `AUTH_ACCOUNT_PASSIVE` |
| Sistem Yöneticisi değil ve aktif yıl yok | Oturum silinir → 403 `AUTH_NO_ACTIVE_SEASON` |
| Sistem Yöneticisi değil ve aktif yılda ataması yok | Oturum silinir → 403 `AUTH_NO_SEASON_MEMBERSHIP` |
| `kvkk_version < app.kvkk.version` ve yol KVKK muafiyet listesinde değil | 403 `KVKK_REQUIRED` |

- **KVKK muafiyet listesi:** `/api/auth/me`, `/api/auth/kvkk`, `/api/auth/kvkk/accept`, `/api/auth/logout`, `/api/auth/csrf`.
- **Sonuç:** Yıl değiştiğinde veya kişi yıldan çıkarıldığında, açık oturumlar bir sonraki istekte kendiliğinden kapanır.

### 2.4 Eğitim-öğretim yılı

**Durumlar:**

- `DRAFT` (Hazırlık): en fazla 1 tane olabilir.
- `ACTIVE` (Aktif): en fazla 1 tane olabilir.
- `CLOSED` (Kapandı): sınırsız.

**Ad kuralı:** Ad `YYYY-YYYY` biçimindedir ve ikinci yıl birinciden 1 fazladır. Yeni yılın adı mevcut tüm yıllardan büyük olmalıdır.

**İşlem izinleri:**

| İşlem | DRAFT | ACTIVE | CLOSED |
|---|---|---|---|
| Ad ve tarihleri düzenleme | ✔ | ✔ | ✘ |
| Personel atama, düzenleme, çıkarma | ✔ | ✔ | ✘ |
| Silme | ✔ | ✘ | ✘ |
| Aktifleştirme | ✔ | — | ✘ |

**Aktifleştirme:**

- Tek bir transaction içinde yapılır: eski ACTIVE yıl → CLOSED, yeni yıl → ACTIVE.
- Ardından tüm `SeasonActivationHook.onActivated(eski, yeni)` metotları çalışır. Örneğin Fotokopi modülü açık talepleri iptal eder.
- Elle "yılı kapat" işlemi **yoktur**. Bir yıl yalnızca yeni yıl aktifleşince kapanır.

**Kayıtların yıla bağlanması:**

- Modüller her yeni kayda `season_id = me.seasonId()` değerini yazar. Yıl tarihleri yalnızca bilgi amaçlıdır (D-08).
- Bir kaydı değiştirmeden önce `me.requireWritable(kayıt.seasonId)` çağrılır. Kaydın yılı aktif yıl değilse → `SEASON_READ_ONLY`.

### 2.5 Yıllık atama (`season_membership`)

**Atanabilecek kişi:** Yalnızca `ACTIVE` durumdaki personel bir yıla atanabilir. Her kişi bir yılda en fazla bir kez atanır.

**Şube:** Zorunludur ve aktif bir şube olmalıdır.

**Fotokopi onaylayıcısı:**

- Opsiyoneldir.
- Aynı yıla atanmış ve aktif bir kişi olmalıdır.
- Kişinin kendisi olamaz.
- İki kişinin birbirini onaylaması serbesttir.

**Onaylayıcı değişikliğinin etkisi:** Yalnızca yeni talepleri etkiler; bekleyen talepler mevcut onaylayıcıda kalır. Arayüzde yardım metni olarak yazılır.

**Yıldan çıkarma şu durumlarda engellenir:**

- Kişi o yılda başka personelin onaylayıcısıysa → `SEASON_MEMBER_IS_APPROVER(n)`
- Herhangi bir `MembershipRemovalHook.blockers()` boş olmayan liste dönerse → `SEASON_MEMBER_REMOVAL_BLOCKED` (detaylarda gerekçeler listelenir)

Kişi aktif yıldan çıkarılırsa tüm oturumları kapatılır.

### 2.6 Personel

**Şifre belirleme:**

- Personeli Sistem Yöneticisi ekler. Şifre girilmez.
- Sistem 72 saat geçerli bir "şifre belirleme" bağlantısı üretir ve e-postayla gönderir.
- E-posta gönderilemese bile personel kaydı oluşur; yanıtta `mailSent: false` döner.

**Sistem Yöneticisi şunları yapamaz:**

- Kendini pasifleştirmek
- Kendi yönetici işaretini kaldırmak
- Son aktif Sistem Yöneticisini pasifleştirmek veya işaretini kaldırmak

**Pasifleştirme:**

1. `deactivation-check` uç noktası etkileri listeler. Kaynaklar: çekirdek (kişinin onaylayıcısı olduğu personel sayısı) ve tüm `UserDeactivationHook.describe()` metotları (örneğin Fotokopi'de bekleyen onaylar, Doküman'da sahip olunan alanlar).
2. Herhangi bir sayı 0'dan büyükse "Devralacak personel" seçimi zorunludur.
3. Pasifleştirme tek bir transaction içinde yapılır:
   - `status = PASSIVE`
   - Tüm `transfer(userId, successorId)` metotları çalışır
   - Kişi tüm gruplardan çıkarılır (`GroupMembersRemovedEvent`)
   - Push abonelikleri silinir
4. Commit sonrasında tüm oturumlar kapatılır.

**Yeniden aktifleştirme:** Kişi giriş yapabilmek için yeniden aktif yıla atanmalıdır. Gruplar geri gelmez.

**Kayıtlar silinmez.** Personel kaydı hiçbir zaman fiziksel olarak silinmez; geçmiş içerikte "(Eski personel)" etiketiyle görünür.

### 2.7 Şube

**Ad:** Ad tekil olmalıdır. Karşılaştırma `fold` edilmiş anahtarla yapılır; yani "Merkez" ile "merkez" aynı sayılır.

**Pasifleştirme şu durumlarda engellenir:**

- Aktif veya hazırlıktaki yılda bu şubeye atanmış personel varsa → `BRANCH_IN_USE(n)`
- Bu son aktif şubeyse → `BRANCH_LAST_ACTIVE`

### 2.8 Grup

- **Ad:** Tekildir (fold edilmiş anahtarla karşılaştırılır).
- **Üyeler:** Yalnızca aktif personel üye olarak eklenebilir.
- **Arşivleme:** Arşivlenen grup hedef seçicilerde görünmez ve değiştirilemez. Geçmiş içerik üyelerine açık kalır.
- **Silme yoktur.** Grup silinmez, yalnızca arşivlenir.
- **Olaylar:** Grup değişiklikleri olay yayınlar (§8). Mesajlaşma modülü grup sohbetini bu olaylarla yönetir.

### 2.9 Hedef (alıcı) çözümleme

Hedef tipleri: `USER`, `GROUP`, `BRANCH`, `ALL`.

`AudienceResolver.resolveUserIds(seasonId, targets)` metodu verilen hedeflere karşılık gelen kişi id'lerini döner. Dönen her kişi şu iki koşulu sağlar:

- Durumu `ACTIVE`
- Verilen yıla atanmış

Hedef tiplerinin anlamı:

| Tip | Kimleri kapsar |
|---|---|
| `USER` | Seçilen kişi |
| `GROUP` | Grubun üyeleri (şu anki üyelik) |
| `BRANCH` | O yılda o şubeye atanmış herkes |
| `ALL` | O yıla atanmış herkes |

Uygulama notu: Boş id listeleri için SQL'e koşul **eklenmez**. Sorgu dinamik kurulur ve `IN (:list)` yalnızca liste boş değilse yazılır.

---

## §3 Veritabanı — `V1__core.sql`

```sql
CREATE TABLE branch (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name        varchar(100) NOT NULL,
  name_key    varchar(100) NOT NULL,
  active      boolean      NOT NULL DEFAULT true,
  created_at  timestamptz  NOT NULL DEFAULT now(),
  updated_at  timestamptz  NOT NULL DEFAULT now(),
  CONSTRAINT ux_branch_name_key UNIQUE (name_key)
);

CREATE TABLE app_user (
  id                 bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  email              varchar(254) NOT NULL,
  password_hash      varchar(100),
  first_name         varchar(50)  NOT NULL,
  last_name          varchar(50)  NOT NULL,
  search_key         varchar(400) NOT NULL,           -- fold(first_name + ' ' + last_name + ' ' + email)
  phone              varchar(13),
  discipline         varchar(100),                    -- branş
  status             varchar(10)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','PASSIVE')),
  system_admin       boolean      NOT NULL DEFAULT false,
  kvkk_version       integer      NOT NULL DEFAULT 0,
  kvkk_accepted_at   timestamptz,
  failed_login_count integer      NOT NULL DEFAULT 0,
  locked_until       timestamptz,
  last_login_at      timestamptz,
  created_at         timestamptz  NOT NULL DEFAULT now(),
  updated_at         timestamptz  NOT NULL DEFAULT now(),
  CONSTRAINT ux_user_email UNIQUE (email)
);

CREATE TABLE season (
  id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name         varchar(9)  NOT NULL,
  start_date   date        NOT NULL,
  end_date     date        NOT NULL,
  status       varchar(10) NOT NULL CHECK (status IN ('DRAFT','ACTIVE','CLOSED')),
  activated_at timestamptz,
  closed_at    timestamptz,
  created_at   timestamptz NOT NULL DEFAULT now(),
  updated_at   timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ux_season_name UNIQUE (name),
  CONSTRAINT ck_season_dates CHECK (end_date > start_date)
);
CREATE UNIQUE INDEX ux_season_one_active ON season ((true)) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX ux_season_one_draft  ON season ((true)) WHERE status = 'DRAFT';

CREATE TABLE season_membership (
  id                   bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  season_id            bigint  NOT NULL REFERENCES season(id) ON DELETE CASCADE,
  user_id              bigint  NOT NULL REFERENCES app_user(id),
  branch_id            bigint  NOT NULL REFERENCES branch(id),
  title                varchar(100),
  is_manager           boolean NOT NULL DEFAULT false,
  is_print_operator    boolean NOT NULL DEFAULT false,
  can_create_dms_space boolean NOT NULL DEFAULT false,
  print_approver_id    bigint  REFERENCES app_user(id),
  created_at           timestamptz NOT NULL DEFAULT now(),
  updated_at           timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ux_membership_season_user UNIQUE (season_id, user_id),
  CONSTRAINT ck_membership_not_self CHECK (print_approver_id IS NULL OR print_approver_id <> user_id)
);
CREATE INDEX ix_membership_user     ON season_membership (user_id);
CREATE INDEX ix_membership_branch   ON season_membership (season_id, branch_id);
CREATE INDEX ix_membership_approver ON season_membership (season_id, print_approver_id);

CREATE TABLE staff_group (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name        varchar(100) NOT NULL,
  name_key    varchar(100) NOT NULL,
  description varchar(500),
  archived    boolean      NOT NULL DEFAULT false,
  created_by  bigint       NOT NULL REFERENCES app_user(id),
  created_at  timestamptz  NOT NULL DEFAULT now(),
  updated_at  timestamptz  NOT NULL DEFAULT now(),
  CONSTRAINT ux_group_name_key UNIQUE (name_key)
);

CREATE TABLE staff_group_member (
  group_id  bigint      NOT NULL REFERENCES staff_group(id),
  user_id   bigint      NOT NULL REFERENCES app_user(id),
  added_at  timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (group_id, user_id)
);
CREATE INDEX ix_group_member_user ON staff_group_member (user_id);

CREATE TABLE auth_token (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id     bigint      NOT NULL REFERENCES app_user(id),
  token_hash  char(64)    NOT NULL,                  -- sha256 hex
  purpose     varchar(10) NOT NULL CHECK (purpose IN ('SETUP','RESET')),
  expires_at  timestamptz NOT NULL,
  used_at     timestamptz,
  created_at  timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ux_auth_token_hash UNIQUE (token_hash)
);
CREATE INDEX ix_auth_token_user ON auth_token (user_id, created_at);

CREATE TABLE stored_file (
  id             uuid PRIMARY KEY,
  original_name  varchar(255) NOT NULL,
  content_type   varchar(150) NOT NULL,
  size_bytes     bigint       NOT NULL,
  storage_path   varchar(300) NOT NULL,
  purpose        varchar(10)  NOT NULL CHECK (purpose IN ('FEED','CHAT','PRINT','DMS','CALENDAR')),
  uploaded_by    bigint       NOT NULL REFERENCES app_user(id),
  attached       boolean      NOT NULL DEFAULT false,
  created_at     timestamptz  NOT NULL DEFAULT now(),
  deleted_at     timestamptz
);
CREATE INDEX ix_file_orphan ON stored_file (created_at) WHERE attached = false AND deleted_at IS NULL;

CREATE TABLE notification (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id     bigint       NOT NULL REFERENCES app_user(id),
  type        varchar(50)  NOT NULL,
  title       varchar(200) NOT NULL,
  body        varchar(500),
  link        varchar(300),
  created_at  timestamptz  NOT NULL DEFAULT now(),
  read_at     timestamptz
);
CREATE INDEX ix_notification_user   ON notification (user_id, id DESC);
CREATE INDEX ix_notification_unread ON notification (user_id) WHERE read_at IS NULL;

CREATE TABLE push_subscription (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id     bigint        NOT NULL REFERENCES app_user(id),
  endpoint    varchar(1000) NOT NULL,
  p256dh      varchar(200)  NOT NULL,
  auth        varchar(100)  NOT NULL,
  user_agent  varchar(300),
  created_at  timestamptz   NOT NULL DEFAULT now(),
  CONSTRAINT ux_push_endpoint UNIQUE (endpoint)
);
CREATE INDEX ix_push_user ON push_subscription (user_id);

CREATE TABLE audit_log (
  id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  created_at    timestamptz  NOT NULL DEFAULT now(),
  actor_user_id bigint       REFERENCES app_user(id),
  ip            varchar(45),
  action        varchar(60)  NOT NULL,
  action_label  varchar(120) NOT NULL,
  entity_type   varchar(40),
  entity_id     varchar(40),
  detail        jsonb
);
CREATE INDEX ix_audit_created ON audit_log (created_at DESC);
CREATE INDEX ix_audit_actor   ON audit_log (actor_user_id, created_at DESC);

CREATE FUNCTION audit_log_immutable() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'audit_log is append-only'; END $$;
CREATE TRIGGER trg_audit_log_immutable BEFORE UPDATE OR DELETE ON audit_log
  FOR EACH ROW EXECUTE FUNCTION audit_log_immutable();

-- Spring Session JDBC tabloları:
-- spring-session-jdbc jar içindeki org/springframework/session/jdbc/schema-postgresql.sql
-- dosyasının içeriği buraya değiştirilmeden kopyalanır.
```

---

## §4 Entity ve sınıf eşlemesi

| Tablo | Entity | Not |
|---|---|---|
| branch | `core/branch/Branch` | `nameKey`, `setName()` içinde `TextNormalizer.fold` ile set edilir |
| app_user | `core/user/AppUser` | `UserStatus { ACTIVE, PASSIVE }`. `searchKey` her kayıtta `@PrePersist/@PreUpdate` ile yeniden hesaplanır. |
| season | `core/season/Season` | `SeasonStatus { DRAFT, ACTIVE, CLOSED }` |
| season_membership | `core/season/SeasonMembership` | Java alan adları: `manager`, `printOperator`, `canCreateDmsSpace`. Kolon adları `@Column(name="is_manager")` gibi açıkça yazılır. |
| staff_group | `core/group/StaffGroup` | |
| staff_group_member | `core/group/StaffGroupMember` | `@IdClass(StaffGroupMemberId)` ile bileşik anahtar |
| auth_token | `core/auth/AuthToken` | `TokenPurpose { SETUP, RESET }` |
| stored_file | `common/file/StoredFile` | `@Id UUID id`; id servis tarafından `UUID.randomUUID()` ile atanır. `FilePurpose` enum. |
| notification | `common/notify/Notification` | |
| push_subscription | `common/notify/PushSubscription` | |
| audit_log | Entity **yok** | `AuditService` JdbcClient ile `INSERT ... CAST(:detail AS jsonb)` yazar. Okumada `detail::text` alınır; record alanı `@JsonRawValue String detail`. |

**jsonb kuralı:** Hiçbir modül `jsonb` kolonunu JPA ile eşlemez. Bu kolonlara yalnızca JdbcClient ile erişilir.

---

## §5 UserContext, filtre ve yetki yardımcıları

```java
public record UserContext(long userId, String firstName, String lastName, boolean systemAdmin,
                          Long seasonId, String seasonName, Membership membership, Set<Long> groupIds) {
  public record Membership(long id, long branchId, String branchName, String title, boolean manager,
                           boolean printOperator, boolean canCreateDmsSpace, Long printApproverId) {}
  public String fullName()            { return firstName + " " + lastName; }
  public boolean isMember()           { return membership != null; }
  public boolean isManager()          { return membership != null && membership.manager(); }
  public boolean canManageGroups()    { return systemAdmin || isManager(); }
  public boolean canCreateDmsSpace()  { return membership != null && (membership.manager() || membership.canCreateDmsSpace()); }
  public long requireMember()         // membership yoksa MEMBERSHIP_REQUIRED; aktif seasonId döner
  public void requireManager()        // değilse CORE_MANAGER_ONLY
  public void requireWritable(long recordSeasonId) // recordSeasonId != seasonId ise SEASON_READ_ONLY
}
```

### `UserContextFilter` (`OncePerRequestFilter`)

**Kapsam:** `/api/**` ve `/ws`. Kimliği doğrulanmamış istekler filtreden olduğu gibi geçer; onları Spring Security reddeder.

Her istekte iki sorgu çalışır:

```sql
-- Q1
SELECT u.id, u.first_name, u.last_name, u.status, u.system_admin, u.kvkk_version,
       s.id AS season_id, s.name AS season_name,
       m.id AS membership_id, m.branch_id, b.name AS branch_name, m.title,
       m.is_manager, m.is_print_operator, m.can_create_dms_space, m.print_approver_id
FROM app_user u
LEFT JOIN season s ON s.status = 'ACTIVE'
LEFT JOIN season_membership m ON m.season_id = s.id AND m.user_id = u.id
LEFT JOIN branch b ON b.id = m.branch_id
WHERE u.id = :uid;
-- Q2 (arşivli gruplar dahil; geçmiş içerik görünürlüğü için)
SELECT group_id FROM staff_group_member WHERE user_id = :uid;
```

- §2.3'teki kurallar uygulanır. Hatalar `ErrorResponseWriter` ile yazılır. Oturum silinirken `request.getSession(false).invalidate()` çağrılır.
- Oluşan `UserContext` nesnesi `request.setAttribute("userContext", ctx)` ile isteğe eklenir. `UserContextArgumentResolver`, controller parametresini buradan doldurur.

### `AdminPathInterceptor`

`HandlerInterceptor`, `/api/admin/**` için çalışır. Kullanıcı Sistem Yöneticisi değilse → 403 `CORE_ADMIN_ONLY`.

---

## §6 API

Notlar:

- "Doğrulama" sütunundaki mesajlar `00-mimari.md` §7'den gelir. Burada yalnızca o tabloda olmayan mesajlar yazılmıştır.
- Tüm DTO'lar TypeScript gösterimiyle verilmiştir. Java'da birebir aynı alanlara sahip `record` olarak yazılır. `?` alanın opsiyonel ya da null olabileceğini gösterir.

### 6.1 Kimlik doğrulama (`core/auth`)

```ts
type MeResponse = {
  id: number; firstName: string; lastName: string; fullName: string; email: string;
  phone?: string; discipline?: string; systemAdmin: boolean; kvkkRequired: boolean;
  season?: { id: number; name: string };
  membership?: { branchId: number; branchName: string; title?: string; manager: boolean; printOperator: boolean;
                 canCreateDmsSpace: boolean; printApproverId?: number; printApproverName?: string };
  permissions: { member: boolean; canPostToAll: boolean; canPostImportant: boolean; canManageGroups: boolean;
                 canEditInstitutionCalendar: boolean; canCreateDmsSpace: boolean; printOperator: boolean; canAccessAdmin: boolean };
};
```

`permissions` alanları şöyle hesaplanır:

| Alan | Değer |
|---|---|
| `member` | `membership != null` |
| `canPostToAll` | Yönetici mi |
| `canPostImportant` | Yönetici mi |
| `canEditInstitutionCalendar` | Yönetici mi |
| `canManageGroups` | Yönetici mi veya Sistem Yöneticisi mi |
| `canCreateDmsSpace` | `UserContext.canCreateDmsSpace()` |
| `printOperator` | `membership.printOperator` |
| `canAccessAdmin` | Sistem Yöneticisi mi |

| Uç nokta | Erişim | İstek | Yanıt | Kurallar ve hatalar |
|---|---|---|---|---|
| `GET /api/auth/csrf` | herkes | — | 204 | CSRF çerezini üretir |
| `POST /api/auth/login` | herkes | `{ email, password }` | 200 `MeResponse` | Doğrulama: email boşsa "E-posta adresinizi girin.", password boşsa "Şifrenizi girin." İş kuralları §2.2'de. |
| `POST /api/auth/logout` | herkes | — | 204 | Oturum varsa silinir. Yolu `permitAll` listesine eklenir. |
| `GET /api/auth/me` | oturum | — | 200 `MeResponse` | |
| `GET /api/auth/kvkk` | oturum | — | `{ version: number, text: string }` | Metin `classpath:kvkk/aydinlatma.md` dosyasından gelir. İçeriği okul sağlar; başlangıçta yer tutucu metin konur. |
| `POST /api/auth/kvkk/accept` | oturum | — | 204 | `kvkk_version = app.kvkk.version`, `kvkk_accepted_at = now`. Denetim: `KVKK_ACCEPTED`. |
| `POST /api/auth/change-password` | oturum | `{ currentPassword, newPassword }` | 204 | Mevcut şifre yanlışsa `AUTH_CURRENT_PASSWORD_WRONG` (alan: currentPassword). Yeni şifre eskisiyle aynıysa `AUTH_PASSWORD_SAME` (alan: newPassword). Başarıda diğer oturumlar kapatılır. Denetim: `AUTH_PASSWORD_CHANGED`. |
| `POST /api/auth/forgot-password` | herkes | `{ email }` | 200 `{ message }` | Ayrıntılar aşağıda. |
| `GET /api/auth/password-token/{token}` | herkes | — | 200 `{ purpose: 'SETUP'\|'RESET', firstName, email }` | Token geçerli sayılmak için dört koşulu sağlar: hash var, `used_at IS NULL`, `expires_at > now`, kullanıcı `ACTIVE`. Değilse `AUTH_TOKEN_INVALID`. |
| `POST /api/auth/password-token` | herkes | `{ token, newPassword }` | 204 | Ayrıntılar aşağıda. |

**`POST /api/auth/forgot-password`**

- Yanıt her durumda aynıdır: "Bu e-posta adresi sistemde kayıtlıysa şifre yenileme bağlantısı gönderildi. Lütfen e-postanızı (gereksiz klasörü dahil) kontrol edin."
- Kullanıcı yoksa veya pasifse hiçbir şey yapılmaz.
- Son 1 saatte bu kullanıcı için 3 veya daha fazla RESET token üretildiyse hiçbir şey yapılmaz.
- Diğer durumda:
  - Önceki kullanılmamış token'lar geçersiz kılınır (`used_at = now`).
  - 60 dakika geçerli bir RESET token üretilir.
  - E-posta `@Async` ile gönderilir. Gönderim hatası yalnızca loglanır.

**`POST /api/auth/password-token`**

- Token kontrolü `GET` ile aynıdır.
- Şifre kaydedilir, token `used_at = now` yapılır, kullanıcının diğer token'ları geçersiz kılınır.
- Kilit sıfırlanır ve tüm oturumlar kapatılır.
- Denetim: `AUTH_PASSWORD_SET`, detayda `{ purpose }`.

**Token üretimi:**

- 32 bayt `SecureRandom` değeri base64url formatına (padding'siz) çevrilir.
- Veritabanında yalnızca SHA-256 hex değeri saklanır.
- Bağlantı: `{app.base-url}/sifre-belirle?token={token}`

**E-posta şablonları** (`MailService`; düz metin ve basit HTML, UTF-8):

| Şablon | Konu | Gövde |
|---|---|---|
| SETUP (`passwordSet = false`) | `{kurum} İletişim – Hesabınız oluşturuldu` | "Merhaba {ad}, {kurum} iç iletişim sistemine hesabınız oluşturuldu. Kullanıcı adınız: {email}. Şifrenizi belirlemek için bağlantıya tıklayın: {link} Bağlantı 72 saat geçerlidir. Şifrenizi belirledikten sonra {baseUrl} adresinden giriş yapabilirsiniz." |
| SETUP (`passwordSet = true`, yönetici gönderdiyse) | `{kurum} İletişim – Şifre yenileme bağlantısı` | "Merhaba {ad}, okul yönetimi sizin için bir şifre yenileme bağlantısı oluşturdu: {link} Bağlantı 72 saat geçerlidir." |
| RESET | `{kurum} İletişim – Şifre yenileme` | "Merhaba {ad}, hesabınız için şifre yenileme talebinde bulunuldu. Yeni şifre belirlemek için: {link} Bağlantı 60 dakika geçerlidir. Bu talebi siz yapmadıysanız bu e-postayı dikkate almayın; şifreniz değişmez." |

### 6.2 Rozetler ve push abonelikleri

| Uç nokta | İstek | Yanıt | Kurallar |
|---|---|---|---|
| `GET /api/me/badges` | — | `Record<string, number>` | Tüm `BadgeProvider` sonuçları birleştirilir. Çekirdek `notifications` anahtarını (okunmamış bildirim sayısı) verir. |
| `POST /api/push/subscriptions` | `{ endpoint, keys: { p256dh, auth } }` | 204 | Doğrulama: endpoint `https://` ile başlar ve en fazla 1000 karakterdir; p256dh en fazla 200, auth en fazla 100 karakterdir. Hata: `PUSH_SUBSCRIPTION_INVALID`. Aynı endpoint varsa kaydın `user_id` ve anahtarları güncellenir (upsert). `user_agent` başlıktan alınır (ilk 300 karakter). |
| `DELETE /api/push/subscriptions` | `{ endpoint }` | 204 | Yalnızca kendi aboneliğini siler; kayıt yoksa da 204 döner. |

Frontend, çıkış yapmadan önce önce `pushManager` aboneliğini, sonra bu kaydı siler.

### 6.3 Bildirimler

```ts
type NotificationDto = { id: number; type: string; title: string; body?: string; link?: string; createdAt: string; read: boolean };
```

| Uç nokta | Yanıt | Kurallar |
|---|---|---|
| `GET /api/notifications?before=&size=20` | `CursorResponse<NotificationDto>` | Yalnızca kullanıcının kendi bildirimleri, `id DESC` sırasıyla |
| `POST /api/notifications/{id}/read` | 204 | Bildirim kendisine ait değilse `NOT_FOUND` |
| `POST /api/notifications/read-all` | 204 | |

WS üzerinden `NOTIFICATION` olayı gelir; payload `NotificationDto` biçimindedir.

### 6.4 Rehber ve hedef seçici (`core/directory`)

```ts
type DirUser   = { id: number; fullName: string; title?: string; branchName?: string; discipline?: string };
type DirGroup  = { id: number; name: string; memberCount: number };
type DirBranch = { id: number; name: string };
type TargetRef = { type: 'USER' | 'GROUP' | 'BRANCH' | 'ALL'; id?: number };   // ALL için id yok
type TargetView = TargetRef & { name: string };                               // ALL → "Tüm Kurum"
```

**`GET /api/directory/search?q=&kinds=user,group,branch&groupScope=mine|all&limit=20`**

- Erişim: yıla atanmış kullanıcı (`requireMember`).
- Yanıt: `{ users: DirUser[], groups: DirGroup[], branches: DirBranch[] }`. İstenmeyen türler boş dizi olarak döner.
- `users`:
  - `q` en az 2 karakter olmalıdır; değilse boş döner.
  - Yalnızca aktif yıla atanmış, `ACTIVE` durumdaki kişiler döner; istek yapan kişi de dahildir.
  - Arama alanı `search_key`'dir. Sıralama Türkçe alfabeye göredir.
  - `limit` en fazla 50'dir.
- `groups`:
  - Arşivli gruplar dönmez.
  - `groupScope=mine` (varsayılan) yalnızca üyesi olunan grupları, `all` tüm grupları döner.
  - `q` verilmişse `name_key` üzerinde filtrelenir; `q` boşsa tamamı döner.
- `branches`: Aktif şubeler döner; `q` verilmişse filtrelenir.

**`AudienceResolver`** (servis; modüller kullanır):

```java
Set<Long> resolveUserIds(long seasonId, Collection<TargetRef> targets);
void validate(UserContext me, Collection<TargetRef> targets, Set<TargetType> allowed);
List<TargetView> describe(Collection<TargetRef> targets);
```

`validate()` şu hataları fırlatır:

| Durum | Hata |
|---|---|
| Liste boş | `TARGET_REQUIRED` |
| Tip izin listesinde yok | `TARGET_TYPE_NOT_ALLOWED({tipAdı})` — tip adları "Tüm Kurum", "Şube", "Grup", "Kişi" |
| `USER` hedefi aktif ve yıla atanmış değil; `GROUP` hedefi yok veya arşivli; `BRANCH` hedefi pasif | `TARGET_INVALID` |
| `ALL` ile birlikte başka hedef var | Hata verilmez; liste yalnızca `ALL` olarak sadeleştirilir |

### 6.5 Şubeler (`core/branch`)

```ts
type BranchRow = { id: number; name: string; active: boolean; memberCount: number }; // memberCount: aktif yıldaki atama sayısı
```

| Uç nokta | İstek | Yanıt | Kurallar ve hatalar |
|---|---|---|---|
| `GET /api/branches` | — | `DirBranch[]` | Oturum açmış herkes; yalnızca aktif şubeler; Türkçe sıralı |
| `GET /api/admin/branches` | — | `BranchRow[]` | |
| `POST /api/admin/branches` | `{ name }` | 201 `BranchRow` | Ad kuralı §7. Ad varsa `BRANCH_NAME_TAKEN` (alan: name). Denetim: `BRANCH_CREATED`. |
| `PUT /api/admin/branches/{id}` | `{ name }` | `BranchRow` | Aynı kurallar. Denetim: `BRANCH_UPDATED`. |
| `POST /api/admin/branches/{id}/deactivate` | — | 204 | `BRANCH_IN_USE(n)`, `BRANCH_LAST_ACTIVE`. Denetim: `BRANCH_DEACTIVATED`. |
| `POST /api/admin/branches/{id}/activate` | — | 204 | Denetim: `BRANCH_ACTIVATED` |

### 6.6 Personel yönetimi (`core/user`)

```ts
type MembershipInfo = { id: number; branchId: number; branchName: string; title?: string; manager: boolean; printOperator: boolean;
                        canCreateDmsSpace: boolean; printApproverId?: number; printApproverName?: string };
type AdminUserRow = { id: number; firstName: string; lastName: string; fullName: string; email: string; phone?: string;
                      discipline?: string; status: 'ACTIVE'|'PASSIVE'; systemAdmin: boolean; passwordSet: boolean;
                      lastLoginAt?: string; membership?: MembershipInfo };   // membership: AKTİF yıldaki atama
type MembershipInput = { branchId: number; title?: string; manager: boolean; printOperator: boolean;
                         canCreateDmsSpace: boolean; printApproverId?: number };
type UserInput = { firstName: string; lastName: string; email: string; phone?: string; discipline?: string;
                   systemAdmin: boolean; membership?: MembershipInput };
type ImpactItem = { label: string; count: number };
```

**`GET /api/admin/users?q=&status=ACTIVE|PASSIVE|ALL&branchId=&assigned=&passwordSet=&page=&size=25`**

- Yanıt: `PageResponse<AdminUserRow>`
- `status` varsayılanı `ACTIVE`'dir.
- `branchId` ve `assigned` filtreleri aktif yıldaki atamaya göre çalışır.
- `passwordSet=false` filtresi henüz şifre belirlememiş kişileri listeler.
- Sıralama Türkçe alfabeye göredir.

**`GET /api/admin/users/{id}`**

- Yanıt: `AdminUserRow`

**`GET /api/admin/users/lookup?q=&seasonId=`**

- Yanıt: `DirUser[]` (en fazla 20).
- `q` en az 2 karakterdir. Yalnızca `ACTIVE` kişiler döner.
- `seasonId` verilmişse yalnızca o yıla atananlar döner. Yönetim formlarındaki onaylayıcı ve devralan seçicileri bunu kullanır.

**`GET /api/admin/titles`**

- Yanıt: `string[]` — mevcut atamalardaki farklı unvanlar, Türkçe sıralı. Unvan alanında öneri listesi (`datalist`) olarak kullanılır.

**`POST /api/admin/users`**

- İstek: `UserInput`
- Yanıt: 201 `{ user: AdminUserRow, mailSent: boolean }`
- Doğrulama §7'ye göre yapılır. Ek hatalar:

| Durum | Hata |
|---|---|
| E-posta kayıtlı | `USER_EMAIL_TAKEN` (alan: email) |
| `membership` verildi ama aktif yıl yok | `SEASON_NONE_ACTIVE` |
| Şube pasif | `BRANCH_INACTIVE` (alan: membership.branchId) |
| Onaylayıcı aktif yıla atanmamış veya pasif | `SEASON_APPROVER_NOT_MEMBER` (alan: membership.printApproverId) |

- İşleyiş:
  1. Kullanıcı `password_hash = NULL` ile oluşturulur.
  2. `membership` verildiyse aktif yıla atama oluşturulur.
  3. SETUP token (72 saat) üretilir.
  4. E-posta **senkron** gönderilir. `MailException` yakalanırsa `mailSent = false` döner ve `log.warn` yazılır.
- Denetim: `USER_CREATED`

**`PUT /api/admin/users/{id}`**

- İstek: `UserInput`
- Yanıt: `AdminUserRow`
- `membership` null ise aktif yıl ataması değişmez. Nesne verilmişse atama oluşturulur ya da güncellenir (upsert). Atamayı kaldırmak yalnızca yıl ekranından yapılır.
- Ek hatalar:

| Durum | Hata |
|---|---|
| E-posta başka kişide kayıtlı | `USER_EMAIL_TAKEN` |
| Kişi kendi Sistem Yöneticisi işaretini kaldırıyor | `USER_CANNOT_REMOVE_OWN_ADMIN` |
| Son aktif Sistem Yöneticisinin işareti kaldırılıyor | `USER_LAST_ADMIN` |
| Pasif kişiye atama yapılıyor | `SEASON_USER_PASSIVE` |
| Onaylayıcı kişinin kendisi | `SEASON_APPROVER_SELF` |

- Denetim: `USER_UPDATED`, detayda `{ changed: [alanAdları] }`

**`POST /api/admin/users/{id}/send-setup-link`**

- Yanıt: 204
- Kişi pasifse `USER_PASSIVE`.
- Kullanılmamış tüm token'lar geçersiz kılınır. 72 saatlik SETUP token üretilir ve e-posta senkron gönderilir.
- Gönderilemezse 502 `MAIL_SEND_FAILED` döner.
- Denetim: `USER_SETUP_LINK_SENT`

**`GET /api/admin/users/{id}/deactivation-check`**

- Yanıt: `{ items: ImpactItem[], successorRequired: boolean }`
- `items`, çekirdeğin ve tüm hook'ların sonuçlarını birleştirir; sayısı 0 olanlar listeden çıkarılır.
- `successorRequired = items.length > 0`

**`POST /api/admin/users/{id}/deactivate`**

- İstek: `{ successorUserId?: number }`
- Yanıt: 204
- Hatalar:

| Durum | Hata |
|---|---|
| Kişi kendisi | `USER_CANNOT_DEACTIVATE_SELF` |
| Zaten pasif | `USER_ALREADY_PASSIVE` |
| Son aktif Sistem Yöneticisi | `USER_LAST_ADMIN` |
| Devralan zorunlu ama verilmedi | `USER_SUCCESSOR_REQUIRED` (alan: successorUserId; mesajda etkiler listelenir) |
| Devralan kişinin kendisi, pasif ya da aktif yıla atanmamış | `USER_SUCCESSOR_INVALID` |

- İşleyiş §2.6'dadır.
- Denetim: `USER_DEACTIVATED`, detayda `{ successorUserId }`

**`POST /api/admin/users/{id}/reactivate`**

- Yanıt: 204
- `status = ACTIVE`, sayaç ve kilit sıfırlanır.
- Kişi zaten aktifse `USER_ALREADY_ACTIVE`.
- Denetim: `USER_REACTIVATED`

### 6.7 CSV ile toplu personel ekleme

**`GET /api/admin/users/import-template`**

- `personel-sablon.csv` dosyası indirilir: UTF-8 BOM, `;` ayraç.
- İçerik:
  ```
  ad;soyad;eposta;telefon;brans;sube;unvan
  Ayşe;Yılmaz;ayse.yilmaz@okul.k12.tr;05321234567;Matematik;Merkez Kampüs;Öğretmen
  ```

**`POST /api/admin/users/import`** (multipart: `file`, `seasonId`)

- Yanıt: 200 `{ created: number }`

Doğrulama (her adımda ilk hatada durur, satır hataları hariç):

1. **Dosya:** `.csv` uzantılı ve en fazla 2 MB olmalı. Değilse `IMPORT_FILE_INVALID`.
2. **Yıl:** Durumu `DRAFT` veya `ACTIVE` olmalı. Değilse `SEASON_CLOSED_READ_ONLY`.
3. **Kodlama:** Dosya katı UTF-8 olarak çözülür (`CodingErrorAction.REPORT`). Başarısız olursa `windows-1254` ile çözülür. Excel'in Türkçe CSV çıktısı windows-1254'tür. Baştaki BOM atılır.
4. **Ayraç:** İlk satırda `;` varsa ayraç `;`, yoksa `,` kabul edilir. Tırnaklı alanlar RFC 4180'e göre okunur (`common/util/Csv`).
5. **Başlık:** Başlık hücreleri `fold` edildikten sonra tam olarak `ad, soyad, eposta, telefon, brans, sube, unvan` olmalı. Değilse `IMPORT_HEADER_INVALID`.
6. **Satır sayısı:** Tamamen boş satırlar atlanır. Hiç veri satırı yoksa `IMPORT_EMPTY`, 1000'den fazlaysa `IMPORT_TOO_MANY_ROWS`.
7. **Satır kontrolleri:** Her satır §7 kurallarıyla doğrulanır. Ek kurallar:

| Kontrol | Hata satırı metni |
|---|---|
| `sube` zorunludur ve aktif bir şubenin `name_key` değeriyle eşleşmelidir | "Şube bulunamadı: {değer}" |
| Aynı e-posta dosyada birden fazla geçmemeli | "Bu e-posta dosyada birden fazla kez geçiyor." |
| E-posta veritabanında kayıtlı olmamalı | "Bu e-posta ile kayıtlı personel var." |

Satır hataları:

- Bir veya daha fazla hata varsa 422 `IMPORT_INVALID` döner ve **hiçbir kayıt oluşturulmaz**.
- `details` dizisi en fazla 100 satır içerir. Fazlası varsa son öğe "… ve {n} hata daha." olur.
- Her satırın biçimi: `"{satırNo}. satır: {mesaj}"`. Başlık satırı 1. satırdır.

Başarılı yükleme:

1. Tüm kullanıcılar ve atamalar tek bir transaction içinde oluşturulur (yalnızca şube ve unvan dolu; diğer işaretler false).
2. Commit sonrasında `@Async` bir iş başlar. Her kişi için SETUP token üretilir ve e-posta gönderilir.
3. İş bitince yükleyen yöneticiye bildirim gider:

| Alan | Değer |
|---|---|
| type | `CORE_IMPORT_MAIL_DONE` |
| title | "Toplu personel e-postaları gönderildi" |
| body | "{ok} e-posta gönderildi, {fail} gönderilemedi." |
| link | `/yonetim/personel?parola=yok` |

Denetim: `USER_IMPORTED`, detayda `{ count, seasonId }`.

### 6.8 Eğitim-öğretim yılları (`core/season`)

```ts
type SeasonRow = { id: number; name: string; startDate: string; endDate: string; status: 'DRAFT'|'ACTIVE'|'CLOSED';
                   memberCount: number; activatedAt?: string; closedAt?: string };
type MembershipRow = MembershipInfo & { userId: number; fullName: string; email: string; userStatus: 'ACTIVE'|'PASSIVE' };
```

**Yıl işlemleri:**

| Uç nokta | İstek | Yanıt | Kurallar ve hatalar |
|---|---|---|---|
| `GET /api/admin/seasons` | — | `SeasonRow[]` | `name DESC` sıralı |
| `GET /api/admin/seasons/{id}` | — | `SeasonRow` | |
| `POST /api/admin/seasons` | `{ name, startDate, endDate }` | 201 `SeasonRow` | `SEASON_NAME_INVALID` (alan: name), `SEASON_NAME_TAKEN`, `SEASON_NAME_NOT_NEWER`, `SEASON_DATES_INVALID` (alan: endDate), `SEASON_DRAFT_EXISTS({ad})`. Denetim: `SEASON_CREATED`. |
| `PUT /api/admin/seasons/{id}` | aynı | `SeasonRow` | CLOSED ise `SEASON_CLOSED_READ_ONLY`; ad kuralları aynıdır (`NOT_NEWER` kontrolü yalnızca DRAFT için yapılır). Denetim: `SEASON_UPDATED`. |
| `DELETE /api/admin/seasons/{id}` | — | 204 | DRAFT değilse `SEASON_NOT_DRAFT`. Atamalar cascade ile silinir. Denetim: `SEASON_DELETED`. |

**Atama listesi ve tekil atama:**

| Uç nokta | İstek | Yanıt | Kurallar ve hatalar |
|---|---|---|---|
| `GET /api/admin/seasons/{id}/memberships?q=&branchId=&filter=manager\|printOperator\|dmsCreator\|noApprover&page=&size=50` | — | `PageResponse<MembershipRow>` | Türkçe sıralı |
| `GET /api/admin/seasons/{id}/unassigned-users?q=` | — | `DirUser[]` | Bu yıla ataması olmayan ACTIVE kişiler, en fazla 1000 |
| `POST /api/admin/seasons/{id}/memberships` | `{ userId } & MembershipInput` | 201 `MembershipRow` | Yıl CLOSED ise `SEASON_CLOSED_READ_ONLY`. Ayrıca `SEASON_USER_PASSIVE`, `SEASON_MEMBER_EXISTS`, `BRANCH_INACTIVE`, `SEASON_APPROVER_NOT_MEMBER`, `SEASON_APPROVER_SELF`. Denetim: `SEASON_MEMBERSHIP_ADDED`. |
| `PUT /api/admin/seasons/{id}/memberships/{mid}` | `MembershipInput` | `MembershipRow` | Aynı kurallar. Denetim: `SEASON_MEMBERSHIP_UPDATED`. |
| `DELETE /api/admin/seasons/{id}/memberships/{mid}` | — | 204 | §2.5'teki engeller. Yıl ACTIVE ise kişinin oturumları kapatılır. Denetim: `SEASON_MEMBERSHIP_REMOVED`. |

**Toplu işlemler:**

| Uç nokta | İstek | Yanıt | Kurallar ve hatalar |
|---|---|---|---|
| `POST /api/admin/seasons/{id}/memberships/batch` | `{ userIds: number[] (1–500), branchId }` | `{ created }` | Zaten atanmış olanlar atlanır. Pasif kişi varsa `SEASON_USER_PASSIVE` (detaylarda adlar). Denetim: tek kayıt, `SEASON_MEMBERSHIP_ADDED`, detayda `{ count }`. |
| `POST /api/admin/seasons/{id}/memberships/bulk-update` | `{ membershipIds (1–500), branchId?, printApproverId?, clearApprover? }` | `{ updated, skippedSelf }` | Hiçbir alan seçilmediyse `BULK_NOTHING_TO_UPDATE`. `printApproverId` ile `clearApprover` birlikte verilirse `BULK_CONFLICT`. Onaylayıcının kendi satırı atlanır ve `skippedSelf` değerine eklenir. Onaylayıcı yıla atanmış olmalıdır. |
| `POST /api/admin/seasons/{id}/memberships/bulk-delete` | `{ membershipIds (1–500) }` | `{ deleted }` | Satırlardan biri bile engelliyse hiçbiri silinmez; `SEASON_MEMBER_REMOVAL_BLOCKED` döner ve detaylarda "{ad}: {gerekçe}" satırları yer alır. |

**Önceki yıldan kopyalama — `POST /api/admin/seasons/{id}/memberships/copy`**

- İstek: `{ sourceSeasonId, userIds: number[] }`
- Yanıt: `{ created, skippedExisting, approverCleared }`
- Hedef yıl DRAFT veya ACTIVE olmalı; kaynak yıl hedeften farklı olmalı.
- Kopyalanan bilgiler: şube, unvan, üç işaret ve onaylayıcı.
- Onaylayıcı ancak şu durumda korunur: kişi ACTIVE ve hedef yılda zaten atanmış ya da bu kopyalama setinde. Aksi halde null yapılır ve `approverCleared` sayısı artar.
- Kaynak şubesi pasif olan satırlar varsa `SEASON_COPY_BRANCH_INACTIVE` döner (detaylarda "{ad}: şube {şube} pasif").
- Pasif kişi varsa `SEASON_USER_PASSIVE`.
- Denetim: `SEASON_MEMBERSHIPS_COPIED`, detayda `{ count, sourceSeasonId }`

**Aktifleştirme önizlemesi — `GET /api/admin/seasons/{id}/activation-preview`**

- Yanıt: `{ canActivate: boolean, blockers: string[], items: string[] }`
- Engeller (`blockers`):
  - "Yalnızca hazırlık aşamasındaki yıl aktifleştirilebilir."
  - "Bu yıla hiç personel atanmamış."
- Bilgi maddeleri (`items`), sırayla:
  1. "{eski} yılı kapanacak; o yılın içerikleri yalnızca görüntülenebilir olacak." (aktif yıl varsa)
  2. "{yeni} yılına atanmış {n} personel giriş yapabilecek."
  3. "{eski} yılında olup {yeni} yılına atanmamış {m} personel artık giriş yapamayacak." (m > 0 ise)
  4. "Fotokopi onaylayıcısı olmayan {k} personelin talepleri onaysız olarak fotokopi birimine düşecek." (k > 0 ise)
  5. Ardından tüm `SeasonActivationHook.preview(eskiId)` metinleri

**Aktifleştirme — `POST /api/admin/seasons/{id}/activate`**

- Yanıt: `SeasonRow`
- İşleyiş:
  1. `SELECT ... FROM season FOR UPDATE` ile tüm yıllar kilitlenir.
  2. Engel kontrolleri tekrar yapılır; engel varsa `SEASON_ACTIVATION_BLOCKED` döner (detaylarda engeller).
  3. Eski yıl `CLOSED` (`closed_at = now`), yeni yıl `ACTIVE` (`activated_at = now`) olur.
  4. Eski yıl varsa `hooks.onActivated(eski, yeni)` çağrılır.
- Denetim: `SEASON_ACTIVATED`

### 6.9 Gruplar (`core/group`)

```ts
type GroupRow = { id: number; name: string; description?: string; memberCount: number; archived: boolean; createdAt: string };
type GroupMember = { userId: number; fullName: string; title?: string; branchName?: string; status: 'ACTIVE'|'PASSIVE' };
type GroupDetail = GroupRow & { members: GroupMember[] };
```

`/api/groups/mine` dışındaki tüm uçlar `canManageGroups` yetkisi ister; yetki yoksa `GROUP_MANAGE_FORBIDDEN`.

| Uç nokta | İstek | Yanıt | Kurallar ve hatalar |
|---|---|---|---|
| `GET /api/groups/mine` | — | `DirGroup[]` | Üyesi olunan, arşivlenmemiş gruplar |
| `GET /api/groups?q=&archived=false` | — | `GroupRow[]` | |
| `GET /api/groups/{id}` | — | `GroupDetail` | `GROUP_NOT_FOUND` |
| `POST /api/groups` | `{ name, description?, memberIds: number[] (0–1000) }` | 201 `GroupDetail` | `GROUP_NAME_TAKEN` (alan: name). Pasif ya da bilinmeyen kişi varsa `GROUP_MEMBER_INVALID`. Olay: `GroupCreatedEvent`. Denetim: `GROUP_CREATED`. |
| `PUT /api/groups/{id}` | `{ name, description? }` | `GroupDetail` | Arşivliyse `GROUP_ARCHIVED`. Olay: `GroupUpdatedEvent`. Denetim: `GROUP_UPDATED`. |
| `POST /api/groups/{id}/members` | `{ userIds (1–500) }` | `GroupDetail` | Zaten üye olanlar atlanır. `GROUP_ARCHIVED`, `GROUP_MEMBER_INVALID`. Olay: `GroupMembersAddedEvent` (yalnızca yeni eklenenlerle). Denetim: `GROUP_MEMBERS_ADDED`. |
| `DELETE /api/groups/{id}/members/{userId}` | — | 204 | `GROUP_ARCHIVED`. Olay: `GroupMembersRemovedEvent`. Denetim: `GROUP_MEMBER_REMOVED`. |
| `POST /api/groups/{id}/archive` | — | 204 | Olay: `GroupArchivedEvent(id, true)`. Denetim: `GROUP_ARCHIVED`. |
| `POST /api/groups/{id}/unarchive` | — | 204 | Olay: `GroupArchivedEvent(id, false)`. Denetim: `GROUP_UNARCHIVED`. |

### 6.10 Denetim kaydı (`common/audit`)

| Uç nokta | Yanıt | Kurallar |
|---|---|---|
| `GET /api/admin/audit?from=&to=&actorId=&action=&page=&size=50` | `PageResponse<AuditRow>` | `from` ve `to` LocalDate tipindedir ve iki uç da dahildir; varsayılan son 7 gündür. Aralık en fazla 366 gün olabilir; aşılırsa `AUDIT_RANGE_TOO_LONG`. Sıralama `created_at DESC`. |
| `GET /api/admin/audit/actions` | `{ code, label }[]` | `SELECT DISTINCT action, action_label FROM audit_log ORDER BY action_label` |

`AuditRow` yapısı: `{ id, createdAt, actorId?, actorName?, ip?, action, actionLabel, entityType?, entityId?, detail? }`. `detail` ham JSON olarak döner.

`AuditService` imzası:

```java
void log(Long actorUserId, AuditAction action, String entityType, Object entityId, Map<String, ?> detail);
// AuditAction: interface { String name(); String label(); } — her modülün enum'u uygular. IP, RequestContextHolder'dan alınır.
```

**Çekirdeğin denetim işlemleri (`CoreAuditAction`, kod → etiket):**

| Kod | Etiket |
|---|---|
| AUTH_LOGIN_SUCCESS | Giriş yapıldı |
| AUTH_LOGIN_FAILED | Hatalı giriş denemesi |
| AUTH_ACCOUNT_LOCKED | Hesap geçici olarak kilitlendi |
| AUTH_PASSWORD_CHANGED | Şifre değiştirildi |
| AUTH_PASSWORD_SET | Şifre belirlendi |
| KVKK_ACCEPTED | Aydınlatma metni onaylandı |
| USER_CREATED | Personel eklendi |
| USER_UPDATED | Personel güncellendi |
| USER_IMPORTED | Toplu personel yüklendi |
| USER_SETUP_LINK_SENT | Şifre bağlantısı gönderildi |
| USER_DEACTIVATED | Personel pasifleştirildi |
| USER_REACTIVATED | Personel aktifleştirildi |
| BRANCH_CREATED | Şube eklendi |
| BRANCH_UPDATED | Şube güncellendi |
| BRANCH_DEACTIVATED | Şube pasifleştirildi |
| BRANCH_ACTIVATED | Şube aktifleştirildi |
| SEASON_CREATED | Eğitim-öğretim yılı oluşturuldu |
| SEASON_UPDATED | Eğitim-öğretim yılı güncellendi |
| SEASON_DELETED | Eğitim-öğretim yılı silindi |
| SEASON_ACTIVATED | Eğitim-öğretim yılı aktifleştirildi |
| SEASON_MEMBERSHIP_ADDED | Yıla personel atandı |
| SEASON_MEMBERSHIP_UPDATED | Yıl ataması güncellendi |
| SEASON_MEMBERSHIP_REMOVED | Personel yıldan çıkarıldı |
| SEASON_MEMBERSHIPS_COPIED | Önceki yıldan atama kopyalandı |
| GROUP_CREATED | Grup oluşturuldu |
| GROUP_UPDATED | Grup güncellendi |
| GROUP_MEMBERS_ADDED | Gruba üye eklendi |
| GROUP_MEMBER_REMOVED | Gruptan üye çıkarıldı |
| GROUP_ARCHIVED | Grup arşivlendi |
| GROUP_UNARCHIVED | Grup arşivden çıkarıldı |

### 6.11 Dosya yükleme

`POST /api/files` uç noktası `00-mimari.md` §9'da tanımlıdır. Çekirdek görevi C-B07'de yazılır.

---

## §7 Çekirdek hata kodları (`CoreErrorCode`)

### Kimlik doğrulama

| code | HTTP | message |
|---|---|---|
| AUTH_INVALID_CREDENTIALS | 401 | E-posta adresi veya şifre hatalı. |
| AUTH_ACCOUNT_LOCKED | 403 | Çok sayıda hatalı giriş denemesi yapıldı. Lütfen {0} dakika sonra tekrar deneyin veya "Şifremi unuttum" bağlantısını kullanın. |
| AUTH_PASSWORD_NOT_SET | 400 | Hesabınız için henüz şifre belirlenmemiş. E-postanıza gönderilen bağlantıyla şifrenizi belirleyin ya da "Şifremi unuttum" bağlantısını kullanın. |
| AUTH_ACCOUNT_PASSIVE | 401 | Hesabınız pasif durumda. Okul yönetimiyle iletişime geçin. |
| AUTH_NO_ACTIVE_SEASON | 403 | Şu anda aktif bir eğitim-öğretim yılı bulunmuyor. Okul yönetimiyle iletişime geçin. |
| AUTH_NO_SEASON_MEMBERSHIP | 403 | {0} eğitim-öğretim yılına atanmamışsınız. Okul yönetimiyle iletişime geçin. |
| AUTH_TOKEN_INVALID | 400 | Bu bağlantı geçersiz veya süresi dolmuş. "Şifremi unuttum" sayfasından yeni bir bağlantı isteyin. |
| AUTH_CURRENT_PASSWORD_WRONG | 400 | Mevcut şifreniz hatalı. |
| AUTH_PASSWORD_SAME | 400 | Yeni şifre mevcut şifrenizle aynı olamaz. |

### Yetki ve genel

| code | HTTP | message |
|---|---|---|
| CORE_ADMIN_ONLY | 403 | Bu işlem yalnızca sistem yöneticisi tarafından yapılabilir. |
| CORE_MANAGER_ONLY | 403 | Bu işlem yalnızca yöneticiler tarafından yapılabilir. |
| PUSH_SUBSCRIPTION_INVALID | 400 | Bildirim aboneliği kaydedilemedi. Sayfayı yenileyip tekrar deneyin. |
| MAIL_SEND_FAILED | 502 | E-posta gönderilemedi. E-posta sunucusuna ulaşılamıyor; birkaç dakika sonra tekrar deneyin. |
| BULK_NOTHING_TO_UPDATE | 400 | Değiştirilecek bir bilgi seçin. |
| BULK_CONFLICT | 400 | Aynı anda hem onaylayıcı atayıp hem kaldıramazsınız. |
| AUDIT_RANGE_TOO_LONG | 400 | Tarih aralığı en fazla 1 yıl olabilir. |

### Hedef seçimi

| code | HTTP | message |
|---|---|---|
| TARGET_REQUIRED | 400 | En az bir alıcı seçin. |
| TARGET_TYPE_NOT_ALLOWED | 403 | "{0}" alıcısını seçme yetkiniz yok. |
| TARGET_INVALID | 400 | Seçilen alıcılardan biri artık geçerli değil (pasif, arşivlenmiş ya da silinmiş olabilir). Alıcıları yeniden seçin. |

### Personel

| code | HTTP | message |
|---|---|---|
| USER_NOT_FOUND | 404 | Personel bulunamadı. |
| USER_EMAIL_TAKEN | 409 | Bu e-posta adresiyle kayıtlı bir personel var. |
| USER_PASSIVE | 400 | Pasif personele bu işlem yapılamaz. |
| USER_ALREADY_PASSIVE | 409 | Bu personel zaten pasif. |
| USER_ALREADY_ACTIVE | 409 | Bu personel zaten aktif. |
| USER_CANNOT_DEACTIVATE_SELF | 400 | Kendi hesabınızı pasifleştiremezsiniz. |
| USER_CANNOT_REMOVE_OWN_ADMIN | 400 | Kendi sistem yöneticisi yetkinizi kaldıramazsınız. |
| USER_LAST_ADMIN | 400 | Sistemde en az bir aktif sistem yöneticisi kalmalıdır. |
| USER_SUCCESSOR_REQUIRED | 400 | Bu personelin devredilmesi gereken sorumlulukları var: {0}. Lütfen devralacak personeli seçin. |
| USER_SUCCESSOR_INVALID | 400 | Devralacak personel, pasifleştirilen kişiden farklı, aktif ve bu yıla atanmış biri olmalıdır. |

### Toplu yükleme

| code | HTTP | message |
|---|---|---|
| IMPORT_FILE_INVALID | 400 | Lütfen .csv uzantılı ve en fazla 2 MB boyutunda bir dosya seçin. |
| IMPORT_HEADER_INVALID | 400 | Dosyanın ilk satırı şablondaki başlıklarla aynı olmalıdır: ad;soyad;eposta;telefon;brans;sube;unvan |
| IMPORT_EMPTY | 400 | Dosyada personel satırı bulunamadı. |
| IMPORT_TOO_MANY_ROWS | 400 | Bir seferde en fazla 1000 personel yüklenebilir. |
| IMPORT_INVALID | 422 | Dosyada düzeltilmesi gereken satırlar var. Hiçbir kayıt eklenmedi. |

### Şube

| code | HTTP | message |
|---|---|---|
| BRANCH_NOT_FOUND | 404 | Şube bulunamadı. |
| BRANCH_NAME_TAKEN | 409 | Bu adla kayıtlı bir şube var. |
| BRANCH_INACTIVE | 400 | Seçilen şube pasif durumda. Başka bir şube seçin. |
| BRANCH_IN_USE | 409 | Bu şubeye atanmış {0} personel var. Önce bu personeli başka bir şubeye taşıyın. |
| BRANCH_LAST_ACTIVE | 400 | En az bir aktif şube bulunmalıdır. |

### Eğitim-öğretim yılı

| code | HTTP | message |
|---|---|---|
| SEASON_NOT_FOUND | 404 | Eğitim-öğretim yılı bulunamadı. |
| SEASON_NAME_INVALID | 400 | Yıl adı 2026-2027 biçiminde olmalı ve ikinci yıl birinciden bir fazla olmalıdır. |
| SEASON_NAME_TAKEN | 409 | Bu eğitim-öğretim yılı zaten var. |
| SEASON_NAME_NOT_NEWER | 400 | Yeni yıl, mevcut yıllardan daha sonraki bir yıl olmalıdır. |
| SEASON_DATES_INVALID | 400 | Bitiş tarihi başlangıç tarihinden sonra olmalıdır. |
| SEASON_DRAFT_EXISTS | 409 | Hazırlık aşamasında bir yıl zaten var ({0}). Önce onu aktifleştirin veya silin. |
| SEASON_NOT_DRAFT | 409 | Bu işlem yalnızca hazırlık aşamasındaki yıl için yapılabilir. |
| SEASON_CLOSED_READ_ONLY | 409 | Kapanmış bir yılın bilgileri değiştirilemez. |
| SEASON_ACTIVATION_BLOCKED | 409 | Yıl aktifleştirilemiyor. |
| SEASON_MEMBERSHIP_NOT_FOUND | 404 | Yıl ataması bulunamadı. |
| SEASON_MEMBER_EXISTS | 409 | Bu personel bu yıla zaten atanmış. |
| SEASON_USER_PASSIVE | 400 | Pasif personel yıla atanamaz. |
| SEASON_APPROVER_NOT_MEMBER | 400 | Seçilen onaylayıcı bu yıla atanmış aktif bir personel olmalıdır. |
| SEASON_APPROVER_SELF | 400 | Personel kendi kendisinin onaylayıcısı olamaz. |
| SEASON_MEMBER_IS_APPROVER | 409 | Bu kişi bu yıl {0} personelin fotokopi onaylayıcısı. Önce onların onaylayıcısını değiştirin. |
| SEASON_MEMBER_REMOVAL_BLOCKED | 409 | Seçilen personel yıldan çıkarılamıyor. |
| SEASON_COPY_BRANCH_INACTIVE | 400 | Bazı personelin önceki yıldaki şubesi pasif. Önce şubeyi aktifleştirin veya bu kişileri seçimden çıkarın. |

### Grup

| code | HTTP | message |
|---|---|---|
| GROUP_NOT_FOUND | 404 | Grup bulunamadı. |
| GROUP_MANAGE_FORBIDDEN | 403 | Grupları yalnızca yöneticiler düzenleyebilir. |
| GROUP_NAME_TAKEN | 409 | Bu adla kayıtlı bir grup var. |
| GROUP_ARCHIVED | 409 | Bu grup arşivlenmiş; değişiklik yapılamaz. Önce arşivden çıkarın. |
| GROUP_MEMBER_INVALID | 400 | Seçilen kişilerden bazıları aktif personel değil. |

`details` alanı dolu olan hatalarda frontend toast'ın altında ilk 5 maddeyi gösterir. Diyalog içinde ise listenin tamamı gösterilir.

---

## §8 Modül genişleme noktaları (`core/spi`)

```java
public record ImpactItem(String label, int count) {}          // label örn: "personelin fotokopi onaylayıcısı" → "12 personelin fotokopi onaylayıcısı"

public interface UserDeactivationHook {                       // çekirdek + Fotokopi + Doküman uygular
  List<ImpactItem> describe(long userId);
  void transfer(long userId, Long successorUserId);           // aynı transaction içinde çalışır
}
public interface SeasonActivationHook {                       // Fotokopi uygular
  List<String> preview(long closingSeasonId);                 // tam cümleler
  void onActivated(long closedSeasonId, long newSeasonId);
}
public interface MembershipRemovalHook {                      // Fotokopi uygular
  List<String> blockers(long seasonId, long userId);          // örn: "Onayını bekleyen 3 fotokopi talebi var."
}
public interface BadgeProvider { Map<String, Integer> badges(UserContext me); }

// ApplicationEvent'ler (record):
GroupCreatedEvent(long groupId, String name, List<Long> memberIds)
GroupUpdatedEvent(long groupId, String name)
GroupMembersAddedEvent(long groupId, List<Long> userIds)
GroupMembersRemovedEvent(long groupId, List<Long> userIds)
GroupArchivedEvent(long groupId, boolean archived)
UserDeactivatedEvent(long userId)
```

Kurallar:

- Çekirdek, `UserDeactivationHook` arayüzünü kendisi de uygular: onaylayıcısı bu kişi olan atamalar, ACTIVE ve DRAFT yıllarda devralana geçer. Devralan o atamanın sahibiyse onaylayıcı null yapılır.
- Çekirdek, `BadgeProvider` arayüzünü `notifications` anahtarıyla uygular.
- Hook'lar `List<XxxHook>` olarak enjekte edilir. Liste boş olabilir; modüller henüz yazılmadıysa da çalışır.

---

## §9 Açılış işleri ve zamanlanmış işler

### `BootstrapAdminRunner` (`ApplicationRunner`)

- Hiç aktif Sistem Yöneticisi yoksa ve `app.bootstrap-admin.email` ile `password` dolu ise bir yönetici oluşturulur:
  - Ad: "Sistem", Soyad: "Yöneticisi", `systemAdmin = true`
  - Şifre hash'lenerek kaydedilir
- Log mesajı: "Başlangıç sistem yöneticisi oluşturuldu: {email}".
- Bir yönetici zaten varsa hiçbir şey yapılmaz.

### `DevDataSeeder` (yalnızca `dev` profili ve `app_user` tablosu boşsa)

Tüm şifreler `Test1234`'tür ve tüm kullanıcılar KVKK'yı onaylamış sayılır. Gruplar `GroupService` üzerinden oluşturulur; böylece olaylar tetiklenir.

**Şubeler ve yıl:**

- Şubeler: "Merkez Kampüs", "Kuzey Kampüs"
- Yıl: "2026-2027", ACTIVE, 2026-09-01 – 2027-08-31

**Kullanıcılar:**

| E-posta | Ad Soyad | Şube | Unvan / Branş | İşaretler | Onaylayıcı |
|---|---|---|---|---|---|
| admin@okul.test | Sistem Yöneticisi | Merkez | BT Sorumlusu | systemAdmin | — |
| mudur@okul.test | Ayşe Yılmaz | Merkez | Müdür | manager | — |
| my.kuzey@okul.test | Mehmet Demir | Kuzey | Müdür Yardımcısı | manager | — |
| zumre.mat@okul.test | Elif Kaya | Merkez | Matematik Zümre Başkanı / Matematik | canCreateDmsSpace | mudur |
| ogretmen1@okul.test | Ali Çelik | Merkez | Öğretmen / Matematik | — | zumre.mat |
| ogretmen2@okul.test | Zeynep Şahin | Kuzey | Öğretmen / Türkçe | — | my.kuzey |
| ogretmen3@okul.test | Burak Öztürk | Merkez | Öğretmen / Fen Bilimleri | — | (yok) |
| fotokopi.merkez@okul.test | Hasan Arslan | Merkez | Teksir Görevlisi | printOperator | — |
| fotokopi.kuzey@okul.test | Fatma Koç | Kuzey | Teksir Görevlisi | printOperator | — |
| pasif@okul.test | Pasif Personel | — | — | PASSIVE | — |
| atanmamis@okul.test | Atanmamış Personel | — | — | ACTIVE, yıla atanmamış | — |

**Gruplar:**

| Grup | Üyeler |
|---|---|
| Matematik Zümresi | Elif, Ali |
| Fen Zümresi | Burak |
| Yönetim Kurulu | Ayşe, Mehmet |

### `MaintenanceJobs` (`@Scheduled`, `zone = "Europe/Istanbul"`)

| Saat | İş |
|---|---|
| 03:00 | 24 saatten eski, bağlanmamış dosyalar: fiziksel dosya silinir, `deleted_at = now` |
| 03:10 | `app.cleanup.notification-retention-days` günden eski bildirimler silinir |
| 03:20 | Süresi 7 günden uzun süredir dolmuş `auth_token` kayıtları silinir |

---

## §10 Ekranlar

Tasarım ve UX kuralları `00-mimari.md` §12'dedir. Aşağıda her ekranın içeriği, metinleri ve davranışı yer alır.

### S-1 Giriş (`/giris`)

**Yerleşim:**

- Ortada en fazla 400px genişliğinde bir kart.
- Kartın üstünde logo ve kurum adı, altında "İç İletişim Sistemi" yazısı.

**Alanlar:**

- "E-posta" (`type=email`, `autoComplete=username`)
- "Şifre" (`type=password`, göz ikonuyla göster/gizle, `autoComplete=current-password`)

**Butonlar ve bağlantılar:**

- "Giriş Yap" (tam genişlik)
- Altta "Şifremi unuttum" bağlantısı

**Hata gösterimi:**

- Hata mesajı formun üstünde kırmızı bir `Alert` içinde gösterilir.
- `?neden=` parametresi varsa sayfa açılışında ilgili mesaj sarı `Alert` ile gösterilir. Mesajlar §7'deki kodlara karşılık gelir; `UNAUTHENTICATED` için "Oturumunuz sona erdi. Lütfen tekrar giriş yapın." yazılır.

**Başarılı giriş sonrası yönlendirme:**

- `?next=` varsa ve `/` ile başlıyorsa oraya gidilir.
- Yoksa kullanıcı yıla atanmışsa `/akis`, değilse `/yonetim/personel` açılır.

**Oturum zaten açıksa** sayfa doğrudan yönlendirir.

### S-2 Şifremi unuttum (`/sifremi-unuttum`)

- Tek alan: "E-posta". Buton: "Bağlantı Gönder".
- Gönderimden sonra kartın içeriği değişir: yeşil bir ikon ve backend'den gelen mesaj gösterilir. Altında "Giriş sayfasına dön" bağlantısı bulunur.

### S-3 Şifre belirle (`/sifre-belirle?token=`)

**Açılış:**

- Sayfa `GET password-token` ile token'ı kontrol eder.
- Token geçersizse: "Bağlantı geçersiz veya süresi dolmuş" kartı ve "Yeni bağlantı iste" butonu (`/sifremi-unuttum` sayfasına gider).

**Başlık:**

- SETUP için: "Hoş geldiniz {ad}! Hesabınız için bir şifre belirleyin."
- RESET için: "Yeni şifrenizi belirleyin."

**Alanlar:**

- "Yeni şifre"
- "Yeni şifre (tekrar)". Eşleşmezse: "Şifreler aynı değil."

Alanların altında canlı bir kontrol listesi bulunur:

- ✓ En az 8 karakter
- ✓ En az bir harf
- ✓ En az bir rakam

**Gönderim:**

- Buton: "Şifremi Kaydet".
- Başarıda `/giris?email=…` sayfasına gidilir ve toast gösterilir: "Şifreniz kaydedildi. Şimdi giriş yapabilirsiniz." Giriş formunda e-posta önceden doldurulur.

### S-4 AppShell ve KVKK

**Yerleşim:** `00-mimari.md` §12.2'deki menü ve çubuklar.

**`nav-items.ts`:**

| Öğe | Yol | İkon | Rozet anahtarı |
|---|---|---|---|
| Akış | /akis | Newspaper | — |
| Mesajlar | /mesajlar | MessageCircle | `chat` |
| Fotokopi | /fotokopi | Printer | `printApprovals` + `printQueue` |
| Dokümanlar | /dokumanlar | FolderOpen | — |
| Takvim | /takvim | CalendarDays | — |
| Yönetim | /yonetim | Settings | — (`canAccessAdmin` veya `canManageGroups` gerekir) |

- Modül öğeleri yalnızca `permissions.member` true ise görünür.
- Yer tutucu sayfalarda `EmptyState` gösterilir: ikon (`Hammer`) ve "Bu bölüm hazırlanıyor."

**KVKK penceresi:**

- `me.kvkkRequired` true ise kapatılamayan tam ekran bir pencere açılır.
- Başlık: "Kişisel Verilerin Korunması Hakkında Aydınlatma Metni".
- Metin kaydırılabilir alanda markdown olarak gösterilir. Basit bir dönüştürücü yeterlidir: paragraflar, başlıklar ve listeler; ek kütüphane kullanılmaz.
- Altta "Okudum, anladım" onay kutusu ve "Onayla ve Devam Et" butonu bulunur. Buton, kutu işaretlenmeden pasiftir.
- İkincil buton: "Çıkış Yap".

**Diğer bileşenler:**

- Yıl çipi `me.season.name` değerini gösterir. Tooltip: "Aktif eğitim-öğretim yılı".
- Sayfa yüklenirken (`useMe` loading) tam ekran ortalanmış bir logo ve spinner gösterilir.
- AppShell'in başlattığı bağlantılar: WebSocket, service worker kaydı ve "bildirimleri aç" kartı (§13).

### S-5 Bildirimler

**Zil:**

- Masaüstünde tıklanınca 380px genişliğinde, en fazla 480px yüksekliğinde bir Popover açılır.
- Mobilde `/bildirimler` sayfasına gider.

**Liste öğesi:**

- Modül ikonu (bildirimin `type` önekine göre: `PRINT_` → Printer, `FEED_` → Newspaper ve benzeri)
- Başlık: okunmamışsa kalın ve solunda mavi nokta
- Gövde: en fazla 2 satır
- `DateText relative`
- Tıklanınca bildirim okundu işaretlenir ve `link` adresine gidilir.

**Diğer:**

- Başlık satırında "Tümünü okundu işaretle" bağlantısı bulunur.
- Boş durum metni: "Henüz bildiriminiz yok."
- `/bildirimler` sayfası `InfiniteList` kullanır.
- **Gelen `NOTIFICATION` olayında:**
  - `['notifications']` ve `['me','badges']` sorguları yenilenir.
  - Sayfa görünürse (`document.visibilityState === 'visible'`) `toast(title, { description: body, action: { label: 'Aç', onClick: () => router.push(link) } })` gösterilir.

### S-6 Profil (`/profil`)

Sayfa kartlardan oluşur:

1. **"Bilgilerim"** (salt okunur): ad soyad, e-posta, telefon, branş, şube, unvan, fotokopi onaylayıcısı. Altta gri not: "Bilgilerinizde hata varsa okul yönetimine bildirin."
2. **"Bildirimler":**
   - "Bu cihazda bildirim al" anahtarı. Altındaki açıklama duruma göre değişir:
     - Açık: "Bu cihazda bildirimler açık."
     - İzin reddedilmiş: "Bildirim izni tarayıcı ayarlarından kapatılmış…" (bkz. `00-mimari.md` §13)
     - iOS ve standalone değil: "Bildirim almak için önce uygulamayı ana ekrana ekleyin."
   - Anahtar kapatılınca abonelik silinir.
3. **"Uygulamayı yükle":** Standalone modda değilse gösterilir; içeriği §13'teki gibidir.
4. **"Şifre değiştir":**
   - Alanlar: Mevcut şifre, Yeni şifre, Yeni şifre (tekrar); kontrol listesi S-3 ile aynı.
   - Buton: "Şifreyi Değiştir".
   - Başarı toast'ı: "Şifreniz değiştirildi. Diğer cihazlardaki oturumlarınız kapatıldı."
5. **"Çıkış Yap"** butonu (`outline`, `destructive` renk).

### S-7 Yönetim yerleşimi (`/yonetim/layout.tsx`)

- Başlık: "Yönetim".
- Altında sekmeler (mobilde yatay kaydırmalı). Yalnızca yetkili olunan sekmeler görünür:

| Sekme | Yol | Yetki |
|---|---|---|
| Personel | /yonetim/personel | canAccessAdmin |
| Eğitim-Öğretim Yılları | /yonetim/yillar | canAccessAdmin |
| Şubeler | /yonetim/subeler | canAccessAdmin |
| Gruplar | /yonetim/gruplar | canManageGroups |
| Denetim Kaydı | /yonetim/denetim | canAccessAdmin |

- `/yonetim` adresi, yetkili olunan ilk sekmeye yönlendirir.

### S-8 Personel (`/yonetim/personel`)

**Araç çubuğu:**

- Arama kutusu (placeholder "Ad, soyad veya e-posta"; 300 ms gecikmeli arama)
- Şube seçimi
- Durum seçimi: Aktif / Pasif / Tümü
- "Bu yıla atanmamışlar" onay kutusu
- Sağda: "Toplu Yükle" (`outline`) ve "Personel Ekle" (birincil)
- URL'de `parola=yok` varsa bir çip görünür: "Şifresini belirlememiş olanlar ✕"

**Tablo sütunları:**

| Sütun | İçerik |
|---|---|
| Ad Soyad | Avatar ve ad; altında gri e-posta |
| Şube | |
| Unvan | |
| Yetkiler | Rozetler: "Sistem Yöneticisi" (mor), "Yönetici" (mavi), "Fotokopi Sorumlusu" (yeşil), "Doküman Alanı Açabilir" (gri) |
| Onaylayıcı | |
| Durum | "Aktif" / "Pasif" rozeti, ayrıca gerekirse sarı "Şifre belirlenmedi" rozeti, yıla atanmamışsa gri "Bu yıla atanmamış" rozeti |
| Son giriş | `DateText` |
| (menü) | ⋯ menüsü |

**Satır menüsü:** Düzenle · Şifre bağlantısı gönder · Pasifleştir (kırmızı) veya Aktifleştir.

**Personel formu** (`ResponsiveDialog`; masaüstünde sağdan açılan `Sheet`, 520px):

- **Bölüm "Kişisel bilgiler":** Ad\*, Soyad\*, E-posta\*, Telefon (placeholder "05XX XXX XX XX"), Branş, ☐ "Sistem yöneticisi" (yardım: "Yönetim ekranlarının tamamına erişir.").
- **Bölüm "{aktif yıl} bilgileri":**
  - Aktif yıl yoksa bölüm yerine bilgi kutusu gösterilir: "Aktif eğitim-öğretim yılı yok; atamayı Yıllar ekranından yapabilirsiniz."
  - Bileşen `MembershipFields` adıyla ayrı yazılır ve S-10'da da kullanılır. Alanları:
    - Şube\* (Select)
    - Unvan (Input ve `/api/admin/titles` önerileri)
    - Fotokopi onaylayıcısı (UserPicker, admin lookup, `seasonId` ile). Yardım metni: "Boş bırakılırsa talepleri onaysız olarak fotokopi birimine düşer. Değişiklik yeni taleplere uygulanır."
    - ☐ Yönetici — "Tüm kuruma ve şubelere duyuru ile önemli duyuru yapabilir, grupları ve kurum takvimini yönetir."
    - ☐ Fotokopi sorumlusu — "Kendi şubesinin fotokopi taleplerini hazırlar ve teslim eder."
    - ☐ Doküman alanı açabilir — "Dokümanlar bölümünde yeni alan açıp yetki dağıtabilir."
  - Yeni kayıtta bölüm opsiyoneldir: ☐ "Bu yıla şimdi ata" (varsayılan işaretli).
- **Butonlar:** "Kaydet" ve "Vazgeç".
- **Başarı toast'ları:**
  - Ekleme, `mailSent = true`: "Personel eklendi. Şifre belirleme bağlantısı {email} adresine gönderildi."
  - Ekleme, `mailSent = false`: `toast.warning("Personel eklendi ancak e-posta gönderilemedi. Daha sonra 'Şifre bağlantısı gönder' ile tekrar deneyin.")`
  - Güncelleme: "Personel bilgileri güncellendi."

**Şifre bağlantısı gönder:**

- `ConfirmDialog`: "{ad} kişisine {email} adresine şifre belirleme bağlantısı gönderilecek. Önceki bağlantılar geçersiz olur."
- Başarı toast'ı: "Bağlantı gönderildi."

**Pasifleştir:**

- Pencere açılınca `deactivation-check` çağrılır. Metin: "{ad} pasifleştirilecek. Sisteme giriş yapamayacak ve açık oturumları kapatılacak. Tüm gruplardan çıkarılacak."
- `items` doluysa:
  - Sarı kutu: "Devredilecek sorumluluklar:" ve madde listesi ("12 personelin fotokopi onaylayıcısı").
  - Zorunlu "Devralacak personel" seçimi (UserPicker, aktif yıl).
- Buton: "Pasifleştir" (`destructive`).
- Başarı toast'ı: "Personel pasifleştirildi."

**Aktifleştir:**

- Başarı toast'ı: "Personel aktifleştirildi. Giriş yapabilmesi için bu yıla atanmış olması gerekir."

**Toplu Yükle penceresi:**

1. Açıklama: "Personeli Excel'den hazırlayacağınız bir CSV dosyasıyla toplu ekleyebilirsiniz." Altında "Şablonu indir" bağlantısı.
2. Sütun açıklama tablosu: ad\*, soyad\*, eposta\*, telefon, brans, sube\* (şube adıyla birebir aynı), unvan.
3. İpucu: "Excel'de 'Farklı Kaydet → CSV UTF-8' ya da 'CSV (noktalı virgülle ayrılmış)' seçin."
4. "Eğitim-öğretim yılı" seçimi: ACTIVE ve DRAFT yıllar; varsayılan aktif yıl.
5. Dosya seçici (`.csv`).
6. Buton: "Kontrol Et ve Yükle".
7. `IMPORT_INVALID` hatasında pencerenin içinde kırmızı bir liste açılır (`details` dizisinin tamamı, kaydırılabilir). Toast da gösterilir.
8. Başarıda pencere kapanır. Toast: "{n} personel eklendi. Şifre belirleme e-postaları gönderiliyor; bitince bildirim alacaksınız."

### S-9 Eğitim-Öğretim Yılları (`/yonetim/yillar`)

**Liste:**

- Yıllar kart ya da tablo olarak gösterilir.
- Her satırda: yıl adı, tarih aralığı, durum rozeti ("Hazırlık" sarı, "Aktif" yeşil, "Kapandı" gri), "{n} personel".
- Satıra tıklanınca detay sayfası açılır.

**"Yeni Yıl Oluştur" butonu:**

- DRAFT bir yıl varsa buton pasiftir. Tooltip: "Hazırlık aşamasında bir yıl var."
- Form alanları:
  - Ad: öneri, en son yılın bir sonrakidir ("2027-2028")
  - Başlangıç: önerilen yılın 1 Eylül'ü
  - Bitiş: sonraki yılın 31 Ağustos'u
- Başarı toast'ı: "{ad} oluşturuldu. Şimdi personel atayabilirsiniz." Ardından detay sayfası açılır.

### S-10 Yıl detayı (`/yonetim/yillar/[id]`)

**Başlık:**

- Yıl adı, durum rozeti ve tarihler.
- Sağdaki aksiyonlar duruma göre değişir:
  - DRAFT: "Önceki Yıldan Kopyala", "Yılı Aktifleştir" (birincil), ⋯ menüsünde "Düzenle" ve "Sil"
  - ACTIVE: ⋯ menüsünde "Düzenle"
  - CLOSED: aksiyon yok; `ArchiveBanner` gösterilir

**Atanmamış personel kartı:**

- Görünme koşulu: yıl DRAFT veya ACTIVE ve atanmamış personel sayısı 0'dan büyük.
- Mavi bilgi kartı: "Bu yıla atanmamış {n} aktif personel var. [Göster ve Ata]".
- Açılan pencere:
  - Onay kutulu liste ve arama
  - "Şube" seçimi (zorunlu)
  - Buton: "Seçilenleri Ata"
- Başarı toast'ı: "{n} personel atandı."

**Atama tablosu:**

- Araç çubuğu: arama, şube filtresi ve hızlı filtre çipleri (Yöneticiler / Fotokopi sorumluları / Doküman alanı açabilenler / Onaylayıcısı olmayanlar).
- Sütunlar: ☐ · Ad Soyad · Şube · Unvan · Yetkiler · Onaylayıcı · ⋯ (Düzenle / Yıldan çıkar).
- "Düzenle", `MembershipFields` bileşenini içeren bir pencere açar.

**Toplu işlem çubuğu:**

- Satır seçilince tablonun altında sabit bir çubuk belirir: "{n} kişi seçildi · [Şube Değiştir] [Onaylayıcı Ata] [Onaylayıcıyı Kaldır] [Yıldan Çıkar] [Seçimi Temizle]".
- Her eylem kendi küçük penceresini açar.
- Sonuç toast'ları:
  - "{n} kayıt güncellendi."
  - `skippedSelf > 0` ise ek metin: "Onaylayıcının kendi kaydı atlandı."
- Engelli silmede pencere içinde liste gösterilir.

**Önceki yıldan kopyala penceresi:**

- "Kaynak yıl" seçimi; varsayılan bir önceki yıl.
- Kaynak yıldaki atamaların listesi; tamamı işaretli gelir.
- Pasif kişiler listede görünmez. Liste altında not: "{m} pasif personel listelenmedi."
- Üstte "Tümünü seç / kaldır".
- Buton: "{n} Kişiyi Kopyala".
- Sonuç toast'ı: "{created} kişi kopyalandı." `approverCleared > 0` ise ek metin: "{k} kişinin onaylayıcısı yeni yıla atanmadığı için boş bırakıldı."

**Yılı aktifleştir penceresi:**

- `activation-preview` çağrılır.
- Engeller kırmızı kutuda listelenir; engel varsa buton pasiftir.
- Bilgi maddeleri liste olarak gösterilir.
- ☐ "Yukarıdakileri okudum, onaylıyorum." İşaretlenmeden buton pasiftir.
- Buton: "Yılı Aktifleştir".
- Başarı toast'ı: "{ad} eğitim-öğretim yılı aktifleştirildi." Ardından `['me']` sorgusu yenilenir.

**Sil:**

- `ConfirmDialog`: "{ad} ve tüm atamaları silinecek. Bu işlem geri alınamaz."

### S-11 Şubeler (`/yonetim/subeler`)

- Tablo sütunları: Ad · Durum · Bu yılki personel · ⋯ (Yeniden adlandır / Pasifleştir veya Aktifleştir).
- Buton: "Şube Ekle".
- Form tek alanlıdır.
- Toast'lar:
  - "Şube eklendi."
  - "Şube güncellendi."
  - "Şube pasifleştirildi."

### S-12 Gruplar (`/yonetim/gruplar`, `/yonetim/gruplar/[id]`)

**Liste:**

- Arama ve "Arşivlenmiş grupları göster" anahtarı.
- Tablo sütunları: Ad · Açıklama · Üye sayısı · Durum.
- Buton: "Grup Oluştur".

**Oluşturma penceresi:**

- Ad\*, Açıklama, Üyeler (çoklu UserPicker).
- Toast: "Grup oluşturuldu. Grup sohbeti de açıldı."

**Detay sayfası:**

- Ad ve açıklama düzenlenebilir.
- Üye listesi: avatar, ad, unvan, şube ve ✕ (çıkar) butonu.
- "Üye Ekle" çoklu seçici açar. Toast: "{n} üye eklendi."
- Arşivleme `ConfirmDialog` ile sorulur: "Grup arşivlenecek: yeni paylaşım ve mesajlarda seçilemeyecek, grup sohbeti salt-okunur olacak. Geçmiş içerik korunur."
- Arşivli grupta tüm düzenleme kontrolleri gizlenir; "Arşivden Çıkar" butonu görünür.

### S-13 Denetim Kaydı (`/yonetim/denetim`)

**Filtreler:**

- Başlangıç ve bitiş tarihi (`type=date`); varsayılan son 7 gün
- Kişi (UserPicker, admin lookup)
- İşlem (Select; `/audit/actions` listesinden)

**Tablo:**

- Sütunlar: Zaman · Kişi · İşlem (etiket) · Kayıt (`entityType #entityId`) · IP.
- Satıra tıklanınca altında detay açılır. `detail` JSON'ı "anahtar: değer" satırları olarak gösterilir.

---

## §11 Zorunlu testler (yalnızca bunlar)

| No | Senaryo |
|---|---|
| T-C1 | **Giriş:**<br>(a) Doğru bilgilerle 200 döner ve yanıt `MeResponse` içerir.<br>(b) Yanlış şifreyle 5. denemede `AUTH_ACCOUNT_LOCKED` döner; ardından doğru şifre de kilitli kalır.<br>(c) Pasif kullanıcıda `AUTH_ACCOUNT_PASSIVE` döner.<br>(d) Aktif yıla atanmamış kullanıcıda `AUTH_NO_SEASON_MEMBERSHIP` döner.<br>(e) Yıla atanmamış Sistem Yöneticisi 200 alır. |
| T-C2 | **UserContextFilter:**<br>(a) Oturumu açık kullanıcının ataması silinince bir sonraki istek 403 `AUTH_NO_SEASON_MEMBERSHIP` alır.<br>(b) KVKK onaylamamış kullanıcı `/api/notifications` için 403 `KVKK_REQUIRED` alır; `/api/auth/me` için 200 alır. |
| T-C3 | **Yıl:**<br>(a) DRAFT yıl, atamalarıyla aktifleştirilir; eski yıl CLOSED, yeni yıl ACTIVE olur.<br>(b) İkinci DRAFT oluşturma denemesi `SEASON_DRAFT_EXISTS` döner.<br>(c) Boş yılın aktifleştirilmesi `SEASON_ACTIVATION_BLOCKED` döner. |
| T-C4 | **Pasifleştirme:**<br>(a) 2 kişinin onaylayıcısı olan kullanıcı devralansız pasifleştirilmek istenince `USER_SUCCESSOR_REQUIRED` döner.<br>(b) Devralanla pasifleştirilince iki atamanın onaylayıcısı devralan olur ve kullanıcı giriş yapamaz. |
| T-C5 | **AudienceResolver:** `USER + GROUP + BRANCH` hedefleri yalnızca aktif ve yıla atanmış kişileri döner; pasif ve atanmamış grup üyeleri dışarıda kalır. |
| T-C6 | **CSV:**<br>(a) windows-1254 kodlu, Türkçe karakterli dosya doğru okunur ve kayıt oluşturulur.<br>(b) Tek hatalı satır olan dosya 422 döner; `details` içinde "3. satır: …" yer alır ve hiçbir kayıt oluşmaz. |
