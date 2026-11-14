# 03 — Fotokopi (Talep ve Onay)

Fotokopi talepleri bu modülde dijital, izlenebilir ve onaylı bir akışla yürür:

1. Personel talebi gönderir.
2. Fotokopi onaylayıcısı talebi onaylar. Onaylayıcı tanımlı değilse bu adım atlanır.
3. Personelin şubesindeki fotokopi sorumlusu talebi hazırlar ve teslim eder.

Bağlı kararlar: D-07, D-13 – D-17, D-35 – D-42. Ortak kurallar `00-mimari.md`'de, hook'lar `01-cekirdek.md` §8'de tanımlıdır.

---

## §1 Kapsam

### Var

- Talep formu (tek dosya ya da basılı orijinal), PDF'ten otomatik sayfa sayısı, canlı toplam hesabı
- Onay akışı: onayla, reddet (gerekçe zorunlu); onaylayıcı tanımlı değilse talep otomatik onaylanır
- Vekil onaylayıcı (tarih aralığıyla)
- Fotokopi kuyruğu (şube bazlı pano): Hazırlamaya başla → Hazır → Teslim edildi
- Talep sahibinin iptali, "Kopyala ve yeniden gönder"
- Gizli evrak
- Durum geçmişi
- Bildirimler, menü rozetleri
- Aylık kişi bazlı rapor ve CSV indirme
- Dosyaların saklama süresi sonunda otomatik silinmesi
- Yıl kapanışında açık taleplerin iptali
- Pasifleştirme sırasında bekleyen onayların devri

### Yok

Aşağıdakiler **yapılmaz**:

- İkinci onay
- Zaman aşımı veya otomatik yükseltme
- "Düzeltme iste"
- "Beklemede" durumu
- Çoklu dosya
- Sayfa aralığı alanı (gerekirse "Not" alanına yazılır)
- Ders/sınıf ve kullanım amacı alanları
- Sonlandırma seçenekleri (zımba, kitapçık vb.)
- Kâğıt tipi
- Maliyet hesabı
- Toplu onay
- Sürükle-bırak pano
- Fotokopi makinesi entegrasyonu
- Mesajlaşma botu

---

## §2 İş kuralları

### 2.1 Durumlar ve geçişler

| Durum (`PrintStatus`) | Arayüz etiketi | Ton |
|---|---|---|
| `PENDING_APPROVAL` | Onay Bekliyor | warning |
| `APPROVED` | Onaylandı | info |
| `IN_PROGRESS` | Hazırlanıyor | info |
| `READY` | Hazır | success |
| `DELIVERED` | Teslim Edildi | neutral |
| `REJECTED` | Reddedildi | danger |
| `CANCELLED` | İptal Edildi | neutral |

| Geçiş | Kim yapar | Koşul |
|---|---|---|
| (yeni) → `PENDING_APPROVAL` | Talep sahibi | Onaylayıcı belirlendiyse (§2.2) |
| (yeni) → `APPROVED` | Sistem | Onaylayıcı yoksa (otomatik) |
| `PENDING_APPROVAL` → `APPROVED` | Atanan onaylayıcı (`approver_id = me`) | — |
| `PENDING_APPROVAL` → `REJECTED` | Atanan onaylayıcı | Gerekçe zorunlu (5–500 karakter) |
| `PENDING_APPROVAL` / `APPROVED` → `CANCELLED` | Talep sahibi | Hazırlık başlamadıysa (D-35) |
| `APPROVED` → `IN_PROGRESS` | Şubenin fotokopi sorumlusu | — |
| `APPROVED` / `IN_PROGRESS` → `READY` | Şubenin fotokopi sorumlusu | Hazırlık adımı atlanabilir (D-40) |
| `READY` → `DELIVERED` | Şubenin fotokopi sorumlusu | — |
| Açık durumlar → `CANCELLED` | Sistem | Yıl kapanışında (§2.7) |

**Açık durumlar:** `PENDING_APPROVAL`, `APPROVED`, `IN_PROGRESS`, `READY`.

**Geçiş kuralları:**

- **Geri dönüş yoktur.** Listede olmayan her geçiş `PRINT_INVALID_STATUS({mevcutEtiket})` döner.
- **Yarış koşulları:** `PrintRequest` entity'sinde `@Version` bulunur. İki sorumlu aynı anda işlem yaparsa ikincisi `CONCURRENT_MODIFICATION` alır.
- **Geçmiş kaydı:** Her geçiş `print_request_history` tablosuna yazılır. Kayıt şunları içerir: önceki durum, yeni durum, işlemi yapan kişi (sistemse null) ve not (red gerekçesi, iptal nedeni, "Vekil olarak atandı" gibi).
- **Geçmiş yıl:** Aktif yıl dışındaki taleplerde hiçbir işlem yapılamaz (`requireWritable`). Kapanış zaten açık talepleri iptal eder.

### 2.2 Onaylayıcı belirleme (talep gönderilirken)

1. `a = me.membership.printApproverId` (talep sahibinin aktif yıldaki fotokopi onaylayıcısı).
2. `a` null ise talep `APPROVED` durumunda başlar ve `approver_id = NULL` olur. Geçmiş notu: "Onaylayıcı tanımlı değil; otomatik onaylandı."
3. `a` için bugün geçerli bir vekâlet varsa ve vekil talep sahibi değilse:
   - `approver_id = vekil`, `original_approver_id = a`
   - Geçmiş notu: "{a} izinde; talep vekil {vekil} kişisine yönlendirildi."
   - Vekâletin geçerliliği Europe/Istanbul tarihine göre `start_date <= bugün <= end_date` koşuluyla belirlenir.
4. Diğer durumlarda `approver_id = a` olur.
5. **Tek adım kuralı:** Vekilin de vekili olsa zincir izlenmez; yalnızca tek adım uygulanır.
6. **Değişmezlik:** Onaylayıcı talep gönderildiği anda belirlenir. Sonradan atama veya vekâlet değişirse bekleyen talep etkilenmez. Tek istisna pasifleştirmedir (§2.8).

### 2.3 Kuyruk şubesi ve görünürlük

- **Kuyruk şubesi:** `branch_id`, talep anında talep sahibinin aktif yıldaki şubesidir. Şube seçimi yoktur (D-36).
- **Fotokopi sorumlusu:** `me.membership.printOperator && me.membership.branchId == request.branchId && request.seasonId == aktif yıl` koşulunu sağlayan kişidir.

Talebi kimin görebileceği:

| Kim | Görebildiği talepler |
|---|---|
| Talep sahibi | Kendi talepleri (tüm yıllar, tüm durumlar) |
| Onaylayıcı | `approver_id = me` olan veya kararını kendisinin verdiği (`decided_by = me`) talepler |
| Fotokopi sorumlusu | Kendi şubesinde, aktif yılda, `PENDING_APPROVAL` ve `REJECTED` dışındaki talepler |
| Yönetici | Tekil talep **göremez**; yalnızca raporu görür (D-42) |

Görünmeyen talep istenirse `PRINT_REQUEST_NOT_FOUND` döner.

### 2.4 Gizli evrak

- **Listeler:** Talep sahibi dışındaki herkes için `title` alanı "Gizli evrak" olarak maskelenir: kuyruk, onay kutusu ve bildirim metinleri. Detay sayfasında yetkili kişiler gerçek açıklamayı görür.
- **Dosya erişimi:** Talep sahibi dışında biri dosyayı her açtığında veya indirdiğinde denetim kaydı yazılır: `PRINT_SECRET_FILE_ACCESSED`, detayda `{ requestId }`.
- **Saklama:** Gizli dosyalar normal dosyalarla aynı saklama kuralına tabidir (§2.6).

### 2.5 Teslim zamanı ve sınırlar

| Kural | Değer |
|---|---|
| En erken teslim zamanı | `now + app.print.min-lead-minutes` (varsayılan 60) |
| En geç teslim zamanı | `now + app.print.max-days-ahead` (varsayılan 60) gün |
| Sayfa sayısı | 1–2000 |
| Kopya adedi | 1–1000 |
| Toplam baskı (`total_pages = sayfa × kopya`) | En fazla 20.000 |

- **Yaprak sayısı** yalnızca arayüzde gösterilir, saklanmaz. Formülü: `ceil(sayfa / (çift ? 2 : 1)) × kopya`.
- **Rapor**, `total_pages` değerini kullanır (D-39).

### 2.6 Dosya saklama (D-17, D-41)

- **Silme işi:** Her gece 04:00'te çalışır.
- **Silinen dosyalar:** Durumu `DELIVERED`, `REJECTED` veya `CANCELLED` olan ve kapanış zamanı (`delivered_at`, `decided_at` veya `cancelled_at`) `app.print.file-retention-days` günden eski taleplerin dosyaları.
- **İşlem:** Fiziksel dosya silinir (`FileStorageService.delete`) ve talepte `file_purged_at = now` yazılır. Talep kaydı kalır.
- **Arayüz:** Silinmiş dosya için "Dosya saklama süresi dolduğu için silindi" yazar.

### 2.7 Yıl kapanışı (`PrintSeasonHook implements SeasonActivationHook`)

| Metot | Davranış |
|---|---|
| `preview(eski)` | Açık talep varsa: "{eski} yılına ait {n} açık fotokopi talebi iptal edilecek." |
| `onActivated(eski, yeni)` | Eski yılın açık talepleri `CANCELLED` olur. `cancel_reason = 'Eğitim-öğretim yılı kapandı'`, `cancelled_by = NULL`. Geçmiş kaydı yazılır. Bildirim gönderilmez. |

### 2.8 Pasifleştirme ve yıldan çıkarma

**`PrintDeactivationHook implements UserDeactivationHook`:**

- `describe(u)`: `approver_id = u` olan `PENDING_APPROVAL` talep sayısını döner. Etiket: "onayını bekleyen fotokopi talebi".
- `transfer(u, s)`:
  1. `approver_id = u` olan bekleyen talepler `approver_id = s` olur.
     - `s` talep sahibinin kendisiyse talep otomatik `APPROVED` olur. Geçmiş notu: "Onaylayıcı ayrıldı; otomatik onaylandı."
     - Diğer durumda geçmiş notu: "Onaylayıcı değişti: {s}."
  2. `u`'nun onaylayıcı veya vekil olduğu, bitişi bugün ya da sonrası olan vekâletler silinir.
  3. Yeni onaylayıcıya her talep için `PRINT_APPROVAL_REQUESTED` bildirimi gönderilir.

**`PrintMembershipRemovalHook`:** Kişinin onayını bekleyen talep varsa yıldan çıkarılması engellenir. Mesaj: "Onayını bekleyen {n} fotokopi talebi var."

### 2.9 Bildirimler ve canlı olaylar

| Olay | Kime | type | title | body | link |
|---|---|---|---|---|---|
| Talep onaya gönderildi | `approver_id` | `PRINT_APPROVAL_REQUESTED` | "Onay bekleyen fotokopi talebi" | "{sahip}: {başlık*} – {sayfa} sayfa × {kopya} kopya" | `/fotokopi/{id}` |
| Talep kuyruğa düştü (onaylandı ya da otomatik) | Şubenin fotokopi sorumluları | `PRINT_QUEUE_NEW` | "Yeni fotokopi talebi" | "{sahip} · Teslim: {teslimZamanı}" | `/fotokopi/{id}` |
| Onaylandı (elle) | Talep sahibi | `PRINT_APPROVED` | "Fotokopi talebiniz onaylandı" | "{başlık}" | `/fotokopi/{id}` |
| Reddedildi | Talep sahibi | `PRINT_REJECTED` | "Fotokopi talebiniz reddedildi" | "Gerekçe: {gerekçe}" | `/fotokopi/{id}` |
| Hazır | Talep sahibi | `PRINT_READY` | "Fotokopileriniz hazır" | "{başlık} teslim alınmaya hazır ({şube})." | `/fotokopi/{id}` |
| İptal (APPROVED durumundan) | Şubenin fotokopi sorumluları | `PRINT_CANCELLED` | "Fotokopi talebi iptal edildi" | "{sahip}: {başlık*}" | `/fotokopi/{id}` |

Notlar:

- \* işaretli `başlık`, talep gizliyse "Gizli evrak" olarak yazılır.
- **Teslim zamanı biçimi:** `"18 Kas Çar 10:00"`, Europe/Istanbul saatiyle.
- **WebSocket olayı `PRINT_REQUEST_CHANGED`:** Her geçişte talep sahibine, `approver_id`'ye ve şubenin fotokopi sorumlularına `{ requestId, status }` payload'ıyla gönderilir. Aynı kişilere `BADGES` olayı da gider.
- **Rozetler (`PrintBadgeProvider`):**
  - `printApprovals`: `approver_id = me` olan `PENDING_APPROVAL` talep sayısı
  - `printQueue`: Fotokopi sorumlusuysa, kendi şubesinde ve aktif yılda `APPROVED` veya `IN_PROGRESS` olan talep sayısı

---

## §3 Veritabanı — `V3__print.sql`

```sql
CREATE TABLE print_request (
  id                   bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  season_id            bigint       NOT NULL REFERENCES season(id),
  requester_id         bigint       NOT NULL REFERENCES app_user(id),
  branch_id            bigint       NOT NULL REFERENCES branch(id),
  title                varchar(200) NOT NULL,
  file_id              uuid         REFERENCES stored_file(id),
  original_on_paper    boolean      NOT NULL DEFAULT false,
  page_count           integer      NOT NULL CHECK (page_count BETWEEN 1 AND 2000),
  copies               integer      NOT NULL CHECK (copies BETWEEN 1 AND 1000),
  color                varchar(5)   NOT NULL CHECK (color IN ('BW','COLOR')),
  sides                varchar(6)   NOT NULL CHECK (sides IN ('SINGLE','DOUBLE')),
  paper_size           varchar(2)   NOT NULL CHECK (paper_size IN ('A4','A3')),
  secret               boolean      NOT NULL DEFAULT false,
  due_at               timestamptz  NOT NULL,
  note                 varchar(500),
  total_pages          integer      NOT NULL CHECK (total_pages BETWEEN 1 AND 20000),
  status               varchar(20)  NOT NULL CHECK (status IN ('PENDING_APPROVAL','APPROVED','REJECTED','IN_PROGRESS','READY','DELIVERED','CANCELLED')),
  approver_id          bigint       REFERENCES app_user(id),
  original_approver_id bigint       REFERENCES app_user(id),
  decided_by           bigint       REFERENCES app_user(id),
  decided_at           timestamptz,
  reject_reason        varchar(500),
  operator_id          bigint       REFERENCES app_user(id),
  started_at           timestamptz,
  ready_at             timestamptz,
  delivered_at         timestamptz,
  cancelled_by         bigint       REFERENCES app_user(id),
  cancelled_at         timestamptz,
  cancel_reason        varchar(200),
  file_purged_at       timestamptz,
  copied_from_id       bigint       REFERENCES print_request(id),
  version              integer      NOT NULL DEFAULT 0,
  created_at           timestamptz  NOT NULL DEFAULT now(),
  updated_at           timestamptz  NOT NULL DEFAULT now(),
  CONSTRAINT ck_print_file_or_paper CHECK (file_id IS NOT NULL OR original_on_paper)
);
CREATE INDEX ix_print_requester ON print_request (requester_id, id DESC);
CREATE INDEX ix_print_approver  ON print_request (approver_id, status);
CREATE INDEX ix_print_decided   ON print_request (decided_by, decided_at DESC);
CREATE INDEX ix_print_queue     ON print_request (season_id, branch_id, status, due_at);
CREATE INDEX ix_print_purge     ON print_request (status) WHERE file_id IS NOT NULL AND file_purged_at IS NULL;
CREATE INDEX ix_print_delivered ON print_request (delivered_at) WHERE status = 'DELIVERED';

CREATE TABLE print_request_history (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  request_id  bigint      NOT NULL REFERENCES print_request(id),
  from_status varchar(20),
  to_status   varchar(20) NOT NULL,
  actor_id    bigint      REFERENCES app_user(id),
  note        varchar(500),
  created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_print_history_req ON print_request_history (request_id, id);

CREATE TABLE print_delegation (
  id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  approver_id  bigint      NOT NULL REFERENCES app_user(id),
  delegate_id  bigint      NOT NULL REFERENCES app_user(id),
  start_date   date        NOT NULL,
  end_date     date        NOT NULL,
  created_at   timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ck_delegation_dates CHECK (end_date >= start_date),
  CONSTRAINT ck_delegation_self  CHECK (approver_id <> delegate_id)
);
CREATE INDEX ix_delegation_approver ON print_delegation (approver_id, end_date);
```

`application.yml` dosyasına eklenecek ayarlar:

```yaml
app.print: { file-retention-days: 30, min-lead-minutes: 60, max-days-ahead: 60 }
```

---

## §4 Sınıflar (`print/`)

| Sınıf | Görev |
|---|---|
| `PrintRequest` (`@Version`), `PrintRequestHistory`, `PrintDelegation` | Entity'ler. Enum'lar: `PrintStatus`, `PrintColor { BW, COLOR }`, `PrintSides { SINGLE, DOUBLE }`, `PaperSize { A4, A3 }`. |
| `PrintRequestService` | Oluşturma (onaylayıcı belirleme dahil) ve tüm durum geçişleri. Her geçiş tek bir özel metottan geçer: `transition(req, to, actor, note)`. Bu metot geçmiş kaydını yazar ve bildirim ile WS olaylarını gönderir. |
| `PrintAccess` | Görünürlük ve yetki kontrolleri (§2.3). Aksiyon bayraklarını (`canX`) hesaplar. |
| `PrintQueries` | Listeler, pano, rapor (JdbcClient) |
| `PrintDelegationService` | Vekâlet işlemleri |
| `PrintPageCounter` | PDFBox: `Loader.loadPDF(file).getNumberOfPages()`. Herhangi bir hatada `null` döner. |
| `PrintSeasonHook`, `PrintDeactivationHook`, `PrintMembershipRemovalHook`, `PrintBadgeProvider` | Çekirdek hook'larının uygulamaları |
| `PrintJobs` | 04:00 dosya temizleme işi (§2.6) |
| `PrintController`, `PrintDtos`, `PrintErrorCode`, `PrintAuditAction` | |
| `PrintDevSeeder` | Yalnızca `dev` profilinde ve tablo boşsa (§9) |

---

## §5 API

### 5.1 DTO'lar

```ts
type PrintStatus = 'PENDING_APPROVAL'|'APPROVED'|'REJECTED'|'IN_PROGRESS'|'READY'|'DELIVERED'|'CANCELLED';
type PersonRef = { id: number; fullName: string; branchName?: string };
type PrintRequestRow = {
  id: number; seasonId: number; archived: boolean; title: string;        // gizliyse ve ben sahibi değilsem "Gizli evrak"
  secret: boolean; status: PrintStatus; requester: PersonRef; branchName: string;
  pageCount: number; copies: number; totalPages: number; color: 'BW'|'COLOR'; sides: 'SINGLE'|'DOUBLE'; paperSize: 'A4'|'A3';
  originalOnPaper: boolean; dueAt: string; createdAt: string; approver?: PersonRef;
};
type PrintHistoryItem = { fromStatus?: PrintStatus; toStatus: PrintStatus; actor?: PersonRef; note?: string; createdAt: string };
type PrintRequestDetail = PrintRequestRow & {
  note?: string; rejectReason?: string; cancelReason?: string; originalApprover?: PersonRef;
  decidedBy?: PersonRef; decidedAt?: string; operator?: PersonRef; startedAt?: string; readyAt?: string; deliveredAt?: string; cancelledAt?: string;
  file?: { name: string; contentType: string; sizeBytes: number; purged: boolean };
  history: PrintHistoryItem[];
  actions: { canCancel: boolean; canCopy: boolean; canApprove: boolean; canReject: boolean;
             canStart: boolean; canMarkReady: boolean; canDeliver: boolean; canDownload: boolean };
};
type CreatePrintRequest = {
  title: string; fileId?: string; originalOnPaper: boolean; pageCount: number; copies: number;
  color: 'BW'|'COLOR'; sides: 'SINGLE'|'DOUBLE'; paperSize: 'A4'|'A3'; secret: boolean;
  dueAt: string;            // ISO, ofsetli
  note?: string; copiedFromId?: number;
};
type PrintContext = {
  isApprover: boolean;      // aktif yılda en az bir kişinin onaylayıcısı veya approver_id=me açık talebi var
  isOperator: boolean; isManager: boolean; branchName: string;
  myApprover?: PersonRef;   // talebim kime gidecek (vekâlet uygulanmış hali)
  myApproverOnLeave?: { approver: PersonRef; delegate: PersonRef; endDate: string };
  minLeadMinutes: number; maxDaysAhead: number;
};
type DelegationDto = { id: number; delegate: PersonRef; startDate: string; endDate: string; active: boolean };
type QueueBoard = { approved: PrintRequestRow[]; inProgress: PrintRequestRow[]; ready: PrintRequestRow[] };
type MonthlyReport = { month: string; rows: ReportRow[]; totals: Omit<ReportRow,'userId'|'fullName'|'branchName'> };
type ReportRow = { userId: number; fullName: string; branchName: string; requestCount: number; bwPages: number; colorPages: number; totalPages: number };
```

`actions` bayrakları sunucuda §2.1 ve §2.3'e göre hesaplanır. Arşivlenmiş taleplerde `canCopy` dışındaki tüm bayraklar `false` olur. `canCopy`, talep sahibi için `REJECTED`, `CANCELLED` ve `DELIVERED` durumlarında `true` olur.

### 5.2 Uç noktalar

Tüm uçlarda `me.requireMember()` çağrılır.

**Bağlam ve okuma**

| Uç nokta | Yanıt | Kurallar |
|---|---|---|
| `GET /api/print/context` | `PrintContext` | |
| `GET /api/print/seasons` | `{ id, name, active }[]` | Kullanıcının atandığı yıllar, `name DESC`. Sorgu, Akış'taki `feed/seasons` ile aynıdır; modüller birbirini çağırmadığı için kopyalanır. |
| `GET /api/print/requests/mine?seasonId=&group=open\|ready\|delivered\|closed&page=&size=20` | `PageResponse<PrintRequestRow>` | `seasonId` varsayılan olarak aktif yıldır. Gruplar: `open` = PENDING_APPROVAL, APPROVED, IN_PROGRESS; `ready` = READY; `delivered` = DELIVERED; `closed` = REJECTED, CANCELLED; parametre verilmezse tümü. Sıralama `id DESC`. |
| `GET /api/print/approvals?tab=pending\|decided&page=&size=20` | `PageResponse<PrintRequestRow>` | `pending`: `approver_id = me AND status = PENDING_APPROVAL`, `due_at ASC`. `decided`: `decided_by = me`, aktif yıl, `decided_at DESC`. |
| `GET /api/print/queue/board` | `QueueBoard` | Fotokopi sorumlusu değilse `PRINT_OPERATOR_ONLY`. Kendi şubesi ve aktif yıl; her sütun `due_at ASC`; sayfalama yok. |
| `GET /api/print/queue/delivered?date=&page=&size=20` | `PageResponse<PrintRequestRow>` | `PRINT_OPERATOR_ONLY`. `date` (LocalDate) verilmişse o günün teslimleri; `delivered_at DESC`. |
| `GET /api/print/requests/{id}` | `PrintRequestDetail` | Görünürlük §2.3; görünmüyorsa `PRINT_REQUEST_NOT_FOUND` |
| `GET /api/print/requests/{id}/file?download=false` | dosya akışı | `canDownload` yoksa `PRINT_REQUEST_NOT_FOUND`. Dosya yoksa `PRINT_NO_FILE`. Dosya silinmişse `PRINT_FILE_PURGED`. Gizli talep ve talep sahibi değilse denetim kaydı (§2.4). |
| `GET /api/print/files/{fileId}/page-count` | `{ pageCount: number \| null }` | Dosya bana ait, bağlanmamış ve `purpose=PRINT` olmalı; değilse `FILE_NOT_USABLE`. PDF değilse veya okunamazsa `null` döner. |

**Talep işlemleri**

| Uç nokta | İstek | Yanıt | Kurallar |
|---|---|---|---|
| `POST /api/print/requests` | `CreatePrintRequest` | 201 `PrintRequestDetail` | §5.3 doğrulamaları ve §2.2. Dosya `attach(fileId, me, PRINT)` ile bağlanır. `copiedFromId` verilmişse kendi talebim olmalı; değilse `PRINT_REQUEST_NOT_FOUND`. |
| `POST /api/print/requests/{id}/cancel` | — | `PrintRequestDetail` | Talep sahibi değilse veya durum uygun değilse `PRINT_CANCEL_NOT_ALLOWED`. `cancel_reason = 'Talep sahibi iptal etti'`. |
| `POST /api/print/requests/{id}/approve` | — | `PrintRequestDetail` | `approver_id ≠ me` ise `PRINT_APPROVE_FORBIDDEN`. Durum uygun değilse `PRINT_INVALID_STATUS`. |
| `POST /api/print/requests/{id}/reject` | `{ reason }` | `PrintRequestDetail` | Aynı kontroller. `reason` 5–500 karakter: "Red gerekçesi 5–500 karakter olmalıdır." |
| `POST /api/print/requests/{id}/start` | — | `PrintRequestDetail` | Şubenin fotokopi sorumlusu değilse `PRINT_OPERATOR_ONLY`. Ayrıca `PRINT_INVALID_STATUS`. `operator_id = me`. |
| `POST /api/print/requests/{id}/ready` | — | `PrintRequestDetail` | Aynı kontroller |
| `POST /api/print/requests/{id}/deliver` | — | `PrintRequestDetail` | Aynı kontroller |

**Vekâlet**

| Uç nokta | İstek | Yanıt | Kurallar |
|---|---|---|---|
| `GET /api/print/delegations` | — | `DelegationDto[]` | Kendi vekâletlerim; `end_date >= bugün`; `start_date ASC` |
| `POST /api/print/delegations` | `{ delegateId, startDate, endDate }` | 201 `DelegationDto` | §5.3 doğrulamaları. Denetim: `PRINT_DELEGATION_CREATED`. |
| `DELETE /api/print/delegations/{id}` | — | 204 | Bana ait değilse `PRINT_DELEGATION_NOT_FOUND`. Denetim: `PRINT_DELEGATION_DELETED`. |

**Rapor**

| Uç nokta | Yanıt | Kurallar |
|---|---|---|
| `GET /api/print/reports/monthly?month=YYYY-MM&branchId=` | `MonthlyReport` | Yönetici değilse `PRINT_REPORT_FORBIDDEN`. Ayrıntılar aşağıda. |
| `GET /api/print/reports/monthly.csv?month=&branchId=` | `text/csv` | Ayrıntılar aşağıda. |

Aylık rapor kuralları:

- Rapora yalnızca `DELIVERED` talepler girer. Talebin hangi aya ait olduğu `delivered_at` değerinin Europe/Istanbul saatindeki ayına göre belirlenir. Yıl filtresi yoktur.
- Satırlar `requester_id` bazında gruplanır; `branchName` talebin şubesidir. Sıralama `totalPages DESC`.
- Ay biçimi `^\d{4}-(0[1-9]|1[0-2])$` olmalı; değilse `PRINT_REPORT_MONTH_INVALID`.

CSV dosyası:

- Kodlama UTF-8 BOM, ayraç `;`.
- Dosya adı: `fotokopi-raporu-{YYYY-MM}.csv`.
- Başlık satırı: `Ad Soyad;Şube;Talep Sayısı;Siyah-Beyaz Sayfa;Renkli Sayfa;Toplam Sayfa`.
- Son satır toplamlardır; ilk sütununa "TOPLAM" yazılır.

### 5.3 Doğrulama tablosu

Frontend ve backend aynı kuralları uygular:

| Alan | Kural | Mesaj |
|---|---|---|
| title | Trim edilir, 3–200 karakter | "Açıklama 3–200 karakter olmalıdır." |
| fileId / originalOnPaper | `originalOnPaper = false` ise `fileId` zorunlu. İkisi birlikte verilirse `fileId` yok sayılır ve dosya bağlanmaz. | PRINT_FILE_REQUIRED |
| pageCount | Tam sayı, 1–2000 | "Sayfa sayısı 1 ile 2000 arasında olmalıdır." |
| copies | Tam sayı, 1–1000 | "Kopya adedi 1 ile 1000 arasında olmalıdır." |
| pageCount × copies | En fazla 20.000 | PRINT_TOTAL_TOO_LARGE |
| color / sides / paperSize | Zorunlu enum | "Bir seçenek seçin." |
| dueAt | Zorunlu; `now + minLead` ile `now + maxDays` arasında | Aralığın altındaysa PRINT_DUE_TOO_SOON, üstündeyse PRINT_DUE_TOO_LATE |
| note | Trim edilir, en fazla 500 karakter | "Not en fazla 500 karakter olabilir." |
| delegateId | Zorunlu; aktif, yıla atanmış ve ben olmayan bir kişi | PRINT_DELEGATE_INVALID |
| startDate / endDate | `startDate >= bugün`, `endDate >= startDate`, en fazla 90 gün; mevcut vekâletlerimle çakışmamalı | PRINT_DELEGATION_DATES_INVALID, PRINT_DELEGATION_OVERLAP |

---

## §6 Hata kodları (`PrintErrorCode`)

| code | HTTP | message |
|---|---|---|
| PRINT_REQUEST_NOT_FOUND | 404 | Fotokopi talebi bulunamadı veya görüntüleme yetkiniz yok. |
| PRINT_FILE_REQUIRED | 400 | Bir dosya yükleyin veya "Basılı orijinali getireceğim" seçeneğini işaretleyin. |
| PRINT_TOTAL_TOO_LARGE | 400 | Toplam baskı (sayfa × kopya) en fazla 20.000 olabilir. |
| PRINT_DUE_TOO_SOON | 400 | Teslim zamanı en erken {0} dakika sonrası olabilir. Lütfen daha ileri bir saat seçin. |
| PRINT_DUE_TOO_LATE | 400 | Teslim zamanı en fazla {0} gün sonrası olabilir. |
| PRINT_INVALID_STATUS | 409 | Bu işlem yapılamıyor; talebin şu anki durumu: {0}. Sayfayı yenileyip tekrar deneyin. |
| PRINT_CANCEL_NOT_ALLOWED | 409 | Talep yalnızca hazırlanmaya başlanmadan önce, talep sahibi tarafından iptal edilebilir. |
| PRINT_APPROVE_FORBIDDEN | 403 | Bu talebi yalnızca atanan onaylayıcı onaylayabilir veya reddedebilir. |
| PRINT_OPERATOR_ONLY | 403 | Bu işlem yalnızca ilgili şubenin fotokopi sorumlusu tarafından yapılabilir. |
| PRINT_NO_FILE | 404 | Bu talepte dosya yok; basılı orijinal fotokopi birimine getirilecek. |
| PRINT_FILE_PURGED | 410 | Dosya saklama süresi dolduğu için silindi. |
| PRINT_REPORT_FORBIDDEN | 403 | Fotokopi raporunu yalnızca yöneticiler görüntüleyebilir. |
| PRINT_REPORT_MONTH_INVALID | 400 | Geçerli bir ay seçin. |
| PRINT_DELEGATE_INVALID | 400 | Vekil, bu yıla atanmış aktif bir personel olmalı ve siz olmamalısınız. |
| PRINT_DELEGATION_DATES_INVALID | 400 | Başlangıç bugün veya sonrası, bitiş başlangıçtan sonra olmalı; vekâlet en fazla 90 gün sürebilir. |
| PRINT_DELEGATION_OVERLAP | 409 | Bu tarihlerle çakışan bir vekâletiniz var. Önce onu silin. |
| PRINT_DELEGATION_NOT_FOUND | 404 | Vekâlet kaydı bulunamadı. |

**Denetim işlemleri (`PrintAuditAction`):**

| Kod | Etiket |
|---|---|
| PRINT_SECRET_FILE_ACCESSED | Gizli fotokopi dosyası açıldı |
| PRINT_DELEGATION_CREATED | Fotokopi vekili tanımlandı |
| PRINT_DELEGATION_DELETED | Fotokopi vekâleti silindi |

Durum geçişleri denetim kaydına yazılmaz; `print_request_history` tablosu bu işi görür.

---

## §7 Ekranlar

### 7.1 Ana sayfa (`/fotokopi`)

**Başlık:**

- Başlık "Fotokopi".
- Birincil aksiyon: "Yeni Talep". Mobilde FAB olarak gösterilir.

**Sekmeler:** Seçili sekme `?sekme=` ile URL'de tutulur. Sekmeler yalnızca yetkili olunanlarda görünür.

| Sekme | Görünme koşulu | Rozet |
|---|---|---|
| Taleplerim | Herkes | — |
| Onay Bekleyenler | `context.isApprover` | `printApprovals` |
| Fotokopi Kuyruğu | `context.isOperator` | `printQueue` |
| Rapor | `context.isManager` | — |

Varsayılan sekme: fotokopi sorumlusuysa **Kuyruk**; değilse ve bekleyen onayı varsa **Onay Bekleyenler**; diğer durumlarda **Taleplerim**.

**Canlı güncelleme:** `PRINT_REQUEST_CHANGED` olayı gelince `['print', …]` sorguları yenilenir.

### 7.2 Talep kartı (`PrintRequestCard`)

Tüm listelerde aynı kart kullanılır:

| Satır | İçerik |
|---|---|
| 1 | Açıklama (semibold, en fazla 2 satır). Gizliyse önünde 🔒 (Lock ikonu). Sağda `StatusBadge`. |
| 2 (muted) | "{sayfa} sayfa × {kopya} kopya · {Renkli \| Siyah-Beyaz} · {A4 \| A3} · {Tek yüz \| Çift yüz}". Basılı orijinalse sonuna "· Basılı orijinal" eklenir. |
| 3 | Takvim ikonu ve "Teslim: 18 Kas Çar 10:00" |

**Teslim satırının rengi:**

- Kırmızı, ön ek "Gecikti · ": Talep açıksa ve teslim zamanı geçtiyse.
- Amber, ön ek "Bugün · ": Teslim zamanı bugünse.

**Bağlama göre ek satır:**

- Taleplerim: "Onaylayıcı: {ad}" ya da "Onaysız"
- Onay ve Kuyruk listeleri: avatar, talep sahibi ve şubesi

**Tıklama:** Kartın tamamı tıklanabilir ve `/fotokopi/{id}` sayfasını açar. Aksiyon butonları tıklamanın karta yayılmasını durdurur (`stopPropagation`).

### 7.3 Taleplerim sekmesi

- **Grup çipleri:** `Tümü` · `Süren` · `Hazır` · `Teslim Edildi` · `Reddedilen / İptal`.
- **Yıl seçimi:** ⋯ menüsünde geçmiş yıllar bulunur (`GET /api/print/seasons`). Geçmiş bir yıl seçilince `ArchiveBanner` gösterilir.
- **Kart aksiyonları:**
  - "İptal Et" (`canCancel` ise). Onay penceresi: "Talep iptal edilecek. Bu işlem geri alınamaz." Toast: "Talep iptal edildi."
  - "Kopyala" (`canCopy` ise). `/fotokopi/yeni?kopya={id}` sayfasını açar.
- **Boş durum:** "Henüz fotokopi talebiniz yok." Buton: "Yeni Talep".

### 7.4 Yeni talep (`/fotokopi/yeni`)

Ayrı bir sayfadır; uzun bir form olduğu için pencere kullanılmaz. Masaüstünde en fazla 720px genişliğinde tek bir kart, mobilde tam ekrandır.

**Üst bilgi kutusu** (mavi `info-bg`), `context` değerine göre:

| Durum | Metin |
|---|---|
| Onaylayıcı var | "Talebiniz onay için **{myApprover}** kişisine gönderilecek." |
| Onaylayıcı izinde | "Onaylayıcınız **{approver}** {endDate} tarihine kadar izinde; talebiniz vekili **{delegate}** kişisine gidecek." |
| Onaylayıcı yok | "Talebiniz doğrudan **{branchName}** fotokopi birimine iletilecek." |

**Bölüm 1 — Dosya**

- Tek dosyalık `FileUploader` (`purpose=PRINT`). İzin verilen uzantılar `00-mimari.md` §9'daki gibidir.
- Dosya PDF ise yükleme bitince `page-count` çağrılır:
  - Değer gelirse "Sayfa sayısı" alanı doldurulur ve altında not gösterilir: "Sayfa sayısı dosyadan okundu. Gerekirse değiştirin."
  - `null` gelirse uyarı toast'ı: "PDF'in sayfa sayısı okunamadı; lütfen elle girin."
- ☐ "Basılı orijinali fotokopi birimine getireceğim". İşaretlenince dosya alanı gizlenir ve yüklenmiş dosya formdan çıkarılır.

**Bölüm 2 — Baskı bilgileri**

- **Açıklama\***. Placeholder: "Ör. 9-A Matematik 2. ünite çalışma kâğıdı".
- **Sayfa sayısı\*** ve **Kopya adedi\***: yan yana sayı alanları. Her birinde − ve + butonları (44px) ve `inputMode="numeric"` bulunur.
- **Renk:** Segmentli seçim. Seçenekler "Siyah-Beyaz" (varsayılan) ve "Renkli".
- **Baskı yüzü:** Segmentli seçim. Seçenekler "Tek yüz" (varsayılan) ve "Çift yüz".
- **Kâğıt:** Segmentli seçim. Seçenekler "A4" (varsayılan) ve "A3".
- **Canlı özet kutusu** (gri): "Toplam **360 baskı** · 180 yaprak".
  - Toplam 20.000'i aşarsa kutu kırmızı olur ve PRINT_TOTAL_TOO_LARGE mesajı gösterilir.

**Bölüm 3 — Teslim**

- **İstenen teslim tarihi ve saati\*** (`datetime-local`).
  - `min` değeri `now + minLead` olur.
  - Varsayılan değer yarın 08:00'dir. Bu değer `min` değerinden önceyse `min` değeri yuvarlanarak kullanılır.
  - Yardım metni: "En erken {minLead} dakika sonrası seçilebilir."
  - Seçilen değer, istemcinin saat dilimiyle ISO'ya çevrilir (`new Date(v).toISOString()`).
- **Not.** Placeholder: "Ör. Yalnızca 2–5. sayfalar, zımbalı olsun."

**Bölüm 4 — Gizlilik**

- ☐ "Gizli evrak (sınav vb.)".
- Yardım metni: "Dosyayı yalnızca siz, onaylayıcınız ve fotokopi sorumlusu görebilir; listelerde açıklama 'Gizli evrak' olarak görünür."

**Gönderim:**

- Buton: "Talebi Gönder" (tam genişlik). Dosya yüklenirken pasiftir.
- Başarı toast'ı:
  - Onay bekliyorsa: "Talebiniz {approverAdı} onayına gönderildi."
  - Otomatik onaylandıysa: "Talebiniz fotokopi birimine iletildi."
- Başarıdan sonra `/fotokopi/{id}` sayfası açılır.

**Kopyalama (`?kopya={id}`):**

- Kaynak talebin detayı çekilir. Dosya dışındaki tüm alanlar forma doldurulur; teslim zamanı varsayılan değere döner.
- Sarı bilgi kutusu gösterilir: "Önceki talepten kopyalandı. {Önceki dosya: ad.pdf} — dosyayı yeniden yükleyin."
- Kaynak talep reddedildiyse red gerekçesi de bu kutuda gösterilir.

**Doğrulama:** zod şeması §5.3 ile aynıdır.

### 7.5 Talep detayı (`/fotokopi/[id]`)

**Başlık:** "← Fotokopi" geri bağlantısı, açıklama ve `StatusBadge`.

**Adım çubuğu (stepper):** Gönderildi → Onaylandı → Hazırlanıyor → Hazır → Teslim Edildi.

- Geçilen adımlar yeşil onay işaretiyle, mevcut adım `primary` renkle gösterilir.
- Onaysız taleplerde "Onaylandı" adımının altında "Otomatik" yazar.
- `REJECTED` ve `CANCELLED` durumlarında adım çubuğu yerine kırmızı ya da gri bir kutu gösterilir: durum etiketi, gerekçe, kim ve ne zaman.

**Bilgi tablosu** (2 sütun, mobilde 1 sütun):

- Talep sahibi, şube
- Sayfa / kopya / toplam baskı
- Renk, yüz, kâğıt
- Teslim zamanı
- Onaylayıcı; vekil üzerinden geldiyse "(vekil — asıl: {ad})"
- Fotokopi sorumlusu
- Not
- Gizli

**Dosya kartı:**

- Dosya adı ve boyutu.
- Butonlar:
  - "Önizle": yalnızca pdf ve görsel için; yeni sekmede `inline` açar.
  - "İndir"
- Dosya silinmişse gri metin: "Dosya saklama süresi dolduğu için silindi."
- Basılı orijinalse: "Basılı orijinal fotokopi birimine getirilecek."

**Geçmiş:** Dikey zaman çizelgesi. Her öğede `DateText`, kişi ve "{önceki} → {yeni}" geçişi, varsa not.

**Aksiyon çubuğu:** Mobilde altta sabit durur. Yalnızca `actions` bayrağı açık olan butonlar görünür:

| Buton | Pencere ve metinler |
|---|---|
| Onayla (birincil, yeşil) | `ConfirmDialog`: "Talep onaylanacak ve fotokopi birimine iletilecek." Toast: "Talep onaylandı." |
| Reddet (destructive outline) | Pencerede "Red gerekçesi\*" textarea'sı. Yardım metni: "Talep sahibi bu gerekçeyi görecek." Buton: "Reddet". Toast: "Talep reddedildi." |
| Hazırlamaya Başla | Onay penceresi yok. Toast: "Talep hazırlanıyor olarak işaretlendi." |
| Hazır | Onay penceresi yok. Toast: "Talep hazır olarak işaretlendi; talep sahibine bildirim gönderildi." |
| Teslim Edildi | `ConfirmDialog`: "{sahip} kişisine teslim edildi olarak işaretlensin mi?" Toast: "Teslim edildi." |
| İptal Et | §7.3'teki gibi |
| Kopyala ve Yeniden Gönder | §7.4'teki kopyalama akışı |

### 7.6 Onay Bekleyenler sekmesi

**Üstte vekâlet kartı:**

- Aktif ya da gelecekteki vekâlet varsa listelenir: "{vekil} · 18–25 Kas" ve yanında "Sil" (onay penceresiyle).
- Vekâlet yoksa: "İzinde olacağınız günler için bir vekil tanımlayabilirsiniz." ve "Vekil Tanımla" butonu.
- Vekil tanımlama penceresi:
  - Alanlar: Vekil\* (`UserPicker`, directory), Başlangıç\*, Bitiş\* (`type=date`)
  - Toast: "Vekil tanımlandı. {başlangıç}–{bitiş} arasında gelen talepler {ad} kişisine gidecek."

**Çipler:** `Bekleyenler` · `Karar Verdiklerim`.

**Bekleyen kartlarında** kart altında iki buton bulunur: "Onayla" ve "Reddet". Pencereleri §7.5 ile aynıdır.

**Boş durum:** "Onayınızı bekleyen talep yok."

### 7.7 Fotokopi Kuyruğu sekmesi (fotokopi sorumlusu)

**Masaüstü (≥1024px):** Üç sütunlu pano.

| Sütun | İçerik | Kart butonu |
|---|---|---|
| "Yeni" | `approved` | "Başla" ve ikincil "Hazır" |
| "Hazırlanıyor" | `inProgress` | "Hazır" |
| "Teslim Bekliyor" | `ready` | "Teslim Et" |

- Sütun başlığında sayı gösterilir.
- Kartlarda büyük puntoyla toplam baskı yazılır ("360 baskı"); altında renk, kâğıt ve yüz bilgisi rozet olarak bulunur.
- Kartta dosya varsa "İndir" ikon butonu bulunur.

**Mobil:** Üç sütun yerine segmentli sekmeler ("Yeni {n}" · "Hazırlanıyor {n}" · "Teslim Bekliyor {n}"). Kartlar tek sütunda listelenir.

**Pano altı:** "Teslim edilenler" bağlantısı bir pencere açar. Pencere tarih seçicili ve sayfalı listeyi gösterir; varsayılan tarih bugündür.

**Boş sütun metni:** "Bu sütunda talep yok."

**Canlı güncelleme:** `PRINT_REQUEST_CHANGED` olayıyla pano yenilenir.

### 7.8 Rapor sekmesi (Yönetici)

**Filtreler:**

- Ay (`type=month`); varsayılan bu ay
- Şube (Tümü / şubeler)
- "CSV İndir" butonu (`outline`)

**Tablo:**

- Sütunlar: Ad Soyad · Şube · Talep · Siyah-Beyaz · Renkli · Toplam.
- Son satır kalın yazılmış toplamlardır.
- Sayılar Türkçe binlik ayraçla gösterilir: `toLocaleString('tr-TR')`.

**Boş durum:** "Bu ay teslim edilmiş talep yok."

**Not satırı:** "Rapor, seçilen ayda teslim edilen talepleri içerir. Baskı = sayfa × kopya."

---

## §8 Dosya temizleme işi (`PrintJobs`)

- Zamanlama: `@Scheduled(cron = "0 0 4 * * *", zone = "Europe/Istanbul")`.
- Seçilen talepler: §2.6'daki koşulları sağlayanlar, en fazla 500 kayıt. Her talep kendi transaction'ında işlenir.
- Her talepte: `FileStorageService.delete(file)` çağrılır ve `file_purged_at = now` yazılır.
- Bir dosyada hata olursa `log.warn` yazılır ve sonrakine geçilir.

---

## §9 Geliştirme verisi (`PrintDevSeeder`)

Dev kullanıcılarıyla, aktif yılda şu kayıtlar oluşturulur:

| Talep sahibi | Durum | Özellik |
|---|---|---|
| ogretmen1 | PENDING_APPROVAL | Onaylayıcı zumre.mat; PDF dosyalı (`dev/ornek.pdf`) |
| ogretmen1 | READY | Merkez kuyruğu |
| ogretmen3 | APPROVED (otomatik) | Onaylayıcısı yok; basılı orijinal |
| ogretmen2 | IN_PROGRESS | Kuzey kuyruğu; renkli A3 |
| zumre.mat | PENDING_APPROVAL | Gizli; onaylayıcı mudur |
| ogretmen2 | REJECTED | Gerekçe: "Lütfen sayfa sayısını kontrol edin." |
| ogretmen1 | DELIVERED | Rapor için; bu ay teslim edilmiş |
| ogretmen1 | DELIVERED | Rapor için; geçen ay teslim edilmiş |

Her kayıt için uygun geçmiş satırları yazılır.

---

## §10 Zorunlu testler

| No | Senaryo |
|---|---|
| T-P1 | **Onaylayıcı belirleme:**<br>(a) Atamada onaylayıcı tanımlıysa talep `PENDING_APPROVAL` olur ve `approver_id` o kişidir.<br>(b) Onaylayıcı yoksa talep `APPROVED` olur ve `approver_id` null'dır.<br>(c) Bugünü kapsayan bir vekâlet varsa `approver_id` vekil, `original_approver_id` asıl onaylayıcı olur.<br>(d) Vekil talep sahibinin kendisiyse asıl onaylayıcı kalır. |
| T-P2 | **Durum makinesi:**<br>(a) Tam akış (onayla → başla → hazır → teslim) başarılı olur ve geçmişte 5 satır oluşur.<br>(b) `APPROVED` durumundan `deliver` çağrılırsa `PRINT_INVALID_STATUS` döner.<br>(c) Atanmamış biri onaylamaya çalışırsa `PRINT_APPROVE_FORBIDDEN` döner.<br>(d) `IN_PROGRESS` durumundaki talep iptal edilmek istenirse `PRINT_CANCEL_NOT_ALLOWED` döner.<br>(e) Gerekçesiz red `VALIDATION_FAILED` döner. |
| T-P3 | **Görünürlük:**<br>(a) Başka şubenin fotokopi sorumlusu talebin detayını isterse `PRINT_REQUEST_NOT_FOUND` alır.<br>(b) Fotokopi sorumlusu `PENDING_APPROVAL` durumundaki talebi göremez. |
| T-P4 | **Hook'lar:**<br>(a) Yıl aktifleştirilince eski yılın açık talepleri `CANCELLED` olur.<br>(b) Onaylayıcı devralanla pasifleştirilince bekleyen talepleri devralana geçer. |
| T-P5 | **Temizleme:** Saklama süresi dolmuş `DELIVERED` talebin dosyası silinir ve `file_purged_at` dolar; süresi dolmamış talebe dokunulmaz. |
