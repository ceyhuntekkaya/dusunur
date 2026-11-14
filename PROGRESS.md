# İlerleme

## İşaretler

| İşaret | Anlamı |
|---|---|
| `[ ]` | Bekliyor |
| `[x]` | Bitti |
| `⛔` | Kullanıcı kararı bekliyor; yanına soru kısaca yazılır |
| `[P]` | Bu dalgadaki Backend ve Frontend listeleri paralel ajanlarla yürür |

## Kurallar

- `→` işareti, görevde okunacak plan bölümünü gösterir. Yalnızca orası okunur.
- Görev ID biçimi `{Faz}-{B|F|X}{no}`:
  - `B`: backend
  - `F`: frontend
  - `X`: entegrasyon veya ortak iş
- Bir görev bitince `[x]` yapılır ve commit atılır: `[ID] açıklama`.

## Faz özeti

| Faz | Kapsam | Plan | Durum |
|---|---|---|---|
| 0 | Kurulum | 00-mimari | ✅ |
| 1 | Çekirdek | 01-cekirdek | ✅ |
| 2 | Akış | 02-akis | ✅ |
| 3 | Fotokopi | 03-fotokopi | ⬜ |
| 4 | Mesajlaşma | 04-mesajlasma | ⬜ |
| 5 | Dokümanlar | 05-dokumanlar | ⬜ |
| 6 | Takvim | 06-takvim | ⬜ |
| 7 | Yayın hazırlığı | 07-yayin | ⬜ |

---

## Faz 0 — Kurulum (sıralı, ana oturum)

- [x] **0-X01** Git deposunu başlat, `.gitignore` dosyasını yaz. Kapsam: `backend/target`, `frontend/.next`, `node_modules`, `.env`, `data/` → 00 §3
- [x] **0-B01** Backend iskeletini kur:
  - start.spring.io ile projeyi oluştur ve ek bağımlılıkları ekle
  - `application.yml` ile `dev` ve `prod` profillerini yaz
  - `AppProperties` sınıfını yaz
  - `@EnableScheduling` ve `@EnableAsync` ekle
  - `GET /api/health` uç noktasını yaz (204 döner)

  → 00 §3, §4.6, §16
- [x] **0-F01** Frontend iskeletini kur:
  - create-next-app, paketler ve shadcn bileşenleri
  - `globals.css` tema değişkenleri, Inter fontu
  - `next.config.ts` ve script'ler
  - ESLint'te `no-img-element` kuralını kapat

  → 00 §3, §11.2, §12.1
- [x] **0-X02** Dağıtım dosyalarını yaz:
  - `deploy/docker-compose.dev.yml` (db ve mailpit)
  - `deploy/docker-compose.yml`, `deploy/nginx/default.conf`
  - `backend/Dockerfile`, `frontend/Dockerfile`
  - `.env.example`, `deploy/backup.sh`

  Doğrulama: `docker compose -f deploy/docker-compose.dev.yml up -d`, ardından backend `spring-boot:run` ile ayağa kalkar ve `/api/health` 204 döner. → 00 §15

---

## Faz 1 — Çekirdek

### Dalga 1.1 [P]

**Backend**

- [x] **1-B01** `V1__core.sql` dosyasını yaz; Spring Session şemasını ekle → 01 §3
- [x] **1-B02** `common/error` paketini yaz: `ApiError`, `ErrorCode`, `CommonErrorCode`, `BusinessException`, `Messages`, `GlobalExceptionHandler`, `ErrorResponseWriter` ve `TraceIdFilter` → 00 §6.1–6.3
- [x] **1-B03** Ortak yardımcıları yaz: `common/util` (`TextNormalizer`, `PhoneNormalizer`, `Csv`), `PageResponse`, `CursorResponse` → 00 §5, §7
- [x] **1-B04** Çekirdek entity ve repository'leri yaz: `Branch`, `AppUser`, `Season`, `SeasonMembership`, `StaffGroup`, `StaffGroupMember`, `AuthToken` → 01 §4
- [x] **1-B05** Güvenliği yaz:
  - `SecurityConfig` (session, CSRF spa, entry point, access denied)
  - `UserContext`, `UserContextFilter`, `UserContextArgumentResolver`
  - `AdminPathInterceptor`, `SessionService`

  → 00 §8, 01 §2.3, §5

**Frontend**

- [x] **1-F01** `lib` dosyalarını yaz: `api.ts`, `query-client.ts`, `form.ts` (`useApiForm`), `validation.ts`, `text.ts`, `format.ts`, `me.ts` (`useMe`) ve kök `Providers` + `Toaster` → 00 §6.4, §7, §11.3
- [x] **1-F02** Ortak bileşenleri yaz: `PageHeader`, `EmptyState`, `ErrorState`, `PageSkeleton`/`ListSkeleton`, `ConfirmDialog`, `ResponsiveDialog`, `UserAvatar`, `DateText`, `DataTable`, `StatusBadge`, `ArchiveBanner`, `InfiniteList` → 00 §11.4
- [x] **1-F03** Kimlik sayfalarını yaz: `(auth)/layout`, `/giris`, `/sifremi-unuttum`, `/sifre-belirle` → 01 §6.1, §10 S-1, S-2, S-3
- [x] **1-F04** AppShell'i yaz:
  - Sidebar, BottomNav, TopBar, `nav-items.ts`
  - Oturum koruması, Suspense sarmalayıcı, KVKK penceresi
  - `/` yönlendirmesi
  - Beş modül için yer tutucu sayfalar

  → 00 §12.2, 01 §10 S-4

### Dalga 1.2 [P]

**Backend**

- [x] **1-B06** `common/audit` paketini yaz: `AuditService`, `AuditAction`, `CoreAuditAction` ve admin audit API'si → 01 §6.10
- [x] **1-B07** `common/file` paketini yaz: `StoredFile`, `FileStorageService` (`store`, `attach`, `serve`, `delete`) ve `POST /api/files` → 00 §9
- [x] **1-B08** `common/notify` paketini yaz:
  - `WebSocketConfig` ve kanal interceptor'ı
  - `RealtimePublisher`, `NotificationService`, `WebPushSender`
  - Push abonelik API'si, bildirim API'si
  - `BadgeProvider` arayüzü, `/api/me/badges`

  → 00 §10, 01 §6.2, §6.3, §8
- [x] **1-B09** `common/mail/MailService` ve kimlik API'sini yaz:
  - login, logout, me, csrf, kvkk
  - change-password, forgot-password, password-token
  - `LoginAttemptService`, `BootstrapAdminRunner`

  → 01 §2.2, §6.1, §9
- [x] **1-B10** Şube API'sini ve `core/spi` arayüzlerini yaz (hook ve event tanımları) → 01 §6.5, §8

**Frontend**

- [x] **1-F05** Ortak seçicileri yaz: `UserPicker`, `TargetPicker`, `FileUploader` (XHR ilerleme, sıkıştırma) → 00 §9, §11.4, 01 §6.4
- [x] **1-F06** WebSocket ve push katmanını yaz:
  - `lib/ws.ts`: STOMP istemcisi, olay dağıtıcısı, bağlantı şeridi
  - `lib/push.ts`, `public/sw.js`, `app/manifest.ts`, ikonlar
  - Ana ekrana ekleme kartı, bildirimleri aç kartı

  → 00 §10, §13
- [x] **1-F07** Bildirim arayüzünü yaz: zil, Popover, `/bildirimler`, `useBadges` ve rozetler → 01 §6.3, §10 S-5
- [x] **1-F08** Profil sayfasını yaz (`/profil`) → 01 §10 S-6

### Dalga 1.3 [P]

**Backend**

- [x] **1-B11** Personel yönetimini yaz:
  - Liste, detay, lookup, titles
  - create, update, send-setup-link
  - deactivation-check, deactivate, reactivate
  - Çekirdeğin `UserDeactivationHook` uygulaması

  → 01 §2.6, §6.6, §8
- [x] **1-B12** CSV içe aktarmayı yaz: şablon, import ve asenkron e-posta işi → 01 §6.7
- [x] **1-B13** Yıl ve atama API'sini yaz: liste, CRUD, memberships, batch, bulk-update, bulk-delete, copy, unassigned, activation-preview, activate → 01 §2.4, §2.5, §6.8
- [x] **1-B14** Grup API'sini, rehber aramasını ve `AudienceResolver` servisini yaz → 01 §2.8, §2.9, §6.4, §6.9
- [x] **1-B15** `MaintenanceJobs` ve `DevDataSeeder` sınıflarını yaz → 01 §9

**Frontend**

- [x] **1-F09** Yönetim yerleşimini ve Şubeler ekranını yaz → 01 §10 S-7, S-11
- [x] **1-F10** Personel ekranını yaz: liste, form, `MembershipFields`, pasifleştir/aktifleştir, şifre bağlantısı, toplu yükleme → 01 §10 S-8
- [x] **1-F11** Yıllar ekranlarını yaz: liste, detay, kopyala, toplu işlemler, aktifleştir → 01 §10 S-9, S-10
- [x] **1-F12** Gruplar ve Denetim Kaydı ekranlarını yaz → 01 §10 S-12, S-13

### Faz 1 kapanışı (sıralı, ana oturum)

- [x] **1-B16** Zorunlu testleri yaz (T-C1…T-C6) ve `./mvnw -q test` çalıştır → 01 §11, 00 §14
- [x] **1-X01** Entegrasyon kontrolü:
  1. db, mailpit, backend (dev) ve frontend'i çalıştır.
  2. Şu akışı curl ile doğrula: `csrf → login (mudur@okul.test) → me → directory/search → notifications`.
  3. `npm run lint && npm run build` çalıştır.
  4. DTO alan adı uyumsuzluklarını düzelt.

  → 01 §6

---

## Faz 2 — Akış

### Dalga 2.1 [P]

**Backend**

- [x] **2-B01** `V2__feed.sql` dosyasını yaz → 02 §3
- [x] **2-B02** `feed/` entity, repository, `FeedErrorCode`, DTO'lar ve `FeedVisibility` sorgu parçasını yaz → 02 §4, §5, §6.1, §7
- [x] **2-B03** Akış servisleri ve API'sini yaz: liste, detay, paylaşım, düzenleme, silme, sabitleme, beğeni, yorum, okundu, okuma durumu, hatırlatma; bildirim, canlı olay ve rozet entegrasyonu → 02 §2, §5.2, §5.3, §6.2, §6.3
- [x] **2-B04** `FeedDevSeeder` sınıfını ve `dev/` örnek dosyalarını yaz → 02 §9

**Frontend**

- [x] **2-F01** Akış sayfası (`/akis`), `PostCard` ve akış veri kancalarını yaz → 02 §8.1, §8.2
- [x] **2-F02** Paylaşım penceresi (`Composer`), gönderi detayı (`/akis/[id]`) ve Okuma Durumu penceresini yaz → 02 §8.3, §8.4, §8.5

### Faz 2 kapanışı (sıralı, ana oturum)

- [x] **2-B05** Zorunlu testleri yaz (T-F1…T-F3) ve `./mvnw -q test` çalıştır → 02 §10, 00 §14
- [x] **2-X01** Entegrasyon kontrolü: db, mailpit, backend (dev), frontend; curl ile akış uçlarını doğrula; `npm run lint && npm run build`; DTO alan adı uyumsuzluklarını düzelt → 02 §6

---

## Faz 3 — Fotokopi

### Dalga 3.1 [P]

**Backend**

- [x] **3-B01** `V3__print.sql` dosyasını yaz → 03 §3
- [x] **3-B02** `print/` entity, repository, enum, `PrintErrorCode`, `PrintAuditAction`, DTO'lar → 03 §4, §5.1, §6
- [x] **3-B03** `PrintAccess`, `PrintRequestService` (onaylayıcı belirleme, durum makinesi), `PrintDelegationService`, `PrintPageCounter`, `PrintQueries` → 03 §2, §4
- [x] **3-B04** `PrintController` uç noktaları, doğrulama, bildirim ve canlı olaylar → 03 §5.2, §5.3, §2.9
- [x] **3-B05** `PrintSeasonHook`, `PrintDeactivationHook`, `PrintMembershipRemovalHook`, `PrintBadgeProvider`, `PrintJobs`, `PrintDevSeeder` → 03 §2.6–2.8, §8, §9

**Frontend**

- [x] **3-F01** `/fotokopi` ana sayfa, `PrintRequestCard`, Taleplerim sekmesi, veri kancaları → 03 §7.1–7.3
- [x] **3-F02** Yeni talep (`/fotokopi/yeni`) ve talep detayı (`/fotokopi/[id]`) → 03 §7.4, §7.5
- [x] **3-F03** Onay Bekleyenler, Fotokopi Kuyruğu ve Rapor sekmeleri → 03 §7.6–7.8

### Faz 3 kapanışı (sıralı, ana oturum)

- [ ] **3-B06** Zorunlu testleri yaz ve `./mvnw -q test` çalıştır → 03 §10, 00 §14
- [ ] **3-X01** Entegrasyon kontrolü: db, mailpit, backend (dev), frontend; curl ile fotokopi uçlarını doğrula; `npm run lint && npm run build`; DTO uyumsuzluklarını düzelt → 03 §5

---

## Faz 4 — Mesajlaşma

### Dalga 4.1 [P]

**Backend**

- [ ] **4-B01** `V4__chat.sql` dosyasını yaz → 04 §3
- [ ] **4-B02** `chat/` entity, repository, `ChatErrorCode`, DTO'lar → 04 §4, §6.1, §7
- [ ] **4-B03** Sohbet servisleri: türler, erişim, gönderme, silme, okundu/görüldü, sessize alma, sorgular → 04 §2, §5
- [ ] **4-B04** `ChatController` uç noktaları, canlı olaylar, push, `ChatGroupSync` → 04 §2.8, §2.9, §6.2, §6.3
- [ ] **4-B05** `ChatDevSeeder` → 04 §9

**Frontend**

- [ ] **4-F01** `/mesajlar` yerleşim ve sohbet listesi, Yeni Sohbet penceresi → 04 §8.1–8.3
- [ ] **4-F02** Sohbet ekranı ve Sohbet Bilgisi paneli → 04 §8.4, §8.5

### Faz 4 kapanışı (sıralı, ana oturum)

- [ ] **4-B06** Zorunlu testleri yaz ve `./mvnw -q test` çalıştır → 04 §10, 00 §14
- [ ] **4-X01** Entegrasyon kontrolü (curl ile sohbet uçları, WS, lint, build) → 04 §6

---

## Faz 5 — Dokümanlar

### Dalga 5.1 [P]

**Backend**

- [ ] **5-B01** `V5__dms.sql` dosyasını yaz → 05 §3
- [ ] **5-B02** `dms/` entity, repository, `DmsErrorCode`, DTO'lar → 05 §4, §6.1, §7
- [ ] **5-B03** Yetki, alan, klasör/dosya, yükleme, çöp kutusu, taşıma, arama servisleri → 05 §2, §5
- [ ] **5-B04** `DmsController`, bildirimler, `DmsDeactivationHook`, `DmsDevSeeder` → 05 §2.7, §2.8, §6.2, §6.3, §8

**Frontend**

- [ ] **5-F01** Alan listesi (`/dokumanlar`) ve Alan gezgini → 05 §9.1, §9.2
- [ ] **5-F02** Yetkiler penceresi, Alan Ayarları, Çöp Kutusu, bağlantı rotası → 05 §9.3–9.6

### Faz 5 kapanışı (sıralı, ana oturum)

- [ ] **5-B05** Zorunlu testleri yaz ve `./mvnw -q test` çalıştır → 05 §10, 00 §14
- [ ] **5-X01** Entegrasyon kontrolü (curl ile doküman uçları, lint, build) → 05 §6

---

## Faz 6 — Takvim

### Dalga 6.1 [P]

**Backend**

- [ ] **6-B01** `V6__calendar.sql` dosyasını yaz → 06 §3
- [ ] **6-B02** `calendar/` entity, repository, `CalendarErrorCode`, DTO'lar → 06 §4, §5.1, §6
- [ ] **6-B03** `CalendarBootstrap`, kayıt, rezervasyon, paylaşım, bildirim servisleri → 06 §2
- [ ] **6-B04** `CalendarController`, doğrulama, `CalendarDevSeeder` → 06 §5.2, §5.3, §8

**Frontend**

- [ ] **6-F01** Kütüphane ve Takvim sayfası (ay/hafta/gün/liste), takvim listesi → 06 §7.1–7.3
- [ ] **6-F02** Kayıt Formu, Kayıt Detayı ve yönetim pencereleri → 06 §7.4–7.6

### Faz 6 kapanışı (sıralı, ana oturum)

- [ ] **6-B05** Zorunlu testleri yaz ve `./mvnw -q test` çalıştır → 06 §9, 00 §14
- [ ] **6-X01** Entegrasyon kontrolü (curl ile takvim uçları, lint, build) → 06 §5

---

## Faz 7 — Yayın hazırlığı (sıralı, ana oturum)

- [ ] **7-X01** Prod yapılandırma kontrolü → 07 §1
- [ ] **7-X02** Güvenlik kontrol listesi → 07 §2
- [ ] **7-X03** Yedekleme ve geri yükleme testi → 07 §3
- [ ] **7-X04** Uçtan uca duman testi → 07 §4
- [ ] **7-X05** `deploy/KURULUM.md` ve yıl devri prosedürü → 07 §5, §6
