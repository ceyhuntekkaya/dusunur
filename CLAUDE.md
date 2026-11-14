# CLAUDE.md — Okul İç İletişim Sistemi

Tek kurum (2 şube, artabilir) için iç iletişim uygulaması. Modüller: Akış, Fotokopi, Mesajlaşma, Dokümanlar (DYS), Takvim. Mobil uyumlu web + PWA. Okulun kendi sunucusunda Docker Compose ile çalışır.

## Altın kurallar

1. **Planın dışına çıkma.** Yalnızca `docs/plan/` içinde yazanı yap. Planda olmayan özellik, alan, ekran, ayar, kütüphane veya soyutlama EKLEME. "İleride lazım olur" diye kod yazma.
2. **İş kuralı belirsizse DUR ve SOR.** Plan bir işleyişi açık bırakıyor ya da kendisiyle çelişiyorsa AskUserQuestion ile sor:
   - İlk seçenek senin önerin olsun ve "(Önerilen)" etiketi taşısın.
   - Gerekçeyi bir cümleyle yaz.
   - Cevabı `docs/KARARLAR.md` dosyasına ekle.
   Teknik ayrıntılarda (isimlendirme, yardımcı sınıf, sorgu biçimi) SORMA; `00-mimari.md` kurallarına göre kendin karar ver.
3. **Onay isteme, seri ilerle.** Bir görev bitince sonrakine geç. Ara özet, açıklama veya "devam edeyim mi?" yazma.
4. **Hata asla sessiz veya anlamsız dönmez.** Her hata kullanıcıya Türkçe bir mesajla ulaşır. Mesaj ne olduğunu ve kullanıcının ne yapması gerektiğini söyler (`00-mimari.md` §6).
5. **Doğrulama iki tarafta yapılır.** Frontend'de zod, backend'de Bean Validation ve servis kontrolleri kullanılır. Kurallar ve mesajlar plandaki tablolarla birebir aynı olur.
6. **Test yalnızca planın "Zorunlu testler" bölümünde yazanlardır.** Başka test yazma, test çalıştırmak için ayrıca iş yapma.

## Token ve zaman verimliliği

- Yalnızca şunları oku: CLAUDE.md, PROGRESS.md ve görevin işaret ettiği plan bölümü. Başka modüllerin planlarını okuma.
- Bir dosyayı bir kez oku. Değişiklik için Edit kullan; dosyayı baştan yazma. `cat` ile büyük dosya döktürme.
- Kod yorumu yazma; yalnızca açık olmayan bir iş kuralı için tek satır yazılabilir. README veya ek doküman üretme.
- Derleme kontrolü görev sonunda bir kez yapılır:
  - Backend: `./mvnw -q -DskipTests compile`
  - Frontend: `npm run typecheck`
  - `npm run build`, `npm run lint` ve `./mvnw -q test` yalnızca faz sonunda çalışır.
- Uzun komut çıktısını `| tail -40` ile kes.
- Aynı hatayı 3 denemede çözemezsen dur. Sorunu ve iki çözüm önerisini kullanıcıya AskUserQuestion ile sor.

## Çalışma döngüsü

`/devam` komutu döngüyü otomatik yürütür (`.claude/skills/devam/SKILL.md`). Elle çalışırken:

1. PROGRESS.md'deki ilk `[ ]` görevi al. ⛔ işaretli görevi atla.
2. Görevin "→" ile gösterdiği plan bölümünü oku.
3. Uygula ve derleme kontrolünü yap.
4. Hata yoksa görevi `[x]` yap ve commit et: `git add -A && git commit -q -m "[GÖREV-ID] kısa açıklama"`. Push yapılmaz.

## Teknoloji (değiştirme)

- Backend: Java 25, Spring Boot 4.x (Maven), PostgreSQL 18, Flyway.
- Frontend: Next.js 16 (App Router, TypeScript), Tailwind 4, shadcn/ui, TanStack Query 5, react-hook-form ve zod 4, sonner, lucide-react, @stomp/stompjs, date-fns (Türkçe).
- Ayrıntılar: `docs/plan/00-mimari.md`.

## Dil

- Arayüz metinleri Türkçedir ve kullanıcıya "siz" diye hitap eder.
- Kod, tablo, sınıf ve değişken adları İngilizcedir.
- URL yolları Türkçedir: `/akis`, `/mesajlar`, `/fotokopi`, `/dokumanlar`, `/takvim`, `/yonetim`.
- Kullanıcıya görünen terimler:
  - "Eğitim-Öğretim Yılı" (kodda `season`)
  - "Doküman Alanı" (kodda `dms space`)
  - "Fotokopi Talebi" (kodda `print request`)
  - "Grup" (kodda `staff group`)

## Dosya haritası

| Dosya | İçerik |
|---|---|
| `docs/KARARLAR.md` | Onaylı iş kararları. Plan ile çelişirse bu dosya geçerlidir. |
| `docs/plan/00-mimari.md` | Mimari, kod kuralları, hata ve validasyon, güvenlik, UI tasarım sistemi, dağıtım |
| `docs/plan/01-cekirdek.md` | Kimlik, eğitim-öğretim yılı, personel, şube, grup, bildirim, denetim |
| `docs/plan/02-akis.md` … `06-takvim.md` | Modül planları |
| `PROGRESS.md` | Görev listesi ve ilerleme |
