# Kurulum Kılavuzu

Bu kılavuz okulun BT sorumlusu içindir. Komutlar sunucuda, projenin kök klasöründe (örnek: `/opt/iletisim`) çalıştırılır.

## 1. Ön koşullar

- Docker ve Docker Compose v2
- Bir alan adı ve geçerli SSL sertifikası (PWA ve bildirimler HTTPS olmadan çalışmaz)
- Sunucunun internete HTTPS ile çıkışı (push servisleri ve SMTP için)
- SMTP bilgileri (sunucu, port, kullanıcı adı, şifre)

## 2. `.env` dosyası

1. `cp .env.example .env`
2. `.env` dosyasını doldurun: veritabanı şifresi, `APP_BASE_URL` (okulun https adresi), `APP_INSTITUTION_NAME`, SMTP bilgileri, `MAIL_FROM`.
3. VAPID anahtarlarını üretin: `npx web-push generate-vapid-keys`. Çıkan açık anahtarı `VAPID_PUBLIC_KEY`, gizli anahtarı `VAPID_PRIVATE_KEY` olarak yazın.
4. İlk yönetici için `BOOTSTRAP_ADMIN_EMAIL` ve `BOOTSTRAP_ADMIN_PASSWORD` değerlerini yazın.

## 3. Sertifika

Sertifika dosyalarını `deploy/nginx/certs/` klasörüne koyun:

- `fullchain.pem`
- `privkey.pem`

## 4. Kurumsal içerik

- Logo: `frontend/public/logo.svg` dosyasını değiştirin.
- PWA ikonlarını `frontend/public/icons/` klasöründe değiştirin.
- KVKK aydınlatma metnini `backend/src/main/resources/kvkk/aydinlatma.md` dosyasına yazın. Metin sonradan değişirse `backend/src/main/resources/application.yml` içindeki `app.kvkk.version` değerini 1 artırın.

## 5. İlk çalıştırma

1. `docker compose -f deploy/docker-compose.yml up -d --build`
2. Tarayıcıdan okulun adresini açın ve `BOOTSTRAP_ADMIN_*` ile ilk yönetici girişini yapın.
3. `.env` dosyasından `BOOTSTRAP_ADMIN_EMAIL` ve `BOOTSTRAP_ADMIN_PASSWORD` satırlarını silin.
4. `docker compose -f deploy/docker-compose.yml up -d` ile servisleri yeniden başlatın.

## 6. İlk veri sırası

1. Şubeler
2. Eğitim-öğretim yılı
3. CSV ile personel yükleme (`docs/personel-sablon.csv` şablonunu kullanın)
4. Onaylayıcılar ve yetkiler (yıl ekranındaki toplu işlemlerle)
5. Gruplar
6. Yılın aktifleştirilmesi

## 7. Yedekleme

Her gece yedek almak için sunucunun crontab'ına (`crontab -e`) şu satırı ekleyin:

```
30 2 * * * /opt/iletisim/deploy/backup.sh
```

Yedekler `/backup` klasörüne yazılır: `db-YYYY-MM-DD.dump` (veritabanı) ve `files/` (yüklenen dosyalar). 14 günden eski veritabanı yedekleri otomatik silinir.

### Geri yükleme

Komutlar `deploy/` klasöründe çalıştırılır.

1. Backend'i durdurun: `docker compose stop backend`
2. Veritabanını geri yükleyin: `docker compose exec -T db pg_restore -c -U $POSTGRES_USER -d $POSTGRES_DB < /backup/db-YYYY-MM-DD.dump`
3. Dosyaları geri yükleyin: `rsync -a /backup/files/` içeriğini dosya volume'unun içine kopyalayın (yeri: `docker volume inspect deploy_files -f '{{ .Mountpoint }}'`).
4. Backend'i başlatın: `docker compose start backend`
5. Giriş yapın, bir doküman açın ve bir fotokopi talebinin dosyasını indirerek kontrol edin.

Son geri yükleme testi: {tarih}

## 8. Güncelleme

1. `git pull`
2. `docker compose -f deploy/docker-compose.yml up -d --build`

Veritabanı geçişlerini Flyway otomatik uygular.

## 9. Sorun giderme

Kullanıcı ekranda bir hata kodu (`traceId`) iletirse loglarda arayın:

```
docker compose -f deploy/docker-compose.yml logs backend | grep {traceId}
```

## 10. Yıl devri (her yaz)

1. Yeni yıl oluşturulur.
2. Önceki yıldan kopyalama yapılır; ayrılanların işareti kaldırılır.
3. Yeni gelenler CSV ile eklenir.
4. Onaylayıcılar ve yetkiler toplu işlemlerle güncellenir.
5. Gruplar kontrol edilir.
6. Yıl aktifleştirilir. Bu adım eski yılın açık fotokopi taleplerini iptal eder.

Not: Takvim yıldan bağımsız olduğu için yeni yılın kurum takvimi yaz döneminde önceden doldurulabilir.
