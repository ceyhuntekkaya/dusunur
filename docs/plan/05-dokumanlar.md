# 05 — Dokümanlar (DYS)

Kurumun kalıcı belge hafızasıdır: yönetmelikler, tutanaklar, şablonlar, sınav arşivi.

Doküman modülü **eğitim-öğretim yılından bağımsızdır**: kayıtlar yıl taşımaz ve yıl değişince arşive geçmez. Giriş kuralı yine de geçerlidir; yani modülü yalnızca aktif yıla atanmış kişiler kullanabilir (`requireMember`).

Bağlı kararlar: D-08, D-20, D-49 – D-54.

---

## §1 Kapsam

### Var

- Doküman Alanları: oluşturma, ad ve açıklama düzenleme, boşken silme
- Alan düzeyinde üç yetki seviyesi; yetki kişiye, gruba, şubeye veya tüm personele verilir
- Klasör ağacı
- Çoklu dosya yükleme: sürükle-bırak, ilerleme çubuğu, aynı ad çıkarsa "üzerine yaz / farklı adla kaydet / vazgeç" sorusu
- Yeniden adlandırma ve aynı alan içinde taşıma
- 30 günlük çöp kutusu: geri yükleme, kalıcı silme, otomatik temizlik
- PDF ve görsel önizleme; diğer dosyalar indirilir
- Erişilebilen tüm alanlarda dosya ve klasör adı araması
- Kurum içi bağlantı kopyalama
- Pasifleştirmede alan sahipliğinin devri

### Yok

Aşağıdakiler **yapılmaz**:

- Sürüm geçmişi
- Klasöre veya dosyaya özel izin
- Alanlar arası taşıma
- Kopyalama
- Klasör yükleme
- Kota
- Office dosyalarının PDF'e çevrilmesi
- Dosya içinde arama
- Kilitleme
- Klasör takibi ve özet bildirimleri
- Kurum dışı paylaşım bağlantısı
- İndirme denetim kaydı
- Sahipliği elle devretme
- Kişisel alan
- Yıl etiketi

---

## §2 İş kuralları

### 2.1 Yetki seviyeleri

| Seviye | Kod (`rank`) | Yapabildikleri |
|---|---|---|
| Görüntüle | `VIEW` (1) | Klasörlerde gezinme, arama, önizleme, indirme, bağlantı kopyalama |
| Düzenle | `EDIT` (2) | Görüntüle'deki her şey + dosya yükleme ve üzerine yazma, klasör oluşturma, yeniden adlandırma, taşıma, çöp kutusuna silme, çöp kutusunu görme ve geri yükleme |
| Yönet | `MANAGE` (3) | Düzenle'deki her şey + yetki verme ve kaldırma, alanın adını ve açıklamasını düzenleme, kalıcı silme, çöp kutusunu boşaltma, boş alanı silme |

**Etkin seviyenin hesaplanması:**

- Alanın sahibi her zaman `MANAGE` seviyesindedir.
- Diğer kullanıcılar için kendisine uyan yetkilerin **en yükseği** geçerlidir.
- Uyan yetki yoksa alan o kişiye görünmez; alanla ilgili her istek `DMS_SPACE_NOT_FOUND` döner.

**Yetkinin kime uyduğu (`TargetRef` türleri):**

| Tür | Uyma koşulu |
|---|---|
| `USER` | Kişinin kendisi |
| `GROUP` | Kişi grubun şu anki üyesi (`me.groupIds` — arşivli gruplar dahil) |
| `BRANCH` | Kişinin **aktif yıldaki** şubesi |
| `ALL` | Aktif yıla atanmış herkes |

**Yetki kuralları:**

- Aynı özneye (aynı tür ve id) bir alanda tek yetki verilir. Yeni yetki eklenirken bu özne zaten varsa seviyesi güncellenir (upsert).
- Bir alanda en fazla 100 yetki satırı olabilir.
- Sahip, yetki listesinde ayrı bir satır olarak görünür ve kaldırılamaz.

### 2.2 Alan

| Kural | Ayrıntı |
|---|---|
| Kim açar | `me.canCreateDmsSpace()` true olan kişi (Yönetici ya da "Doküman alanı açabilir" işareti olan); değilse `DMS_CREATE_FORBIDDEN`. Alanı açan sahip olur. |
| Ad | 2–100 karakter. `name_key = fold(ad)` ile tekildir; aynı ad varsa `DMS_SPACE_NAME_TAKEN`. |
| Açıklama | En fazla 500 karakter |
| Silme (D-50) | `MANAGE` yetkisi gerekir. Alanda hiç klasör ya da dosya yoksa (çöp kutusu dahil) silinebilir; değilse `DMS_SPACE_NOT_EMPTY`. Fiziksel silinir. |
| Sahiplik (D-50) | Yalnızca pasifleştirmede, devralan kişiye geçer (§2.7). Elle devretme yoktur. |

### 2.3 Klasör ve dosya adları

**İzin verilen karakterler:**

- Trim sonrası ad 1–100 karakter olmalıdır (klasör). Dosya adında sınır, uzantı dahil 200 karakterdir.
- `/ \ : * ? " < > |` karakterleri ve kontrol karakterleri kullanılamaz.
- Ad `.` ya da `..` olamaz.
- Hata: `DMS_NAME_INVALID`.

**Aynı ad kuralı:**

- Aynı klasörde, silinmemiş öğeler arasında aynı adlı iki klasör ya da aynı adlı iki dosya olamaz. Klasör ile dosyanın aynı adı taşıması serbesttir.
- Karşılaştırma büyük-küçük harf duyarsızdır: `name_key = ad.toLowerCase(Locale.forLanguageTag("tr"))`. Türkçe harfler katlanmaz; yani "Sınav" ile "Sinav" farklı adlardır.
- Ad çakışırsa `DMS_NAME_EXISTS`.

**Dosya uzantısı:**

- Yeniden adlandırmada uzantı değiştirilemez (`DMS_EXTENSION_CHANGE`). Arayüzde yalnızca uzantısız ad düzenlenir; uzantı yanında sabit olarak görünür.

**Yükleme sırasında ad:**

- Yüklenen dosyanın adı `stored_file.original_name` değerinden gelir.
- Geçersiz karakterler `_` ile değiştirilir.

**Otomatik benzersiz ad (`uniqueName`):**

- Yöntem: Ad doluysa sonuna ` (2)`, ` (3)` … eklenir. Ek, uzantıdan önce yazılır: `Rapor (2).pdf`.
- Kullanıldığı durumlar: yüklemede `RENAME` seçeneği ve geri yükleme.

**Derinlik sınırı:** Klasör derinliği en fazla 10'dur; aşılırsa `DMS_DEPTH_LIMIT`.

### 2.4 Yükleme ve ad çakışması (D-20)

1. İstemci dosyayı `POST /api/files` (`purpose=DMS`) ile yükler.
2. Ardından `POST /api/dms/spaces/{id}/files { folderId, fileId, onConflict }` çağrılır. `onConflict` seçenekleri:

| onConflict | Aynı adlı dosya yoksa | Aynı adlı dosya varsa |
|---|---|---|
| `ASK` (varsayılan) | Kaydeder → 201 | 409 `DMS_NAME_CONFLICT`. `details[0]` mevcut dosyanın adıdır. Yüklenen `fileId` bağlanmaz; istemci aynı `fileId` ile tekrar çağırabilir. |
| `OVERWRITE` | Kaydeder → 201 | Mevcut `dms_file` kaydının `file_id`, `size_bytes`, `content_type`, `updated_by` ve `updated_at` alanları güncellenir. Eski `stored_file` commit sonrasında fiziksel olarak silinir. → 200 |
| `RENAME` | Kaydeder → 201 | `uniqueName` ile yeni ad verilir → 201 |

Kullanıcı "Vazgeç" seçerse bağlanmamış dosya 24 saat sonra çekirdeğin gece işiyle silinir.

### 2.5 Çöp kutusu (D-52)

**Silme (çöp kutusuna taşıma):**

| Öğe | Yapılanlar |
|---|---|
| Dosya | `deleted_at = now`, `deleted_by = me`, `trash_root = true` |
| Klasör F | F'ye `trash_root = true` ve `deleted_at` yazılır. F'nin silinmemiş tüm alt öğelerine (recursive CTE) `deleted_at = now`, `trash_root = false`, `trashed_with = F.id` yazılır. |

**Listeleme:** Çöp kutusunda yalnızca `trash_root = true` olan öğeler görünür. Normal listelerde ve aramada yalnızca `deleted_at IS NULL` öğeler görünür.

**Geri yükleme:**

- Klasör F geri yüklenince F ve `trashed_with = F.id` olan tüm öğeler geri gelir.
- Öğenin üst klasörü artık yoksa (silinmiş ya da kalıcı silinmiş) öğe alanın ana dizinine (`folder_id`/`parent_id = NULL`) yüklenir. Yanıtta `movedToRoot: true` döner.
- Hedefte aynı ad varsa öğeye `uniqueName` ile yeni ad verilir. Yanıtta `renamedTo` döner.

**Kalıcı silme:** `MANAGE` yetkisi gerekir. Öğe ve tüm alt ağacı hard delete ile silinir. Bu, alt ağaçta tek tek çöpe atılmış öğeleri de kapsar. Bağlı `stored_file` dosyaları fiziksel olarak silinir.

**Otomatik temizlik:** `DmsJobs` her gece 04:30'da çalışır. `trash_root = true AND deleted_at < now - app.dms.trash-retention-days` (varsayılan 30) koşulundaki öğeleri kalıcı olarak siler.

### 2.6 Taşıma (D-49)

- **Yetki:** `EDIT` gerekir.
- **Hedef:** Aynı alandaki silinmemiş bir klasör ya da ana dizin (`null`).
- **Geçersiz taşıma:** Klasör kendisine ya da kendi alt klasörüne taşınamaz. Hedef başka bir alana aitse ya da silinmişse de taşıma yapılamaz. Bu durumların hepsinde `DMS_MOVE_INVALID` döner.
- **Diğer kontroller:** Hedefte aynı ad varsa `DMS_NAME_EXISTS`. Taşıma sonrası derinlik 10'u aşarsa `DMS_DEPTH_LIMIT`.

### 2.7 Pasifleştirme (`DmsDeactivationHook implements UserDeactivationHook`)

| Metot | Davranış |
|---|---|
| `describe(u)` | Kişinin sahibi olduğu alan sayısını döner. Etiket: "doküman alanının sahibi". |
| `transfer(u, s)` | Bu alanların `owner_id` değeri `s` olur. Kişiye ait `USER` tipindeki yetki satırları tüm alanlardan silinir. Devralana `DMS_SPACE_OWNER_CHANGED` bildirimi gider. |

### 2.8 Bildirimler (D-51)

| Olay | Kime | type | title | link |
|---|---|---|---|---|
| Alana `USER` yetkisi eklendi (yeni satır; seviye değişikliğinde gönderilmez) | O kişi | `DMS_SPACE_SHARED` | "{verenAdı} sizinle bir doküman alanı paylaştı: {alan}" | `/dokumanlar/{id}` |
| Pasifleştirmede sahiplik devredildi | Devralan | `DMS_SPACE_OWNER_CHANGED` | "Doküman alanının yeni sahibi oldunuz: {alan}" | `/dokumanlar/{id}` |

- `GROUP`, `BRANCH` ve `ALL` yetkilerinde bildirim gönderilmez.
- Bu modülün WebSocket olayı ve rozeti yoktur.

---

## §3 Veritabanı — `V5__dms.sql`

```sql
CREATE TABLE dms_space (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name        varchar(100) NOT NULL,
  name_key    varchar(100) NOT NULL,
  description varchar(500),
  owner_id    bigint       NOT NULL REFERENCES app_user(id),
  created_at  timestamptz  NOT NULL DEFAULT now(),
  updated_at  timestamptz  NOT NULL DEFAULT now(),
  CONSTRAINT ux_dms_space_name UNIQUE (name_key)
);

CREATE TABLE dms_space_grant (
  id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  space_id     bigint      NOT NULL REFERENCES dms_space(id) ON DELETE CASCADE,
  subject_type varchar(10) NOT NULL CHECK (subject_type IN ('USER','GROUP','BRANCH','ALL')),
  subject_id   bigint,
  level        smallint    NOT NULL CHECK (level IN (1,2,3)),   -- 1=VIEW 2=EDIT 3=MANAGE
  created_by   bigint      NOT NULL REFERENCES app_user(id),
  created_at   timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ck_dms_grant_id CHECK ((subject_type = 'ALL') = (subject_id IS NULL))
);
CREATE UNIQUE INDEX ux_dms_grant ON dms_space_grant (space_id, subject_type, COALESCE(subject_id, 0));
CREATE INDEX ix_dms_grant_subject ON dms_space_grant (subject_type, subject_id);

CREATE TABLE dms_folder (
  id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  space_id     bigint       NOT NULL REFERENCES dms_space(id),
  parent_id    bigint       REFERENCES dms_folder(id),
  name         varchar(100) NOT NULL,
  name_key     varchar(100) NOT NULL,
  search_key   varchar(200) NOT NULL,
  depth        smallint     NOT NULL,                          -- ana dizindeki klasör = 1
  created_by   bigint       NOT NULL REFERENCES app_user(id),
  created_at   timestamptz  NOT NULL DEFAULT now(),
  updated_at   timestamptz  NOT NULL DEFAULT now(),
  deleted_at   timestamptz,
  deleted_by   bigint       REFERENCES app_user(id),
  trash_root   boolean      NOT NULL DEFAULT false,
  trashed_with bigint
);
CREATE UNIQUE INDEX ux_dms_folder_name ON dms_folder (space_id, COALESCE(parent_id, 0), name_key) WHERE deleted_at IS NULL;
CREATE INDEX ix_dms_folder_parent ON dms_folder (space_id, parent_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_dms_folder_trash  ON dms_folder (space_id, deleted_at) WHERE trash_root;

CREATE TABLE dms_file (
  id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  space_id     bigint       NOT NULL REFERENCES dms_space(id),
  folder_id    bigint       REFERENCES dms_folder(id),
  name         varchar(200) NOT NULL,
  name_key     varchar(200) NOT NULL,
  search_key   varchar(300) NOT NULL,
  file_id      uuid         NOT NULL REFERENCES stored_file(id),
  size_bytes   bigint       NOT NULL,
  content_type varchar(150) NOT NULL,
  uploaded_by  bigint       NOT NULL REFERENCES app_user(id),
  updated_by   bigint       NOT NULL REFERENCES app_user(id),
  created_at   timestamptz  NOT NULL DEFAULT now(),
  updated_at   timestamptz  NOT NULL DEFAULT now(),
  deleted_at   timestamptz,
  deleted_by   bigint       REFERENCES app_user(id),
  trash_root   boolean      NOT NULL DEFAULT false,
  trashed_with bigint
);
CREATE UNIQUE INDEX ux_dms_file_name ON dms_file (space_id, COALESCE(folder_id, 0), name_key) WHERE deleted_at IS NULL;
CREATE INDEX ix_dms_file_folder ON dms_file (space_id, folder_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_dms_file_trash  ON dms_file (space_id, deleted_at) WHERE trash_root;
```

`search_key = TextNormalizer.fold(name)` kuralıyla hesaplanır.

`application.yml` dosyasına eklenecek ayar:

```yaml
app.dms: { trash-retention-days: 30 }
```

---

## §4 Sınıflar (`dms/`)

| Sınıf | Görev |
|---|---|
| `DmsSpace`, `DmsSpaceGrant`, `DmsFolder`, `DmsFile` | Entity'ler. `DmsLevel { VIEW(1), EDIT(2), MANAGE(3) }`, `@Converter` ile `smallint` olarak saklanır. |
| `DmsAccess` | `level(me, spaceId)`, `require(me, spaceId, DmsLevel)`, `accessibleSpaceLevels(me)` (§5.1) |
| `DmsSpaceService` | Alan oluşturma, düzenleme, silme; yetki ekleme, güncelleme, kaldırma; bildirim |
| `DmsItemService` | Klasör oluşturma, yeniden adlandırma, taşıma; dosya kaydetme (çakışma yönetimi), yeniden adlandırma, taşıma; çöp kutusu işlemleri; `uniqueName` |
| `DmsQueries` | Alan listesi (sayaçlarla), klasör içeriği, breadcrumb, klasör ağacı, çöp kutusu, arama (yol bilgisiyle) |
| `DmsNameRules` | Ad doğrulama ve temizleme, `name_key` üretimi, uzantı ayırma |
| `DmsDeactivationHook`, `DmsJobs` | §2.7, §2.5 |
| `DmsController`, `DmsDtos`, `DmsErrorCode`, `DmsAuditAction` | |
| `DmsDevSeeder` | Yalnızca `dev` profilinde (§8) |

---

## §5 Sorgular

### 5.1 Etkin seviye

Sorgu, `GROUP` koşulunu yalnızca `me.groupIds` boş değilse, `BRANCH` koşulunu yalnızca kişinin şubesi varsa ekler. Sorgu dinamik kurulur.

```sql
SELECT s.id AS space_id,
       CASE WHEN s.owner_id = :uid THEN 3 ELSE COALESCE(max(g.level), 0) END AS level
FROM dms_space s
LEFT JOIN dms_space_grant g ON g.space_id = s.id AND (
       (g.subject_type = 'USER'   AND g.subject_id = :uid)
    OR (g.subject_type = 'GROUP'  AND g.subject_id IN (:groupIds))
    OR (g.subject_type = 'BRANCH' AND g.subject_id = :branchId)
    OR (g.subject_type = 'ALL'))
WHERE (:spaceId IS NULL OR s.id = :spaceId)
GROUP BY s.id, s.owner_id
HAVING s.owner_id = :uid OR max(g.level) IS NOT NULL
```

### 5.2 Alan listesi sayaçları

```sql
SELECT space_id, count(*) AS file_count, COALESCE(sum(size_bytes), 0) AS total_bytes, max(updated_at) AS last_update
FROM dms_file WHERE deleted_at IS NULL AND space_id IN (:ids) GROUP BY space_id
```

### 5.3 Yol (breadcrumb, arama ve çöp kutusu için)

Bir klasörün yolu recursive CTE ile hesaplanır:

```sql
WITH RECURSIVE up AS (
  SELECT id, parent_id, name, 1 AS lvl FROM dms_folder WHERE id = :folderId
  UNION ALL
  SELECT f.id, f.parent_id, f.name, up.lvl + 1 FROM dms_folder f JOIN up ON f.id = up.parent_id)
SELECT id, name FROM up ORDER BY lvl DESC
```

- **Arama sonuçları:** Klasör yolları `dms_folder` tablosu tek sorguyla belleğe alınarak hesaplanır. Bir alandaki klasör sayısı yüzlerle sınırlı olduğu için bu yeterlidir.
- **Yol metni biçimi:** `"Alan / Klasör / Alt Klasör"`.

### 5.4 Arama

```sql
-- klasörler ve dosyalar için ayrı ayrı; en fazla 25 + 25 sonuç
SELECT ... FROM dms_file f WHERE f.deleted_at IS NULL AND f.space_id IN (:accessibleIds) AND f.search_key LIKE :q
ORDER BY f.name COLLATE "tr-x-icu" LIMIT 25
```

`q` en az 2 karakter olmalıdır; `%fold(q)%` biçiminde aranır.

---

## §6 API

### 6.1 DTO'lar

```ts
type DmsLevel = 'VIEW'|'EDIT'|'MANAGE';
type DmsPerson = { id: number; fullName: string };
type SpaceRow = { id: number; name: string; description?: string; owner: DmsPerson; myLevel: DmsLevel;
                  fileCount: number; totalBytes: number; lastUpdate?: string };
type GrantRow = { id: number; subject: TargetView; level: DmsLevel };              // TargetView: 01 §6.4
type FolderRow = { id: number; name: string; updatedAt: string; createdBy: DmsPerson; itemCount: number };
type FileRow = { id: number; name: string; sizeBytes: number; contentType: string; previewable: boolean;
                 updatedAt: string; updatedBy: DmsPerson; uploadedBy: DmsPerson; createdAt: string };
type FolderContent = { space: SpaceRow; folder?: { id: number; name: string; parentId?: number };
                       breadcrumb: { id: number; name: string }[]; folders: FolderRow[]; files: FileRow[] };
type FolderNode = { id: number; parentId?: number; name: string; depth: number };
type TrashRow = { type: 'FOLDER'|'FILE'; id: number; name: string; originalPath: string; deletedBy: DmsPerson;
                  deletedAt: string; purgeAt: string; sizeBytes?: number };
type SearchResult = { type: 'FOLDER'|'FILE'; id: number; name: string; spaceId: number; spaceName: string;
                      folderId?: number; path: string; sizeBytes?: number; contentType?: string };
type RestoreResult = { movedToRoot: boolean; renamedTo?: string };
```

**Alan değerlerinin hesaplanması:**

- `previewable`: `contentType` değeri `application/pdf` ise ya da `image/` ile başlıyorsa `true`.
- `itemCount`: Klasörün doğrudan içindeki silinmemiş alt klasör ve dosya sayısı.
- `purgeAt`: `deletedAt + saklama süresi`.

### 6.2 Uç noktalar

**Genel kurallar:**

- Tüm uçlarda `me.requireMember()` çağrılır.
- Alan, klasör ya da dosya id'si alan uçlarda önce ilgili alanın etkin seviyesi bulunur:
  - Seviye 0 ise → `DMS_SPACE_NOT_FOUND`
  - Klasör veya dosya alanda yoksa ya da silinmişse → `DMS_ITEM_NOT_FOUND`
  - Seviye gereken seviyenin altındaysa → `DMS_EDIT_FORBIDDEN({mevcutSeviyeEtiketi})` ya da `DMS_MANAGE_FORBIDDEN`
- Seviye etiketleri: VIEW → "Görüntüleme", EDIT → "Düzenleme", MANAGE → "Yönetim".

**Alanlar ve yetkiler**

| Uç nokta | Seviye | İstek | Yanıt | Kurallar |
|---|---|---|---|---|
| `GET /api/dms/spaces` | — | — | `SpaceRow[]` | Erişilebilen alanlar, Türkçe ad sıralı |
| `POST /api/dms/spaces` | §2.2 | `{ name, description? }` | 201 `SpaceRow` | Hatalar: `DMS_CREATE_FORBIDDEN`, `DMS_SPACE_NAME_TAKEN` (alan: name). Denetim: `DMS_SPACE_CREATED`. |
| `GET /api/dms/spaces/{id}` | VIEW | — | `SpaceRow` | |
| `PUT /api/dms/spaces/{id}` | MANAGE | `{ name, description? }` | `SpaceRow` | Denetim: `DMS_SPACE_UPDATED` |
| `DELETE /api/dms/spaces/{id}` | MANAGE | — | 204 | `DMS_SPACE_NOT_EMPTY`. Denetim: `DMS_SPACE_DELETED`. |
| `GET /api/dms/spaces/{id}/grants` | MANAGE | — | `GrantRow[]` | Sıralama: önce tür (ALL, BRANCH, GROUP, USER), sonra ad |
| `POST /api/dms/spaces/{id}/grants` | MANAGE | `{ subject: TargetRef, level }` | 201 `GrantRow` | `AudienceResolver.validate`. Sahibin kendisine `USER` yetkisi verilmek istenirse `DMS_GRANT_OWNER`. 100 satır sınırı aşılırsa `DMS_GRANT_LIMIT`. Özne varsa upsert yapılır ve 200 döner. Bildirim §2.8. Denetim: `DMS_GRANT_CHANGED`, detayda `{ subject, level, op:'ADD' }`. |
| `PUT /api/dms/spaces/{id}/grants/{gid}` | MANAGE | `{ level }` | `GrantRow` | Denetim: `op:'UPDATE'` |
| `DELETE /api/dms/spaces/{id}/grants/{gid}` | MANAGE | — | 204 | Denetim: `op:'REMOVE'` |

**Klasörler ve dosyalar**

| Uç nokta | Seviye | İstek | Yanıt | Kurallar |
|---|---|---|---|---|
| `GET /api/dms/spaces/{id}/items?folderId=` | VIEW | — | `FolderContent` | Önce klasörler, sonra dosyalar; Türkçe ad sıralı. Sayfalama yok. |
| `GET /api/dms/spaces/{id}/folders` | VIEW | — | `FolderNode[]` | Silinmemiş tüm klasörler (taşıma penceresi için) |
| `POST /api/dms/spaces/{id}/folders` | EDIT | `{ parentId?, name }` | 201 `FolderRow` | §2.3. Hatalar: `DMS_NAME_INVALID`, `DMS_NAME_EXISTS`, `DMS_DEPTH_LIMIT`. |
| `PUT /api/dms/folders/{id}` | EDIT | `{ name }` | `FolderRow` | Yeniden adlandırma |
| `POST /api/dms/folders/{id}/move` | EDIT | `{ targetFolderId?: number }` | 204 | §2.6. Alt klasörlerin `depth` değeri de güncellenir. |
| `DELETE /api/dms/folders/{id}` | EDIT | — | 204 | §2.5. Denetim: `DMS_ITEM_TRASHED`. |
| `POST /api/dms/spaces/{id}/files` | EDIT | `{ folderId?, fileId, onConflict: 'ASK'\|'OVERWRITE'\|'RENAME' }` | 201 / 200 `FileRow` | §2.4. Dosya `attach(fileId, me, DMS)` ile bağlanır; bu çağrı yalnızca kayıt başarılı olursa yapılır. |
| `PUT /api/dms/files/{id}` | EDIT | `{ name }` | `FileRow` | `DMS_EXTENSION_CHANGE`, `DMS_NAME_EXISTS` |
| `POST /api/dms/files/{id}/move` | EDIT | `{ targetFolderId?: number }` | 204 | §2.6 |
| `DELETE /api/dms/files/{id}` | EDIT | — | 204 | Denetim: `DMS_ITEM_TRASHED` |
| `GET /api/dms/files/{id}/content?download=false` | VIEW | — | dosya akışı | `inline` yalnızca `previewable` dosyalar için; diğer dosyalar her zaman `attachment` |
| `GET /api/dms/files/{id}/locate` | VIEW | — | `{ spaceId, folderId? }` | Bağlantı rotası için (§7.6) |

**Çöp kutusu ve arama**

| Uç nokta | Seviye | İstek | Yanıt | Kurallar |
|---|---|---|---|---|
| `GET /api/dms/spaces/{id}/trash` | EDIT | — | `TrashRow[]` | `deleted_at DESC` |
| `POST /api/dms/trash/{type}/{itemId}/restore` | EDIT | — | `RestoreResult` | `type` değeri `folder` veya `file`. Öğe çöp kutusunda (`trash_root`) değilse `DMS_ITEM_NOT_FOUND`. Denetim: `DMS_ITEM_RESTORED`. |
| `DELETE /api/dms/trash/{type}/{itemId}` | MANAGE | — | 204 | Kalıcı silme. Denetim: `DMS_ITEM_PURGED`. |
| `DELETE /api/dms/spaces/{id}/trash` | MANAGE | — | `{ purged: number }` | Çöp kutusunu boşaltır. Denetim: `DMS_TRASH_EMPTIED`. |
| `GET /api/dms/search?q=` | — | — | `SearchResult[]` | §5.4. `q` 2 karakterden kısaysa boş dizi döner. |

### 6.3 Doğrulama tablosu

| Alan | Kural | Mesaj |
|---|---|---|
| Alan adı | Trim edilir, 2–100 karakter | "Alan adı 2–100 karakter olmalıdır." |
| Alan açıklaması | En fazla 500 karakter | "Açıklama en fazla 500 karakter olabilir." |
| Klasör adı | §2.3, 1–100 karakter | DMS_NAME_INVALID |
| Dosya adı | §2.3, uzantı dahil 1–200 karakter | DMS_NAME_INVALID |
| `level` | VIEW, EDIT veya MANAGE | "Bir yetki seviyesi seçin." |
| `onConflict` | ASK, OVERWRITE veya RENAME; verilmezse ASK | — |

---

## §7 Hata kodları (`DmsErrorCode`)

| code | HTTP | message |
|---|---|---|
| DMS_SPACE_NOT_FOUND | 404 | Doküman alanı bulunamadı veya bu alana erişiminiz yok. |
| DMS_ITEM_NOT_FOUND | 404 | Klasör veya dosya bulunamadı ya da silinmiş. |
| DMS_CREATE_FORBIDDEN | 403 | Doküman alanı açma yetkiniz yok. Okul yönetimiyle iletişime geçin. |
| DMS_EDIT_FORBIDDEN | 403 | Bu alanda değişiklik yapma yetkiniz yok (yetkiniz: {0}). |
| DMS_MANAGE_FORBIDDEN | 403 | Bu işlem için alanda "Yönetim" yetkisi gerekir. |
| DMS_SPACE_NAME_TAKEN | 409 | Bu adla bir doküman alanı zaten var. |
| DMS_SPACE_NOT_EMPTY | 409 | Alanı silmek için önce içindeki tüm klasör ve dosyaları silin ve çöp kutusunu boşaltın. |
| DMS_GRANT_OWNER | 400 | Alanın sahibi zaten tam yetkilidir. |
| DMS_GRANT_LIMIT | 400 | Bir alanda en fazla 100 yetki tanımlanabilir. Kişiler yerine grup veya şube seçmeyi deneyin. |
| DMS_NAME_INVALID | 400 | Ad boş olamaz, çok uzun olamaz ve şu karakterleri içeremez: / \ : * ? " < > \| |
| DMS_NAME_EXISTS | 409 | Bu konumda "{0}" adında bir öğe zaten var. Başka bir ad seçin. |
| DMS_NAME_CONFLICT | 409 | "{0}" adında bir dosya zaten var. |
| DMS_EXTENSION_CHANGE | 400 | Dosya uzantısı değiştirilemez. |
| DMS_DEPTH_LIMIT | 400 | Klasörler en fazla 10 kat iç içe olabilir. |
| DMS_MOVE_INVALID | 400 | Öğe bu konuma taşınamaz. Bir klasör kendi içine veya alt klasörüne taşınamaz. |

**Denetim işlemleri (`DmsAuditAction`):**

| Kod | Etiket |
|---|---|
| DMS_SPACE_CREATED | Doküman alanı oluşturuldu |
| DMS_SPACE_UPDATED | Doküman alanı güncellendi |
| DMS_SPACE_DELETED | Doküman alanı silindi |
| DMS_GRANT_CHANGED | Doküman alanı yetkisi değiştirildi |
| DMS_ITEM_TRASHED | Doküman çöp kutusuna taşındı |
| DMS_ITEM_RESTORED | Doküman geri yüklendi |
| DMS_ITEM_PURGED | Doküman kalıcı olarak silindi |
| DMS_TRASH_EMPTIED | Çöp kutusu boşaltıldı |

Denetim detayında her zaman `{ spaceId, name, type }` bulunur.

---

## §8 Geliştirme verisi (`DmsDevSeeder`)

| Alan | Sahip | Yetkiler | İçerik |
|---|---|---|---|
| "İdari Formlar" | mudur | ALL: VIEW; Yönetim Kurulu grubu: EDIT | "İzin Formları" klasörü ve içinde 1 PDF; ana dizinde 1 görsel |
| "Matematik Zümresi Arşivi" | zumre.mat | Matematik Zümresi grubu: EDIT | "2025-2026 Sınavlar" > "1. Dönem" alt klasörleri; 1 PDF; çöp kutusunda 1 dosya |
| "Kuzey Kampüs" | my.kuzey | BRANCH Kuzey: VIEW | 1 PDF |

---

## §9 Ekranlar

### 9.1 Alan listesi (`/dokumanlar`)

**Başlık ve aksiyonlar:**

- Başlık: "Dokümanlar".
- Başlığın altında tam genişlikte arama kutusu: "Tüm dokümanlarda ara (dosya veya klasör adı)". Sorgu 300 ms gecikmeyle gönderilir; yazılan değer URL'de `?ara=` olarak tutulur.
- `canCreateDmsSpace` yetkisi varsa birincil aksiyon "Alan Oluştur" butonudur (mobilde FAB).

**Arama sonuçları:** Arama kutusu doluyken alan ızgarası yerine sonuçlar gösterilir.

- Klasörler ve dosyalar iki ayrı bölümde listelenir.
- Her satırda: ikon, ad (eşleşen kısım `<mark>` ile vurgulu), altında gri yol, dosyalarda boyut.
- Klasöre tıklanınca o klasör açılır.
- Dosyaya tıklanınca dosyanın bulunduğu klasör açılır ve dosya satırı 2 saniye vurgulanır (`?vurgu={fileId}`).
- Sonuç yoksa: "'{q}' ile eşleşen dosya veya klasör bulunamadı."

**Alan ızgarası:** Masaüstünde 3, tablette 2, mobilde 1 sütundur. Her kartta:

- Üstte renkli bir klasör ikonu. Renk alan id'sinden avatar paletiyle türetilir.
- Alan adı (semibold) ve en fazla 2 satır açıklama
- Alt satırda gri metin: "{fileCount} dosya · {formatBytes(totalBytes)} · Son güncelleme {DateText}"
- Sağ üstte yetki rozeti: "Görüntüleme" (gri), "Düzenleme" (mavi), "Yönetim" (mor). Sahip olunan alanda "Sahibi" rozeti gösterilir.
- Karta tıklanınca alan açılır.

**Boş durum:**

- Alan oluşturma yetkisi varsa: "Henüz doküman alanı yok." ve "Alan Oluştur" butonu.
- Yoksa: "Size açılmış bir doküman alanı yok. Bir dokümana erişmeniz gerekiyorsa yöneticinizle iletişime geçin."

**Alan Oluştur penceresi:**

- Alanlar: Ad\* ve Açıklama.
- Başarıdan sonra yeni alan açılır ve toast gösterilir: "Alan oluşturuldu. Kimlerin erişeceğini belirlemek için 'Yetkiler'e tıklayın."
- Bu ilk açılışta Yetkiler penceresi otomatik olarak açılır.

### 9.2 Alan gezgini (`/dokumanlar/[spaceId]?klasor={folderId}`)

**Üst kısım:**

- Breadcrumb: "Dokümanlar › {Alan} › {Klasör} › {Alt}". Her parçası tıklanabilir. Mobilde yalnızca son iki parça görünür, öncekiler "…" menüsüne toplanır.
- Altında alan açıklaması (gri metin).
- Sağdaki butonlar:
  - EDIT ve üzeri: "Yükle" (birincil) ve "Yeni Klasör"
  - MANAGE: "Yetkiler"
  - ⋯ menüsü: "Çöp Kutusu" (EDIT ve üzeri), "Alan Ayarları" (MANAGE)
- Mobilde birincil aksiyon FAB'dır. FAB menüsünde "Dosya Yükle" ve "Klasör Oluştur" seçenekleri bulunur.

**İçerik listesi (masaüstü tablo):**

| Sütun | İçerik |
|---|---|
| Ad | Türe göre ikon (Folder sarı, FileText pdf kırmızı, FileSpreadsheet yeşil, FileImage mor, FileArchive gri, File) ve ad |
| Boyut | Klasörlerde "{n} öğe" |
| Son değiştiren | |
| Değiştirilme | `DateText` |
| ⋯ | Satır menüsü |

- **Satır menüsü:**
  - Dosyalarda: "Önizle" (yalnızca `previewable` dosyalarda), "İndir", "Bağlantıyı kopyala"
  - Klasörlerde: "Aç"
  - EDIT ve üzeri: "Yeniden adlandır", "Taşı", "Sil"
- **Mobil liste:** Her satırda ikon, ad (tek satır) ve altında gri metin: "{boyut} · {DateText}". Sağda ⋯ menüsü.
- **Tıklama:**
  - Klasör satırı → `?klasor=` ile açılır.
  - `previewable` dosya → önizlenir. Görseller `ImageViewer` ile açılır; klasördeki tüm görseller arasında gezinilir. PDF yeni sekmede açılır (`content?download=false`).
  - Diğer dosyalar → indirilir.
- **Boş klasör:** EDIT ve üzeriyse: FolderOpen ikonu, "Bu klasör boş." ve "Dosya Yükle" butonu. VIEW ise yalnızca "Bu klasör boş."

**Sürükle-bırak:** EDIT ve üzeri yetkisi olan kullanıcı içerik alanına dosya sürükleyince alanın tamamı kesik çizgili `primary` bir çerçeveyle kaplanır ve "Dosyaları bu klasöre bırakın" yazar.

**Yükleme süreci:**

- Dosyalar **sırayla** yüklenir: önce `/api/files`, ardından kayıt çağrısı.
- Sağ altta sabit bir "Yüklemeler" paneli durur (masaüstünde 360px, mobilde tam genişlik). Her dosya için ad, ilerleme çubuğu ve durum gösterilir: ✓, ✕ (hata mesajıyla) ya da "Bekliyor".
- Panelin başlığında "{n} dosya yükleniyor" yazar. Yüklemeler bitince başlık "Yükleme tamamlandı" olur ve panel kapatılabilir.
- Hata alan dosya listede kalır ve "Tekrar dene" bağlantısı gösterilir.

**Ad çakışması penceresi:** `DMS_NAME_CONFLICT` hatasında açılır.

- Metin: "'{ad}' adında bir dosya bu klasörde zaten var."
- Butonlar:
  - "Üzerine Yaz" (destructive). Yardım metni: "Eski dosya kalıcı olarak değiştirilir."
  - "İkisini de Sakla". Yeni dosya "{ad} (2)" olarak kaydedilir.
  - "Bu Dosyayı Atla"
- ☐ "Kalan tüm çakışmalar için aynısını yap"

**Bitiş toast'ı:** "{n} dosya yüklendi." Atlanan dosya varsa: "{n} dosya yüklendi, {m} dosya atlandı."

**Diğer işlem pencereleri:**

| İşlem | Pencere | Toast |
|---|---|---|
| Yeni Klasör | Tek alan "Klasör adı". | "Klasör oluşturuldu." |
| Yeniden adlandır | Dosyalarda yalnızca uzantısız ad düzenlenir; uzantı alanın sağında gri ve sabit görünür. | "Yeniden adlandırıldı." |
| Taşı | Alanın klasör ağacı (`/folders` verisi, istemcide ağaç olarak kurulur). En üstte "Ana dizin" seçeneği bulunur. Taşınan klasör ve alt klasörleri seçilemez. Buton: "Buraya Taşı". | "Taşındı." |
| Sil | Klasör için: "'{ad}' klasörü ve içindeki her şey çöp kutusuna taşınacak. 30 gün içinde geri yükleyebilirsiniz." Dosya için: "'{ad}' çöp kutusuna taşınacak." | "Çöp kutusuna taşındı." Toast'ta "Geri Al" aksiyonu vardır; restore çağırır. |
| Bağlantıyı kopyala | `navigator.clipboard.writeText(origin + '/dokumanlar/dosya/' + id)` | "Bağlantı kopyalandı. Yalnızca bu alana erişimi olan kişiler açabilir." |

### 9.3 Yetkiler penceresi (MANAGE)

- Masaüstünde 560px genişliğinde, mobilde tam ekran açılır.
- Başlık: "Yetkiler — {Alan}".

**Seviye açıklaması:** Pencerenin üstünde gri bir kutu üç seviyeyi tek satırlık açıklamalarla anlatır:

- Görüntüleme: "Dosyaları görür ve indirir."
- Düzenleme: "Ayrıca yükler, klasör açar, adlandırır, taşır ve siler."
- Yönetim: "Ayrıca yetki verir ve alan ayarlarını değiştirir."

**Yetki ekleme satırı:**

- `TargetPicker`: tüm türler seçilebilir, grup kapsamı `all`.
- Seviye seçimi; varsayılan "Görüntüleme".
- "Ekle" butonu. Toast: "Yetki eklendi."

**Yetki listesi:**

- İlk satır sahiptir: avatar, "{ad} · Sahip" ve sabit "Yönetim" etiketi. Silme butonu yoktur.
- Diğer satırlarda: hedef ikonu ve adı, seviye seçimi (değişiklik hemen kaydedilir; toast "Yetki güncellendi.") ve ✕ butonu (onay penceresiyle; toast "Yetki kaldırıldı.").
- Kişi kendi yetkisini düşürmeye ya da kaldırmaya çalışırsa ek uyarı çıkar: "Kendi yetkinizi değiştiriyorsunuz; bu alanı yönetemeyebilirsiniz."

**Boş liste:** "Bu alana şu an yalnızca siz erişebilirsiniz."

### 9.4 Alan Ayarları (MANAGE)

- Ad ve Açıklama düzenlenir; buton "Kaydet".
- Altında "Tehlikeli bölge" kartı ve "Alanı Sil" butonu. Onay: "'{ad}' alanı kalıcı olarak silinecek." Alan boş değilse backend hatası toast olarak gösterilir.

### 9.5 Çöp Kutusu (`/dokumanlar/[spaceId]/cop`)

- Başlık: "Çöp Kutusu — {Alan}".
- Bilgi metni: "Silinen öğeler 30 gün sonra kalıcı olarak silinir."
- MANAGE ise "Çöp Kutusunu Boşalt" butonu (destructive) bulunur. Onay: "Çöp kutusundaki {n} öğe kalıcı olarak silinecek. Bu işlem geri alınamaz."

**Satırlar:**

- İkon ve ad; altında gri "Eski konum: {originalPath}".
- "{deletedBy} sildi · {DateText}" ve "{n} gün sonra kalıcı silinecek". 3 gün ve daha az kaldıysa bu metin kırmızı yazılır.
- Butonlar: "Geri Yükle" ve (MANAGE ise) "Kalıcı Sil".

**Geri yükleme toast'ı:** "Geri yüklendi." Duruma göre ek metin:

- `movedToRoot`: "Eski klasörü bulunamadığı için ana dizine geri yüklendi."
- `renamedTo`: "Aynı adda öğe olduğu için '{renamedTo}' olarak geri yüklendi."

**Boş durum:** "Çöp kutusu boş."

### 9.6 Bağlantı rotası (`/dokumanlar/dosya/[fileId]`)

- Sayfa `GET /files/{id}/locate` çağırır ve `/dokumanlar/{spaceId}?klasor={folderId}&vurgu={fileId}` adresine yönlendirir.
- Hata durumunda `EmptyState` gösterilir: "Bu dosya bulunamadı veya erişiminiz yok. Erişime ihtiyacınız varsa alanın yöneticisiyle iletişime geçin."

---

## §10 Zorunlu testler

| No | Senaryo |
|---|---|
| T-D1 | **Yetki:**<br>(a) Etkin seviye `USER`, `GROUP`, `BRANCH` ve `ALL` yetkilerinin en yükseğidir.<br>(b) Sahibin seviyesi her zaman `MANAGE`'dir.<br>(c) Yetkisi olmayan kişi `DMS_SPACE_NOT_FOUND` alır.<br>(d) VIEW yetkili kişi dosya yükleyince `DMS_EDIT_FORBIDDEN` alır.<br>(e) Gruptan çıkarılan kişi grup yetkisini kaybeder. |
| T-D2 | **Ad çakışması:**<br>(a) `ASK` ile `DMS_NAME_CONFLICT` döner.<br>(b) `OVERWRITE` aynı kaydın `file_id` değerini değiştirir ve eski `stored_file` silinir.<br>(c) `RENAME` "ad (2).pdf" adını üretir. |
| T-D3 | **Çöp kutusu:**<br>(a) Silinen klasörün alt öğeleri listede görünmez.<br>(b) Klasör geri yüklenince alt öğeleri de geri gelir.<br>(c) Saklama süresi dolmuş öğeyi temizlik işi kalıcı olarak siler ve dosyaları fiziksel olarak kaldırır. |
| T-D4 | **Hook ve yıl:**<br>(a) Pasifleştirmede alan sahipliği devralana geçer.<br>(b) Yeni yıl aktifleştikten sonra (kişi yeni yıla da atanmışsa) `ALL` ve `USER` yetkileri çalışmaya devam eder. |
| T-D5 | **Taşıma:** Klasör kendi alt klasörüne taşınmak istenince `DMS_MOVE_INVALID` döner. |
