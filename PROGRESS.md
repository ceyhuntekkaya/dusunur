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
| 3 | Fotokopi | 03-fotokopi (yazılacak) | ⬜ |
| 4 | Mesajlaşma | 04-mesajlasma (yazılacak) | ⬜ |
| 5 | Dokümanlar | 05-dokumanlar (yazılacak) | ⬜ |
| 6 | Takvim | 06-takvim (yazılacak) | ⬜ |
| 7 | Yayın hazırlığı | 07-yayin (yazılacak) | ⬜ |

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
