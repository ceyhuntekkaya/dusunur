# 06 — Takvim

Kurumun, şubelerin, kişilerin ve grupların bilgi amaçlı takvimleri ile salon ve laboratuvar rezervasyonları bu modüldedir.

- **Davet, katılım yanıtı, hatırlatıcı ve tekrarlayan kayıt yoktur** (D-22).
- Arayüzde takvimdeki her öğeye **"Kayıt"**, salon takvimlerindeki öğelere **"Rezervasyon"** denir.

Bağlı kararlar: D-21, D-22, D-27, D-28, D-29, D-55 – D-61.

---

## §1 Kapsam

### Var

**Takvim türleri:**

| Tür | Adet ve oluşma şekli |
|---|---|
| Kurum takvimi | 1 adet |
| Şube takvimi | Her şube için 1 adet, otomatik |
| Kişisel takvim | Herkesin 1 varsayılan takvimi olur; kişi en fazla 9 ek takvim açabilir |
| Grup takvimi | Her grup için 1 adet, otomatik |
| Salon / laboratuvar takvimi | Yönetici tanımlar |

**Kayıt alanları:** Başlık, tüm gün ya da saatli tarih, konum, açıklama ve 1 dosya.

**Diğer özellikler:**

- Kişisel takvimi kişi veya grupla iki seviyede paylaşma: Görüntüleme ve Düzenleme
- Salon rezervasyonlarında çakışma engeli (veritabanı kısıtıyla garanti altında)
- Ay, Hafta, Gün ve Liste görünümleri; takvimleri göster/gizle

### Yok

Aşağıdakiler **yapılmaz**:

- Davet ve katılım yanıtı
- Hatırlatıcı
- Tekrarlayan kayıt
- "Yalnız müsaitlik" paylaşım seviyesi
- Özel kayıt
- Kullanıcının takvim rengini seçmesi
- Kayıtlar için çöp kutusu
- iCal dışa veya içe aktarma
- Yıl devrinde takvim şablonu kopyalama
- Kayıt değişikliği bildirimi

---

## §2 İş kuralları

### 2.1 Takvim türleri ve erişim

**Erişim seviyesi (`myAccess`):**

| Seviye | Ne yapabilir |
|---|---|
| `VIEW` | Yalnızca görür |
| `CREATE` | Kayıt ekler; yalnızca **kendi eklediği** kayıtları düzenler ve siler |
| `EDIT_ALL` | Kayıt ekler; tüm kayıtları düzenler ve siler |
| `OWNER` | `EDIT_ALL` + takvimi paylaşır, adını değiştirir, siler |

**Türe göre erişim:**

| Tür (`type`) | Kim görür | `myAccess` | Takvim yönetimi |
|---|---|---|---|
| `INSTITUTION` | Herkes | Yönetici: `EDIT_ALL`, diğerleri: `VIEW` | Sistem yönetir; ad: "Kurum Takvimi" |
| `BRANCH` | Herkes (tüm şubeler) | Yönetici: `EDIT_ALL`, diğerleri: `VIEW` | Sistem yönetir; ad: "{şube adı}" |
| `PERSONAL` | Sahibi ve paylaşılanlar | Sahip: `OWNER`. Paylaşım seviyesi EDIT: `EDIT_ALL`, VIEW: `VIEW`. | Sahip yönetir. Varsayılan takvim silinemez ve adı değiştirilemez. |
| `GROUP` | Grubun şu anki üyeleri ve tüm Yöneticiler | Yönetici: `EDIT_ALL`. Üye: `CREATE`. Grup arşivliyse herkes: `VIEW`. | Sistem yönetir; ad grubun adıdır |
| `RESOURCE` | Herkes | Yönetici: `EDIT_ALL`, diğerleri: `CREATE` | Yönetici tanımlar, düzenler, siler |

**Ek kurallar:**

- **Kurum ve şube takvimi (D-57):** Her Yönetici kurum takvimini ve tüm şube takvimlerini düzenleyebilir; şube ayrımı yoktur.
- **Paylaşımın uyduğu kişiler:** Bir kişisel takvim paylaşımı şu durumlarda kişiye uyar:
  - `USER = me`
  - `GROUP ∈ me.groupIds`

  Birden çok paylaşım uyarsa en yüksek seviye geçerlidir.
- **Kaydı düzenleme yetkisi (`canEdit`):** Üç koşulun hepsi sağlanmalıdır:
  1. Takvim salt okunur değildir.
  2. `myAccess` değeri `OWNER` ya da `EDIT_ALL`'dur, veya `myAccess = CREATE` ve kaydı ekleyen kişi bendir.
  3. Kayıt bu takvime aittir.
- **Görünmeyen takvim:** Bu takvime ya da kayıtlarına yapılan her istek `CAL_CALENDAR_NOT_FOUND` / `CAL_ENTRY_NOT_FOUND` döner.

### 2.2 Sistem takvimlerinin oluşması (`CalendarBootstrap`)

Sistem takvimleri iki yerde kontrol edilir: uygulama açılışında (`ApplicationRunner`, `@Order(110)`) ve her `GET /api/calendar/calendars` çağrısında. Her kontrolde eksik olanlar oluşturulur; bu işlem idempotenttir.

| Takvim | Oluşturma koşulu |
|---|---|
| `INSTITUTION` | Hiç yoksa |
| `BRANCH` | Her şube için yoksa. Pasif şubenin takvimi kalır ve görünmeye devam eder. |
| `GROUP` | Her grup için yoksa. `read_only = grup arşivli mi`. |
| Varsayılan `PERSONAL` | İstek yapan kişi için yoksa. Ad: "Kişisel Takvimim", `is_default = true`. |

**Grup olayları:**

| Olay | İşlem |
|---|---|
| `GroupCreatedEvent` | Grup takvimi oluşturulur |
| `GroupUpdatedEvent` | Takvimin adı güncellenir |
| `GroupArchivedEvent` | `read_only` güncellenir |

**Pasifleştirme:** `UserDeactivatedEvent` geldiğinde, pasifleşen kişiye yapılmış (`subject USER = u`) tüm paylaşımlar silinir. Kişinin kendi takvimleri ve kayıtları kalır.

### 2.3 Kayıt

**Tarih ve saat:** Saat dilimi sabittir: `Europe/Istanbul`.

| Kayıt tipi | API'de | Veritabanında |
|---|---|---|
| Tüm gün (`allDay = true`) | `startDate` ve `endDate` (ikisi de dahil, LocalDate) | `start_at = startDate 00:00 İstanbul`, `end_at = (endDate + 1 gün) 00:00 İstanbul` (bitiş hariç) |
| Saatli | `startAt` ve `endAt` (ISO, ofsetli; saniyeler sıfırlanır) | Olduğu gibi saklanır |

**Kurallar:**

| Kural | Hata |
|---|---|
| `end_at > start_at` | `CAL_END_BEFORE_START` |
| Süre en fazla 31 gün | `CAL_DURATION_TOO_LONG` |
| Tarihler bugünden en fazla 5 yıl önce ve 5 yıl sonra olabilir | `CAL_DATE_OUT_OF_RANGE` |

**Diğer davranışlar:**

- **Takvim değişmez:** Kaydın takvimi oluşturulduktan sonra değiştirilemez. Güncelleme isteğindeki `calendarId` yok sayılır. Arayüzde bu alan düzenlemede kilitlidir.
- **Dosya:** En fazla 1 dosya eklenebilir (`purpose = CALENDAR`). Güncellemede:
  - `fileId` verilirse dosya değiştirilir; eski dosya silinir.
  - `removeFile = true` ise dosya kaldırılır.
  - İkisi de yoksa mevcut dosya korunur.
- **Silme (D-61):** Kayıt fiziksel olarak silinir ve dosyası da silinir; çöp kutusu yoktur. Silen kişi kaydı ekleyen değilse denetim kaydı yazılır.
- **Yıl bağı yoktur (D-56):** Kayıtların `season_id` alanı yoktur. Bu sayede yaz döneminde yeni yılın takvimi hazırlanabilir. Geçmiş tarihli kayıtlar da yetkiye göre düzenlenebilir.

### 2.4 Salon ve laboratuvar rezervasyonu (D-29, D-58)

**Salon tanımı:** Yönetici yapar.

- Ad 2–100 karakter olur ve salon adları arasında tekildir (`fold` ile karşılaştırılır).
- Açıklama en fazla 500 karakterdir.
- Şube isteğe bağlıdır; yalnızca etiket olarak gösterilir.

**Rezervasyon:** Herkes rezervasyon yapabilir.

**Çakışma kuralı:** Aynı salon takviminde iki kaydın zaman aralığı `[start, end)` kesişemez. Uç uca gelen kayıtlar (biri 10:00'da biterken diğeri 10:00'da başlıyorsa) serbesttir.

Çakışma iki katmanda engellenir:

1. **Servis ön kontrolü:** Çakışan ilk kayıt bulunur ve kullanıcıya `CAL_RESOURCE_CONFLICT({başlık}, {ekleyen}, {zaman})` döner.
2. **Veritabanı kısıtı:** Aynı anda iki istek gelmesi durumuna karşı `ex_cal_resource_overlap` kısıtı vardır. Bu kısıtın ihlali (`DataIntegrityViolationException`) yakalanır ve aynı hata koduna çevrilir. Mesajdaki yer tutucular bu durumda "başka bir rezervasyon" metniyle doldurulur.

**Salon silme:** Salonun bugünden sonra başlayan rezervasyonu yoksa silinebilir; varsa `CAL_RESOURCE_HAS_FUTURE(n)`. Silmede geçmiş rezervasyonlar da silinir.

### 2.5 Paylaşım (D-27, D-55)

- **Kim paylaşabilir:** Yalnızca `PERSONAL` takvimin sahibi. Diğer türler paylaşılamaz → `CAL_SHARE_NOT_PERSONAL`.
- **Kiminle:** `USER` (aktif yıla atanmış kişi) ve `GROUP` (arşivlenmemiş herhangi bir grup). Doğrulama `AudienceResolver.validate(me, [subject], {USER, GROUP})` ile yapılır.
- **Kendine paylaşım:** Yapılamaz → `CAL_SHARE_OWNER`.
- **Seviye:** `VIEW` ya da `EDIT`. Aynı özneye yeni paylaşım yapılırsa seviye güncellenir (upsert). Bir takvimde en fazla 50 paylaşım olabilir → `CAL_SHARE_LIMIT`.

### 2.6 Bildirim (D-60)

Bu modülde tek bir bildirim vardır:

| Olay | Kime | type | title | link |
|---|---|---|---|---|
| Kişisel takvim bir kişiyle paylaşıldı (yeni satır) | O kişi | `CAL_SHARED` | "{ad} '{takvim}' takvimini sizinle paylaştı" | `/takvim` |

Bu modülün WebSocket olayı ve rozeti yoktur.

### 2.7 Renkler

Renk sunucuda belirlenir; kullanıcı değiştiremez.

**Palet** (10 renk, sırasıyla): `#2453C7`, `#0F9D8A`, `#7C3AED`, `#D97706`, `#DB2777`, `#059669`, `#DC2626`, `#0891B2`, `#65A30D`, `#9333EA`.

| Takvim | Renk |
|---|---|
| `INSTITUTION` | 0 (`#2453C7`) |
| `BRANCH` | `1 + (branchId % 2)` |
| Diğerleri | `3 + (id % 7)` |

---

## §3 Veritabanı — `V6__calendar.sql`

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE cal_calendar (
  id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  type            varchar(12)  NOT NULL CHECK (type IN ('INSTITUTION','BRANCH','PERSONAL','GROUP','RESOURCE')),
  name            varchar(100) NOT NULL,
  name_key        varchar(100) NOT NULL,
  description     varchar(500),
  owner_id        bigint       REFERENCES app_user(id),
  branch_id       bigint       REFERENCES branch(id),
  staff_group_id  bigint       REFERENCES staff_group(id),
  is_default      boolean      NOT NULL DEFAULT false,
  read_only       boolean      NOT NULL DEFAULT false,
  created_by      bigint       REFERENCES app_user(id),
  created_at      timestamptz  NOT NULL DEFAULT now(),
  updated_at      timestamptz  NOT NULL DEFAULT now(),
  CONSTRAINT ck_cal_personal CHECK ((type = 'PERSONAL') = (owner_id IS NOT NULL)),
  CONSTRAINT ck_cal_group    CHECK ((type = 'GROUP') = (staff_group_id IS NOT NULL)),
  CONSTRAINT ck_cal_branch   CHECK (type <> 'BRANCH' OR branch_id IS NOT NULL),
  CONSTRAINT ck_cal_default  CHECK (NOT is_default OR type = 'PERSONAL')
);
CREATE UNIQUE INDEX ux_cal_institution ON cal_calendar ((true)) WHERE type = 'INSTITUTION';
CREATE UNIQUE INDEX ux_cal_branch      ON cal_calendar (branch_id) WHERE type = 'BRANCH';
CREATE UNIQUE INDEX ux_cal_group       ON cal_calendar (staff_group_id) WHERE type = 'GROUP';
CREATE UNIQUE INDEX ux_cal_default     ON cal_calendar (owner_id) WHERE is_default;
CREATE UNIQUE INDEX ux_cal_resource    ON cal_calendar (name_key) WHERE type = 'RESOURCE';
CREATE INDEX ix_cal_owner ON cal_calendar (owner_id) WHERE type = 'PERSONAL';

CREATE TABLE cal_share (
  id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  calendar_id  bigint      NOT NULL REFERENCES cal_calendar(id) ON DELETE CASCADE,
  subject_type varchar(6)  NOT NULL CHECK (subject_type IN ('USER','GROUP')),
  subject_id   bigint      NOT NULL,
  level        smallint    NOT NULL CHECK (level IN (1,2)),     -- 1=VIEW 2=EDIT
  created_at   timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ux_cal_share UNIQUE (calendar_id, subject_type, subject_id)
);
CREATE INDEX ix_cal_share_subject ON cal_share (subject_type, subject_id);

CREATE TABLE cal_entry (
  id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  calendar_id  bigint        NOT NULL REFERENCES cal_calendar(id) ON DELETE CASCADE,
  is_resource  boolean       NOT NULL,
  title        varchar(150)  NOT NULL,
  all_day      boolean       NOT NULL,
  start_at     timestamptz   NOT NULL,
  end_at       timestamptz   NOT NULL,
  location     varchar(200),
  description  varchar(2000),
  file_id      uuid          REFERENCES stored_file(id),
  created_by   bigint        NOT NULL REFERENCES app_user(id),
  updated_by   bigint        NOT NULL REFERENCES app_user(id),
  created_at   timestamptz   NOT NULL DEFAULT now(),
  updated_at   timestamptz   NOT NULL DEFAULT now(),
  CONSTRAINT ck_cal_entry_range CHECK (end_at > start_at),
  CONSTRAINT ex_cal_resource_overlap EXCLUDE USING gist
    (calendar_id WITH =, tstzrange(start_at, end_at, '[)') WITH &&) WHERE (is_resource)
);
CREATE INDEX ix_cal_entry_range ON cal_entry (calendar_id, start_at, end_at);
```

**Dosya silme notu:** `ON DELETE CASCADE` yalnızca takvim silinirken kayıtları siler. Kayıtlara bağlı dosyalar bu yolla silinmez; servis, takvimi silmeden önce dosyaları `FileStorageService.delete` ile kendisi siler.

---

## §4 Sınıflar (`calendar/`)

| Sınıf | Görev |
|---|---|
| `CalCalendar`, `CalShare`, `CalEntry` | Entity'ler. Enum'lar: `CalendarType`, `CalShareLevel { VIEW(1), EDIT(2) }`, `CalAccess { VIEW, CREATE, EDIT_ALL, OWNER }`. |
| `CalendarAccess` | `visibleCalendars(me)`, `access(me, calendar)`, `canEditEntry(me, cal, entry)` (§2.1) |
| `CalendarBootstrap` | §2.2. Grup ve pasifleştirme olaylarını dinler. |
| `CalendarService` | Kişisel takvim ve salon oluşturma, düzenleme, silme; paylaşım işlemleri |
| `CalendarEntryService` | Kayıt oluşturma, güncelleme, silme; tüm gün dönüşümü, çakışma kontrolü ve kısıt hatasının çevrilmesi, dosya işlemleri |
| `CalendarQueries` | Aralık sorgusu (JdbcClient), takvim listesi |
| `CalendarController`, `CalendarDtos`, `CalendarErrorCode`, `CalendarAuditAction` | |
| `CalendarDevSeeder` | Yalnızca `dev` profilinde (§8) |

**Görünür takvim sorgusu (`visibleCalendars`):**

```sql
SELECT c.* FROM cal_calendar c
WHERE c.type IN ('INSTITUTION','BRANCH','RESOURCE')
   OR (c.type = 'PERSONAL' AND (c.owner_id = :uid
        OR EXISTS (SELECT 1 FROM cal_share s WHERE s.calendar_id = c.id AND
                   ((s.subject_type = 'USER' AND s.subject_id = :uid) OR (s.subject_type = 'GROUP' AND s.subject_id IN (:groupIds))))))
   OR (c.type = 'GROUP' AND (:isManager OR c.staff_group_id IN (:groupIds)))
```

`groupIds` boşsa ilgili koşullar sorgudan çıkarılır; sorgu dinamik kurulur.

---

## §5 API

### 5.1 DTO'lar

```ts
type CalAccess = 'VIEW'|'CREATE'|'EDIT_ALL'|'OWNER';
type CalendarSection = 'INSTITUTION'|'BRANCH'|'MINE'|'SHARED'|'GROUP'|'OTHER_GROUP'|'RESOURCE';
type CalendarDto = {
  id: number; type: 'INSTITUTION'|'BRANCH'|'PERSONAL'|'GROUP'|'RESOURCE'; name: string; description?: string;
  color: string; section: CalendarSection; myAccess: CalAccess; canCreate: boolean;   // canCreate: myAccess != VIEW && !readOnly
  canManage: boolean;     // PERSONAL sahibi (ad, paylaşım, silme — varsayılan takvimde yalnızca paylaşım) veya RESOURCE için Yönetici
  readOnly: boolean; isDefault: boolean; branchName?: string; owner?: { id: number; fullName: string };
};
type EntryDto = {
  id: number; calendarId: number; title: string; allDay: boolean;
  startAt: string; endAt: string;                  // her zaman dolu (tüm günde gece yarısı, bitiş hariç)
  startDate?: string; endDate?: string;            // yalnızca allDay; endDate dahil
  location?: string; description?: string;
  file?: { name: string; contentType: string; sizeBytes: number };
  createdBy: { id: number; fullName: string }; createdAt: string; updatedAt: string; canEdit: boolean;
};
type EntryInput = {
  calendarId: number; title: string; allDay: boolean;
  startDate?: string; endDate?: string; startAt?: string; endAt?: string;
  location?: string; description?: string; fileId?: string; removeFile?: boolean;
};
type CalShareRow = { id: number; subject: TargetView; level: 'VIEW'|'EDIT' };
```

**`section` değerinin belirlenmesi:**

| section | Kapsam |
|---|---|
| `INSTITUTION` | Kurum takvimi |
| `BRANCH` | Şube takvimleri |
| `MINE` | Sahibi olduğum kişisel takvimler |
| `SHARED` | Benimle paylaşılan kişisel takvimler |
| `GROUP` | Üyesi olduğum grupların takvimleri |
| `OTHER_GROUP` | Yöneticiler için, üyesi olmadıkları grupların takvimleri |
| `RESOURCE` | Salon ve laboratuvarlar |

**Liste sıralaması:** Önce yukarıdaki section sırası, sonra her section içinde Türkçe ad sırası. `MINE` içinde varsayılan takvim en üsttedir.

### 5.2 Uç noktalar

Tüm uçlarda `me.requireMember()` çağrılır.

**Takvimler**

| Uç nokta | İstek | Yanıt | Kurallar |
|---|---|---|---|
| `GET /api/calendar/calendars` | — | `CalendarDto[]` | Önce `CalendarBootstrap.ensure(me)` çalışır |
| `POST /api/calendar/calendars` | `{ name, description? }` | 201 `CalendarDto` | Yeni `PERSONAL` takvim. Kişinin toplam 10 kişisel takvimi olabilir; aşılırsa `CAL_PERSONAL_LIMIT`. |
| `POST /api/calendar/resources` | `{ name, description?, branchId? }` | 201 `CalendarDto` | Yönetici değilse `CAL_RESOURCE_FORBIDDEN`. Ad tekrarlanırsa `CAL_RESOURCE_NAME_TAKEN` (alan: name). Denetim: `CAL_RESOURCE_CREATED`. |
| `PUT /api/calendar/calendars/{id}` | `{ name, description?, branchId? }` | `CalendarDto` | Sistem takvimlerinde `CAL_SYSTEM_CALENDAR`, varsayılan takvimde `CAL_DEFAULT_CALENDAR`, yetki yoksa `CAL_MANAGE_FORBIDDEN`. `branchId` yalnızca `RESOURCE` için kullanılır. Salon için denetim: `CAL_RESOURCE_UPDATED`. |
| `DELETE /api/calendar/calendars/{id}` | — | 204 | Kişisel takvim: yalnızca sahibi siler; varsayılan takvim silinemez; kayıtlar ve dosyaları da silinir. Salon: Yönetici siler; §2.4'teki ileri tarih kuralı uygulanır. Sistem takvimlerinde `CAL_SYSTEM_CALENDAR`. Salon için denetim: `CAL_RESOURCE_DELETED`. |

**Paylaşım**

| Uç nokta | İstek | Yanıt | Kurallar |
|---|---|---|---|
| `GET /api/calendar/calendars/{id}/shares` | — | `CalShareRow[]` | Yalnızca sahip; değilse `CAL_MANAGE_FORBIDDEN` |
| `POST /api/calendar/calendars/{id}/shares` | `{ subject: TargetRef, level }` | 201 / 200 `CalShareRow` | §2.5 kuralları. Yeni `USER` paylaşımında bildirim gider (§2.6). |
| `PUT /api/calendar/calendars/{id}/shares/{sid}` | `{ level }` | `CalShareRow` | |
| `DELETE /api/calendar/calendars/{id}/shares/{sid}` | — | 204 | |

**Kayıtlar**

| Uç nokta | İstek | Yanıt | Kurallar |
|---|---|---|---|
| `GET /api/calendar/entries?from=&to=&calendarIds=1,2` | — | `EntryDto[]` | `from` ve `to` ofsetli ISO değerlerdir. Aralık en fazla 100 gün olabilir; aşılırsa `CAL_RANGE_TOO_LONG`. Koşul: `start_at < to AND end_at > from`. Görünmeyen takvim id'leri sessizce atlanır. Sıralama `start_at`. |
| `GET /api/calendar/entries/{id}` | — | `EntryDto` | |
| `POST /api/calendar/entries` | `EntryInput` | 201 `EntryDto` | Takvime ekleme yetkisi yoksa `CAL_CREATE_FORBIDDEN`. Takvim salt okunursa `CAL_READ_ONLY`. Ayrıca §2.3 ve §2.4 kuralları. Dosya `attach(fileId, me, CALENDAR)` ile bağlanır. |
| `PUT /api/calendar/entries/{id}` | `EntryInput` (`calendarId` yok sayılır) | `EntryDto` | `canEdit` değilse `CAL_EDIT_FORBIDDEN`. Salon kaydında çakışma kontrolü kaydın kendisi hariç tutularak yapılır. |
| `DELETE /api/calendar/entries/{id}` | — | 204 | `CAL_EDIT_FORBIDDEN`. Silen kişi kaydı ekleyen değilse denetim: `CAL_ENTRY_DELETED`, detayda `{ calendarId, title, createdBy }`. |
| `GET /api/calendar/entries/{id}/file?download=false` | — | dosya akışı | Kaydı görebilen herkes. Kayıtta dosya yoksa `FILE_NOT_FOUND`. |

### 5.3 Doğrulama tablosu

| Alan | Kural | Mesaj |
|---|---|---|
| `title` | Trim edilir, 2–150 karakter | "Başlık 2–150 karakter olmalıdır." |
| `allDay = true` | `startDate` ve `endDate` zorunlu, `endDate >= startDate` | "Başlangıç ve bitiş tarihini seçin." / CAL_END_BEFORE_START |
| `allDay = false` | `startAt` ve `endAt` zorunlu, `endAt > startAt` | "Başlangıç ve bitiş saatini seçin." / CAL_END_BEFORE_START |
| Süre | En fazla 31 gün | CAL_DURATION_TOO_LONG |
| `location` | En fazla 200 karakter | "Konum en fazla 200 karakter olabilir." |
| `description` | En fazla 2000 karakter | "Açıklama en fazla 2000 karakter olabilir." |
| Takvim adı | Trim edilir, 2–100 karakter | "Takvim adı 2–100 karakter olmalıdır." |
| Paylaşım `level` | VIEW veya EDIT | "Bir yetki seviyesi seçin." |

---

## §6 Hata kodları (`CalendarErrorCode`)

| code | HTTP | message |
|---|---|---|
| CAL_CALENDAR_NOT_FOUND | 404 | Takvim bulunamadı veya erişiminiz yok. |
| CAL_ENTRY_NOT_FOUND | 404 | Kayıt bulunamadı veya silinmiş. |
| CAL_CREATE_FORBIDDEN | 403 | Bu takvime kayıt ekleme yetkiniz yok. |
| CAL_EDIT_FORBIDDEN | 403 | Bu kaydı yalnızca ekleyen kişi veya takvimi yönetmeye yetkili kişiler değiştirebilir. |
| CAL_READ_ONLY | 409 | Bu takvim salt okunur; grup arşivlendiği için değişiklik yapılamaz. |
| CAL_MANAGE_FORBIDDEN | 403 | Bu takvimi yönetme yetkiniz yok. |
| CAL_SYSTEM_CALENDAR | 400 | Bu takvim sistem tarafından yönetilir; adı değiştirilemez ve silinemez. |
| CAL_DEFAULT_CALENDAR | 400 | Varsayılan kişisel takviminiz silinemez ve adı değiştirilemez. |
| CAL_PERSONAL_LIMIT | 400 | En fazla 10 kişisel takviminiz olabilir. |
| CAL_RESOURCE_FORBIDDEN | 403 | Salon ve laboratuvarları yalnızca yöneticiler tanımlayabilir. |
| CAL_RESOURCE_NAME_TAKEN | 409 | Bu adla bir salon veya laboratuvar zaten var. |
| CAL_RESOURCE_HAS_FUTURE | 409 | Bu salonun ileri tarihli {0} rezervasyonu var. Silmeden önce bu rezervasyonları kaldırın. |
| CAL_RESOURCE_CONFLICT | 409 | Bu salon seçilen saatte dolu: "{0}" ({1}, {2}). Lütfen başka bir saat seçin. |
| CAL_END_BEFORE_START | 400 | Bitiş, başlangıçtan sonra olmalıdır. |
| CAL_DURATION_TOO_LONG | 400 | Bir kayıt en fazla 31 gün sürebilir. |
| CAL_DATE_OUT_OF_RANGE | 400 | Geçerli bir tarih seçin. |
| CAL_RANGE_TOO_LONG | 400 | Tek seferde en fazla 100 günlük kayıt görüntülenebilir. |
| CAL_SHARE_NOT_PERSONAL | 400 | Yalnızca kişisel takvimler paylaşılabilir. |
| CAL_SHARE_OWNER | 400 | Takvimi kendinizle paylaşamazsınız. |
| CAL_SHARE_LIMIT | 400 | Bir takvim en fazla 50 kişi veya grupla paylaşılabilir. |

**`CAL_RESOURCE_CONFLICT` mesajındaki yer tutucular:**

- `{1}`: Kaydı ekleyen kişinin adı.
- `{2}`: "18 Kas Çar 10:00–11:30" biçimindedir. Tüm gün kayıtlarında "18 Kas (tüm gün)" yazılır.

**Denetim işlemleri (`CalendarAuditAction`):**

| Kod | Etiket |
|---|---|
| CAL_RESOURCE_CREATED | Salon/laboratuvar tanımlandı |
| CAL_RESOURCE_UPDATED | Salon/laboratuvar güncellendi |
| CAL_RESOURCE_DELETED | Salon/laboratuvar silindi |
| CAL_ENTRY_DELETED | Başkasına ait takvim kaydı silindi |

---

## §7 Ekranlar

### 7.1 Kütüphane

Takvim görünümü **FullCalendar** ile yapılır. Kullanılan paketlerin tamamı MIT lisanslı, standart paketlerdir:

- `@fullcalendar/react`
- `@fullcalendar/core`
- `@fullcalendar/daygrid`
- `@fullcalendar/timegrid`
- `@fullcalendar/list`
- `@fullcalendar/interaction`

Sürüm olarak React 19 uyumlu en güncel 6.x kullanılır. Kurulum: `npm i @fullcalendar/react @fullcalendar/core @fullcalendar/daygrid @fullcalendar/timegrid @fullcalendar/list @fullcalendar/interaction`.

**Ortak ayarlar:**

| Ayar | Değer |
|---|---|
| `locale` | `trLocale` (`@fullcalendar/core/locales/tr`) |
| `firstDay` | 1 (pazartesi) |
| `timeZone` | `'local'` |
| `slotMinTime` / `slotMaxTime` | `'07:00'` / `'22:00'` |
| `slotDuration` | `'00:30'` |
| `nowIndicator` | `true` |
| `dayMaxEvents` | 3 (fazlası "+n daha" olarak gösterilir) |
| `eventTimeFormat` | `{ hour: '2-digit', minute: '2-digit', hour12: false }` |
| `buttonText` | Bugün, Ay, Hafta, Gün, Liste |
| `allDayText` | `'Tüm gün'` |
| `noEventsText` | `'Bu aralıkta kayıt yok.'` |
| `events` | Fonksiyon olarak verilir: `(info, ok, fail) => api('/api/calendar/entries?from=…&to=…&calendarIds=…')`. Hata `fail` ile döner ve global toast gösterilir. |
| Yenileme | Kayıt değişince ya da takvim görünürlüğü değişince `calendarRef.current.getApi().refetchEvents()` çağrılır |
| `eventColor` | Takvimin rengi. Görünüm: `rounded-md`, metin beyaz. Liste görünümünde renkli nokta. |

**Tüm gün dönüşümü:** FullCalendar'da tüm gün kayıtlarının bitişi **hariç** tarihtir. API'nin `endDate` değeri ise dahil tarihtir. Dönüşümü `features/calendar/convert.ts` yapar:

- API'den gelirken: `end = endDate + 1 gün`
- Formdan API'ye giderken: `endDate = end − 1 gün`

### 7.2 Takvim sayfası (`/takvim?gorunum=ay|hafta|gun|liste&tarih=YYYY-MM-DD`)

**Masaüstü (≥1024px) yerleşimi:**

- Solda 260px'lik panel:
  - "Yeni Kayıt" (birincil, tam genişlik)
  - Takvim listesi (§7.3)
- Sağda FullCalendar.
  - Başlık çubuğu: sol tarafta "‹ › Bugün", ortada başlık ("Kasım 2026"), sağda görünüm düğmeleri (Ay, Hafta, Gün, Liste).
  - Varsayılan görünüm Ay'dır (`dayGridMonth`).

**Mobil yerleşimi:**

- Üstte segmentli görünüm seçimi: **Liste** (varsayılan, `listWeek`), **Gün** (`timeGridDay`), **Ay** (`dayGridMonth`). Hafta görünümü mobilde gösterilmez.
- Altında "‹ Bugün ›" ve başlık; sağda "Takvimler" butonu. Bu buton §7.3'teki listeyi Drawer içinde açar.
- FAB: "Yeni Kayıt".

**Etkileşim:**

| Eylem | Sonuç |
|---|---|
| Kayda tıklama | Kayıt Detayı penceresi (§7.5) |
| Boş güne veya saate tıklama | Takvime ekleme yetkisi olan en az bir görünür takvim varsa Kayıt Formu tıklanan zamanla açılır. Ay görünümünde varsayılan, o gün 09:00–10:00'dır. |
| Saat aralığı sürükleme (masaüstü, Hafta ve Gün görünümlerinde, `selectable`) | Form seçilen aralıkla açılır |
| Kaydı sürükleyip taşıma, kenarından tutup uzatma | **Kapalı** (`editable: false`). Değişiklik yalnızca formdan yapılır. |

### 7.3 Takvim listesi (sol panel veya Drawer)

**Bölümler:** "Kurum", "Şubeler", "Takvimlerim", "Benimle Paylaşılanlar", "Gruplarım", "Diğer Grup Takvimleri" (Yönetici; varsayılan kapalı ve katlanır), "Salonlar ve Laboratuvarlar".

- Boş bölümler gizlenir.
- Bölüm başlıkları katlanabilir.

**Her satırda:**

- Takvim renginde onay kutusu (göster/gizle) ve ad
- Salon satırında şube etiketi gri metinle; paylaşılan takvimde "{sahip}" gri metinle
- Kilit ikonu: salt okunur takvimlerde
- ⋯ menüsü (yalnızca `canManage` olan takvimlerde): "Paylaş" (kişisel), "Yeniden adlandır", "Sil"

**"Yalnızca bunu göster":** Salon satırının ⋯ menüsünde bulunur. Diğer tüm takvimleri gizler ve görünümü Hafta'ya çevirir. Böylece salonun boş saatleri görülür.

**Ekleme bağlantıları:**

- "Takvimlerim" altında: "+ Takvim oluştur"
- "Salonlar ve Laboratuvarlar" altında, Yönetici için: "+ Salon ekle"

**Görünürlük seçimleri (D-59):**

- Seçimler `localStorage` içinde saklanır: `takvim-gorunur = { [calendarId]: boolean }`. Okuma ve yazma `try/catch` ile yapılır; hata olursa varsayılanlar kullanılır.
- Varsayılan olarak görünenler: Kurum, kendi şubem, `MINE`, `SHARED` ve `GROUP` bölümleri.
- Varsayılan olarak gizli olanlar: diğer şubeler, `OTHER_GROUP` ve salonlar.

### 7.4 Kayıt Formu (`ResponsiveDialog`)

**Başlık:** "Yeni Kayıt" / "Kaydı Düzenle". Seçili takvim bir salonsa "Yeni Rezervasyon" / "Rezervasyonu Düzenle".

**Alanlar:**

1. **Takvim\***
   - Select; yalnızca `canCreate` olan takvimler listelenir. Seçenekler bölüm adlarıyla gruplanır ve her birinde renkli nokta bulunur.
   - Varsayılan: varsayılan kişisel takvim. Takvim listesinden bir salon için açıldıysa o salon.
   - Düzenlemede kilitlidir.
2. **Başlık\*** — Placeholder: "Ör. Veli toplantısı".
3. **Tüm gün** — anahtar.
4. **Tarih ve saat:**
   - Tüm gün kapalıysa: "Başlangıç\*" ve "Bitiş\*" (`datetime-local`). Başlangıç değiştirilince bitiş, aradaki süre korunarak kaydırılır. Varsayılan süre 1 saattir.
   - Tüm gün açıksa: "Başlangıç tarihi\*" ve "Bitiş tarihi\*" (`date`).
5. **Konum.** Salon takviminde bu alan gizlenir; konum zaten salondur.
6. **Açıklama** — textarea, 4 satır.
7. **Dosya** — tek dosyalık `FileUploader` (`purpose=CALENDAR`). Düzenlemede mevcut dosya "ad · boyut" ve ✕ ile gösterilir; ✕ `removeFile` değerini ayarlar.

**Butonlar:** "Kaydet" ve "Vazgeç". Düzenlemede ayrıca solda "Sil" (destructive).

**Toast'lar:**

| İşlem | Toast |
|---|---|
| Ekleme | "Kayıt eklendi." (salonda "Rezervasyon yapıldı.") |
| Güncelleme | "Kayıt güncellendi." |
| Silme | "Kayıt silindi." |

**Silme onayı:** "'{başlık}' silinecek. Bu işlem geri alınamaz."

**Salon çakışması:** `CAL_RESOURCE_CONFLICT` hatasında toast'a ek olarak, formun tarih alanının altında aynı mesaj kırmızı yazıyla gösterilir.

### 7.5 Kayıt Detayı (`ResponsiveDialog`)

- Üstte takvim renginde 4px bir şerit, ardından başlık (lg, semibold).
- Satırlar:
  - Calendar ikonu ve zaman. Saatli kayıt: "14 Kasım 2026 Cuma, 10:00–11:30". Birden fazla güne yayılıyorsa: "14 Kas 10:00 – 15 Kas 12:00". Tüm gün: "14 Kasım 2026 (tüm gün)" ya da "14–16 Kasım 2026 (tüm gün)".
  - MapPin ikonu ve konum
  - Takvim adı (renkli nokta ile)
  - Açıklama (`linkify`, `pre-wrap`)
  - Dosya kartı: "Önizle" (pdf veya görselse) ve "İndir"
- Altta gri metin: "{ad} ekledi · {DateText}".
- Butonlar: `canEdit` ise "Düzenle" ve "Sil"; her durumda "Kapat".

### 7.6 Takvim yönetimi pencereleri

| Pencere | Alanlar | Toast |
|---|---|---|
| **Takvim oluştur** | Ad\*, Açıklama | "Takvim oluşturuldu." |
| **Yeniden adlandır** | Ad\*, Açıklama | "Takvim güncellendi." |
| **Salon ekle / düzenle** | Ad\* (placeholder "Ör. Konferans Salonu"), Açıklama (placeholder "Kapasite, ekipman vb."), Şube (opsiyonel Select) | "Salon eklendi." / "Salon güncellendi." |

**Sil onay metinleri:**

- Kişisel takvim: "'{ad}' takvimi ve içindeki tüm kayıtlar silinecek. Bu işlem geri alınamaz."
- Salon: "'{ad}' silinecek. Geçmiş rezervasyonlar da silinir."

**Paylaş penceresi:**

- Başlık: "'{ad}' takvimini paylaş".
- `TargetPicker` (kişi ve grup), seviye seçimi ("Görüntüleyebilir" / "Düzenleyebilir") ve "Ekle" butonu.
- Paylaşım listesi: her satırda ad, seviye seçimi ve ✕.
- Toast'lar: "Takvim paylaşıldı.", "Paylaşım güncellendi.", "Paylaşım kaldırıldı."
- Boş liste metni: "Bu takvimi henüz kimseyle paylaşmadınız."
- Seviye açıklama metni: "Düzenleyebilir: kayıt ekleyebilir, tüm kayıtları değiştirebilir ve silebilir."

---

## §8 Geliştirme verisi (`CalendarDevSeeder`)

Bu ayın ve gelecek ayın tarihleriyle şu kayıtlar oluşturulur:

| Takvim | Kayıtlar |
|---|---|
| Kurum Takvimi | "Öğretmenler Kurulu" (saatli, konum "Konferans Salonu"), "Ara Tatil" (tüm gün, 5 gün), 1 dosyalı kayıt |
| Merkez Kampüs | "Veli Toplantısı – 9. sınıflar" |
| Kuzey Kampüs | "Kampüs Gezisi" (tüm gün) |
| Ali'nin varsayılan takvimi | 2 kayıt |
| Ali'nin ek takvimi "Nöbet" | 1 kayıt; Matematik Zümresi ile VIEW olarak paylaşılır |
| Matematik Zümresi grup takvimi | Elif'in 1 kaydı |
| Salon "Konferans Salonu" (Merkez) | 2 uç uca rezervasyon |
| Salon "Fen Laboratuvarı" (Kuzey) | 1 rezervasyon |

---

## §9 Zorunlu testler

| No | Senaryo |
|---|---|
| T-K1 | **Kişisel takvim:**<br>(a) Başkası paylaşılmamış takvimi isteyince `CAL_CALENDAR_NOT_FOUND` alır.<br>(b) VIEW paylaşımı olan kişi kayıt eklemeye çalışınca `CAL_CREATE_FORBIDDEN` alır.<br>(c) EDIT paylaşımı olan kişi kayıt ekleyebilir ve başkasının kaydını düzenleyebilir.<br>(d) Grup üzerinden yapılan paylaşım, grubun üyesine uyar. |
| T-K2 | **Grup takvimi:**<br>(a) Üye kayıt ekler; başka bir üye bu kaydı düzenlemeye çalışınca `CAL_EDIT_FORBIDDEN` alır.<br>(b) Yönetici bu kaydı düzenleyebilir.<br>(c) Grup arşivlenince yeni kayıt `CAL_READ_ONLY` döner. |
| T-K3 | **Salon:**<br>(a) Çakışan rezervasyon `CAL_RESOURCE_CONFLICT` döner.<br>(b) Uç uca rezervasyon (10:00–11:00 ve 11:00–12:00) kabul edilir.<br>(c) Kendi saatini değiştiren güncelleme kendisiyle çakışmaz. |
| T-K4 | **Kurum takvimi:** Yönetici olmayan kişi kayıt eklemeye çalışınca `CAL_CREATE_FORBIDDEN` alır; Yönetici ekleyebilir. |
| T-K5 | **Tüm gün:** `startDate = 2026-11-14`, `endDate = 2026-11-16` ile kaydedilen kayıt, `start_at = 2026-11-14T00:00+03:00`, `end_at = 2026-11-17T00:00+03:00` olarak saklanır ve yanıtta aynı tarihler döner. |
