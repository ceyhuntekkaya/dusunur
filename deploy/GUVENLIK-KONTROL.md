# Güvenlik Kontrol Listesi

Kontroller dev ortamında curl ile yapıldı (2026-11-14). Prod'da (nginx arkasında, https) yeniden çalıştırılmalıdır.

- [x] 1. Oturumsuz `/api/**` istekleri 401 `UNAUTHENTICATED` döner (feed, print, chat, dms, calendar, admin, me/badges denendi).
- [x] 2. `X-XSRF-TOKEN` başlığı olmayan POST 403 `CSRF_INVALID` döner.
- [x] 3. Yetkisiz erişim 404 döner:
  - [x] ogretmen2 → Matematik Zümresi gönderisi: `FEED_POST_NOT_FOUND`
  - [x] fotokopi.kuzey → Merkez kuyruğundaki talep: `PRINT_REQUEST_NOT_FOUND`
  - [x] ogretmen3 → ogretmen1 ile ogretmen2 arasındaki sohbet: `CHAT_CONVERSATION_NOT_FOUND`
  - [x] ogretmen2 → "Matematik Zümresi Arşivi" alanı: `DMS_SPACE_NOT_FOUND`
  - [x] ogretmen2 → Ali'nin paylaşılmamış takvim kaydı: `CAL_ENTRY_NOT_FOUND`
- [x] 4. Yönetici olmayan kullanıcı `/api/admin/**` çağırınca 403 `CORE_ADMIN_ONLY` alır.
- [x] 5. `.exe` yükleme `FILE_TYPE_NOT_ALLOWED`, uzantısı değiştirilmiş dosya `FILE_CONTENT_MISMATCH` döner.
- [x] 6. 5. hatalı girişte hesap kilitlenir (`AUTH_ACCOUNT_LOCKED`).
- [x] 7. Hata yanıtlarında stack trace veya SQL yok; yalnızca `code`, `message`, `traceId`.
- [x] 8. `X-Content-Type-Options` ve `X-Frame-Options` uygulamadan döner. `Strict-Transport-Security` yalnızca https isteğinde (proxy başlığıyla) döner; nginx de ekler. Prod'da https üzerinden doğrulanmalıdır.
