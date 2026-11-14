# 07 — Yayın Hazırlığı

Bu fazda yeni özellik geliştirilmez. Amaç, sistemi okul sunucusunda çalışır, yedeklenir ve güvenli hale getirmektir.

---

## §1 Prod yapılandırma kontrolü

Aşağıdaki ayarlar tek tek kontrol edilir ve gerekirse düzeltilir.

| Kontrol | Beklenen |
|---|---|
| `COOKIE_SECURE` | `true` |
| `APP_BASE_URL` | Okulun https adresi. Aynı değer `allowed-origins` ayarında da kullanılır. |
| `DevDataSeeder` ve modül seeder'ları | Yalnızca `@Profile("dev")` ile çalışır; prod'da kesinlikle devre dışıdır |
| `logging.level` | prod profilinde `INFO` |
| Actuator | Eklenmemiştir (bağımlılıklar arasında yoktur) |
| `spring.jpa.show-sql` | `false` |
| nginx | `client_max_body_size 110m`. `/ws` için upgrade başlıkları ayarlıdır. Güvenlik başlıkları eklenmiştir (`00-mimari.md` §8). |
| VAPID anahtarları | `.env` dosyasında doludur. `NEXT_PUBLIC_VAPID_PUBLIC_KEY` frontend build'ine geçirilmiştir. |
| `BOOTSTRAP_ADMIN_*` | İlk açılıştan sonra `.env` dosyasından silinir. Kurulum kılavuzunda (§5) bu adım yazılıdır. |

---

## §2 Güvenlik kontrol listesi

Kontroller curl ile yapılır ve sonuçları `deploy/GUVENLIK-KONTROL.md` dosyasına işaretlenir.

1. **Oturum açmadan erişim.** Oturumsuz yapılan tüm `/api/**` istekleri 401 `UNAUTHENTICATED` dönmelidir. Hariç tutulanlar `permitAll` listesindeki uçlardır.
2. **CSRF koruması.** `X-XSRF-TOKEN` başlığı olmayan bir POST isteği 403 `CSRF_INVALID` dönmelidir.
3. **Yetkisiz erişim.** Dev verisiyle, her modül için "erişimi olmayan kullanıcı" senaryosu denenir. Hepsi 404 `*_NOT_FOUND` dönmelidir:
   - ogretmen2 → Matematik Zümresi gönderisi
   - fotokopi.kuzey → Merkez kuyruğundaki talep
   - ogretmen3 → ogretmen1 ile ogretmen2 arasındaki sohbet
   - ogretmen2 → "Matematik Zümresi Arşivi" doküman alanı
   - ogretmen2 → Ali'nin paylaşılmamış takvimi
4. **Yönetim uçları.** Yönetici olmayan bir kullanıcı `/api/admin/**` çağırdığında 403 `CORE_ADMIN_ONLY` almalıdır.
5. **Dosya yükleme.** `.exe` dosyası ya da uzantısı değiştirilmiş bir dosya yüklenmeye çalışılınca `FILE_TYPE_NOT_ALLOWED` veya `FILE_CONTENT_MISMATCH` dönmelidir.
6. **Kaba kuvvet koruması.** 5 hatalı giriş denemesinden sonra hesap kilitlenmelidir.
7. **Hata içeriği.** Hata yanıtlarında stack trace ya da SQL ifadesi bulunmamalıdır. 500 hatası yalnızca bir mesaj ve `traceId` göstermelidir.
8. **Güvenlik başlıkları.** `X-Content-Type-Options`, `X-Frame-Options` ve `Strict-Transport-Security` başlıkları yanıtlarda bulunmalıdır.

---

## §3 Yedekleme ve geri yükleme testi

1. Prod ortamında `deploy/backup.sh` elle bir kez çalıştırılır.
2. Ayrı bir test ortamında, betiğin başındaki geri yükleme adımları izlenir: `pg_restore` ile veritabanı, `rsync` ile dosyalar geri yüklenir.
3. Geri yüklenen sistemde giriş yapılır, rastgele bir doküman açılır ve bir fotokopi talebinin dosyası indirilir.
4. Sonuç `deploy/KURULUM.md` dosyasına "Son geri yükleme testi: {tarih}" satırıyla yazılır.

---

## §4 Uçtan uca duman testi (manuel, rol bazlı)

Testler gerçek tarayıcıda, masaüstünde **ve** bir telefonda (iOS Safari ana ekran uygulaması + Android Chrome) yapılır. Sonuçlar `deploy/DUMAN-TESTI.md` dosyasına işaretlenir.

| Rol | Akış |
|---|---|
| Sistem Yöneticisi | Şube ekle → yeni yıl oluştur → CSV ile personel yükle → yılı aktifleştir → bir personeli pasifleştir (devralan seçerek) |
| Personel | Şifre belirleme e-postası → giriş → KVKK onayı → bildirimleri aç → Akış'ta okudum onayı → fotokopi talebi → mesaj gönder → doküman indir → takvime kayıt ekle |
| Onaylayıcı | Push bildirimi gelir → talebi onaylar → vekil tanımlar |
| Fotokopi Sorumlusu | Kuyruktan Başla → Hazır → Teslim Et; talep sahibine "Hazır" bildirimi gider |
| Yönetici | Önemli duyuru yayınla → okumayanlara hatırlat → fotokopi raporunu CSV olarak indir → salon ekle → kurum takvimine kayıt ekle |

---

## §5 Kurulum kılavuzu (`deploy/KURULUM.md`)

Bu dosya projede yazılan **tek kullanıcı dokümanıdır**. Hedef okuyucu okulun BT sorumlusudur. Sade ve adım adım yazılır.

1. **Ön koşullar:** Docker ve Compose v2, alan adı ve SSL sertifikası, internete HTTPS çıkışı, SMTP bilgileri.
2. **`.env` dosyası:** `.env.example` dosyası kopyalanır ve doldurulur. VAPID anahtarı üretimi: `npx web-push generate-vapid-keys`.
3. **Sertifika:** Dosyalar `deploy/nginx/certs/` klasörüne konur (`fullchain.pem`, `privkey.pem`).
4. **Kurumsal içerik:** Logo (`frontend/public/logo.svg`) ve PWA ikonları değiştirilir. KVKK aydınlatma metni `backend/src/main/resources/kvkk/aydinlatma.md` dosyasına yazılır; metin değişirse `app.kvkk.version` değeri 1 artırılır.
5. **İlk çalıştırma:** `docker compose -f deploy/docker-compose.yml up -d --build`. Ardından ilk yönetici girişi yapılır ve `BOOTSTRAP_ADMIN_*` değişkenleri `.env` dosyasından silinir.
6. **İlk veri sırası:**
   1. Şubeler
   2. Eğitim-öğretim yılı
   3. CSV ile personel yükleme (`docs/personel-sablon.csv`)
   4. Onaylayıcılar ve yetkiler (yıl ekranındaki toplu işlemlerle)
   5. Gruplar
   6. Yılın aktifleştirilmesi
7. **Yedekleme:** crontab satırı eklenir ve geri yükleme adımları kılavuza yazılır.
8. **Güncelleme:** `git pull` sonrası `docker compose up -d --build`. Veritabanı geçişlerini Flyway otomatik uygular.
9. **Sorun giderme:** Kullanıcı bir hata kodu (`traceId`) iletirse loglarda aranır: `docker compose logs backend | grep {traceId}`.

---

## §6 Yıl devri prosedürü (`KURULUM.md` içinde ayrı bölüm)

Her yaz şu adımlar izlenir:

1. Yeni yıl oluşturulur.
2. Önceki yıldan kopyalama yapılır; ayrılanların işareti kaldırılır.
3. Yeni gelenler CSV ile eklenir.
4. Onaylayıcılar ve yetkiler toplu işlemlerle güncellenir.
5. Gruplar kontrol edilir.
6. Yıl aktifleştirilir. Bu adım eski yılın açık fotokopi taleplerini iptal eder.

**Not:** Takvim yıldan bağımsız olduğu için yeni yılın kurum takvimi yaz döneminde önceden doldurulabilir.
