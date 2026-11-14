---
name: devam
description: PROGRESS.md'deki sıradaki görevlerden devam eder. Planı uygular, derler, işaretler ve commit eder; karar gerekmedikçe durmadan sonraki göreve geçer.
---

# /devam — kesintisiz geliştirme döngüsü

Bu döngü kullanıcı durdurana ya da bir karar gerekene kadar sürer. Görev aralarında özet yazma ve onay isteme.

1. `PROGRESS.md` dosyasını oku. İlk tamamlanmamış dalgayı bul; ⛔ işaretli görevleri atla.
2. **Dalga `[P]` işaretli değilse** görevleri sırayla kendin yap. Her görevde:
   1. `→` ile gösterilen plan bölümünü oku.
   2. Uygula.
   3. Derleme kontrolünü yap (CLAUDE.md'de tanımlı).
   4. Görevi `[x]` yap.
   5. Commit et: `git add -A && git commit -q -m "[ID] …"`.
3. **Dalga `[P]` işaretliyse** iki alt ajanı **aynı mesajda**, Agent aracıyla başlat:
   - `backend-dev`'e ver:
     - dalganın Backend görev ID'leri ve açıklamaları
     - görevlerin `→` referansları
     - şu not: "PROGRESS.md'yi ve git'i değiştirme."
   - `frontend-dev`'e aynı biçimde Frontend görevlerini ver.
   - İki ajan da `run_in_background: false` ile başlar ve ikisinin bitmesi beklenir.
4. **Ajanlar dönünce:**
   1. Her iki tarafta derleme kontrolünü kendin bir kez yap:
      - `cd backend && ./mvnw -q -DskipTests compile`
      - `cd frontend && npm run typecheck`
   2. Hata varsa düzelt.
   3. Ajanların bitirdiği görevleri `[x]` yap.
   4. Tek commit at: `[1.2] Dalga 1.2 tamam` gibi.
5. **Ajan karar sorusu döndürdüyse** (iş kuralı):
   1. AskUserQuestion ile kullanıcıya sor. İlk seçenek ajanın önerisi olsun ve "(Önerilen)" etiketi taşısın.
   2. Cevabı `docs/KARARLAR.md` dosyasına `D-xx` olarak ekle.
   3. İlgili görevi ilgili ajanla yeniden yürüt. SendMessage ile aynı ajana devam ettirebilirsin.
6. **Faz kapanışında** "kapanış" görevlerini sırayla yap (testler, entegrasyon kontrolü). Ardından PROGRESS'teki Faz özeti tablosunda fazı ✅ yap.
7. **Sonraki fazın plan dosyası henüz yoksa** (tabloda "yazılacak" yazıyorsa) dur. Kullanıcıya tek cümle yaz: "Faz N tamamlandı; Faz N+1 için plan dosyası bekleniyor."

## Kurallar

- Ajanlara görev verirken plan içeriğini kopyalama. Yalnızca dosya ve bölüm referansını ver; ajan kendisi okur. Bu token tasarrufu sağlar.
- Bir ajan hata döndürürse ve sorun 2 denemede çözülmezse kullanıcıya iki çözüm önerisiyle sor.
