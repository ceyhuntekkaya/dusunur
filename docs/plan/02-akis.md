# 02 — Akış

Kurumun resmî iç paylaşım ve duyuru kanalıdır. Sosyal medya akışı gibi görünür, ama bir iş aracıdır.

**Temel ilke:** Herkes yalnızca kendisine gönderileni görür. Önemli olanın okunduğu da kayıt altına alınır.

Bağlı kararlar: D-10, D-11, D-12, D-30 – D-34. Ortak kurallar `00-mimari.md`'de, hedef çözümleme `01-cekirdek.md` §2.9 ve §6.4'te tanımlıdır.

---

## §1 Kapsam

### Var

- Normal gönderi ve Önemli Duyuru
- Alıcı seçimi: kişi, grup, şube, Tüm Kurum
- Metin ve en fazla 10 ek (görsel, video, dosya)
- Tek seviye yorum, tek tepki ("Beğen")
- Önemli duyuruda "Okudum" onayı, okuma durumu ve "Okumayanlara hatırlat"
- Sabitleme (Yönetici, en fazla 3 gönderi)
- Filtreler, geçmiş yıl arşivi, "yeni paylaşımlar var" uyarısı
- Menü rozeti: okunmamış önemli duyuru sayısı

### Yok

Aşağıdakiler **yapılmaz**:

- Anket
- Zengin metin editörü
- Bağlantı önizlemesi
- @bahsetme
- Kaydetme / yer imi
- Taslak
- Zamanlanmış gönderi
- Alıcı veya ek düzenleme
- Yorum düzenleme
- Beğenenler listesi
- Doküman modülünden dosya ekleme
- Gönderi süresi veya bitiş tarihi
- Yeni yıla gönderi taşıma

---

## §2 İş kuralları

### 2.1 Kim neye paylaşabilir

| Kullanıcı | Seçebileceği hedefler | Önemli Duyuru |
|---|---|---|
| Personel (yıla atanmış) | `USER` (aktif yıla atanmış her kişi), `GROUP` (yalnızca üyesi olduğu gruplar) | ✘ |
| Yönetici | `USER`, `GROUP` (tüm arşivlenmemiş gruplar), `BRANCH`, `ALL` | ✔ |

Hedef seçerken uygulanan kurallar:

- **Yazarın kendisi:** Kişi kendini alıcı olarak seçerse sessizce listeden çıkarılır. Liste boşalırsa `TARGET_REQUIRED` döner.
- **Hedef sayısı:** En fazla 50 hedef satırı seçilebilir; aşılırsa `FEED_TOO_MANY_TARGETS`.
- **Doğrulama sırası:** Önce `AudienceResolver.validate(me, targets, allowed)` çalışır. Ardından, kişi Yönetici değilse her `GROUP` hedefi için üyelik kontrol edilir; üye değilse `FEED_GROUP_NOT_MEMBER`.

### 2.2 Görünürlük

Silinmemiş bir gönderiyi kullanıcı `U` ancak aşağıdakilerden **biri** doğruysa görür:

1. `U` gönderinin yazarıdır.
2. Hedeflerde `USER = U` vardır.
3. Hedeflerde `GROUP = G` vardır ve `U`, `G` grubunun **şu anki** üyesidir. Arşivli gruplar da dahildir.
4. Hedeflerde `BRANCH = B` vardır ve `U` **gönderinin yılında** `B` şubesine atanmıştır.
5. Hedeflerde `ALL` vardır ve `U` **gönderinin yılına** atanmıştır.

Bu kuralların sonuçları:

- Gruptan çıkan kişi o grubun gönderilerini artık göremez. Gruba yeni katılan kişi grubun geçmiş gönderilerini görür.
- Yönetici de olsa, alıcı olmadığı gönderiyi göremez.
- Görünmeyen gönderi istenirse "bulunamadı" hatası döner (`FEED_POST_NOT_FOUND`). Gönderinin varlığı açığa vurulmaz.

### 2.3 Eğitim-öğretim yılı

- **Yıl bağlama:** Gönderi oluşturulurken `season_id = me.seasonId()` olarak kaydedilir.
- **Geçmiş yıl gönderileri:** Salt okunurdur. Beğeni, yorum, okudum onayı, düzenleme, silme, sabitleme ve hatırlatma yapılamaz; hepsi `me.requireWritable(post.seasonId)` kontrolünden geçer. Arayüzde bu kontroller gizlenir ve `ArchiveBanner` gösterilir.
- **Varsayılan görünüm:** Akış aktif yılı gösterir. Yıl seçiciyle, kullanıcının atanmış olduğu geçmiş yıllara geçilebilir.
- **Sabitli gönderiler:** Yalnızca aktif yılın gönderileri sabitli olarak gösterilir.

### 2.4 Düzenleme, silme, sabitleme

| İşlem | Kim yapabilir | Kural |
|---|---|---|
| Metni düzenleme | Yalnızca yazar | Ekleri olmayan bir gönderinin metni boş bırakılamaz (`FEED_EMPTY_POST`). `edited_at = now`. |
| Gönderiyi silme | Yazar veya gönderiyi görebilen Yönetici | Soft delete: `deleted_at`, `deleted_by`. Ek dosyalar fiziksel olarak silinir (`FileStorageService.delete`). Silen kişi yazar değilse denetim kaydı yazılır. |
| Sabitleme / kaldırma | Gönderiyi görebilen Yönetici | Aktif yılda en fazla 3 sabitli gönderi olabilir; aşılırsa `FEED_PIN_LIMIT`. |
| Yorum silme | Yorumun yazarı, gönderinin yazarı veya Yönetici | Soft delete |

### 2.5 Önemli Duyuru

- **Okudum onayı:** Alıcı "Okudum" butonuyla okuduğunu onaylar. Tekrar onaylamak bir şey değiştirmez (idempotent). Yazarın onayı gerekmez; yazar onay verirse işlem sessizce yok sayılır (204).
- **Okuyan ve okumayanlar:** `alıcılar = AudienceResolver.resolveUserIds(post.seasonId, targets) − {yazar}`. Okuyanlar, alıcılar içinden `feed_read` kaydı olanlardır; okumayanlar geri kalanlardır. Alıcılar her seferinde yeniden hesaplanır; gruba sonradan katılan kişi de okumayanlar listesine girer.
- **Okuma durumunu görme:** Yalnızca yazar görebilir (D-33).
- **Hatırlatma:** Yalnızca yazar gönderebilir. `last_reminder_at` alanı son 24 saat içindeyse `FEED_REMIND_TOO_SOON` döner. Okumayan kimse yoksa `FEED_REMIND_NONE` döner.
- **Rozet:** Okunmamış önemli duyurular `feedImportant` rozetinde sayılır. Kapsam: aktif yıl, kullanıcının görebildiği, yazarı kendisi olmayan ve okumadığı duyurular.

### 2.6 Bildirimler ve canlı olaylar

| Olay | Kime | type | title | body | link |
|---|---|---|---|---|---|
| Önemli duyuru yayınlandı | alıcılar − yazar | `FEED_IMPORTANT` | "Önemli duyuru: {yazarAdı}" | metnin ilk 120 karakteri, metin yoksa "{n} ek dosya" | `/akis/{id}` |
| Normal gönderi, kişi hedefli | `USER` hedefleri (aktif ve atanmış) − yazar | `FEED_DIRECT` | "{yazarAdı} sizinle bir paylaşım yaptı" | aynı | `/akis/{id}` |
| Yorum yapıldı | gönderinin yazarı (yorumcu yazar değilse) | `FEED_COMMENT` | "{yorumcuAdı} paylaşımınıza yorum yaptı" | yorumun ilk 120 karakteri | `/akis/{id}` |
| Hatırlatma | okumayanlar | `FEED_REMINDER` | "Hatırlatma: Okumanız gereken bir önemli duyuru var" | metnin ilk 120 karakteri | `/akis/{id}` |

Notlar:

- Bildirim içindeki metin kısaltmalarında satır sonları boşluğa çevrilir.
- Önemli duyuruda `FEED_DIRECT` ayrıca gönderilmez.
- **WebSocket olayı `FEED_NEW_POST`:** Gönderi oluşturulunca tüm alıcılara (yazar hariç) `{ postId, seasonId }` payload'ıyla gönderilir. Önemli duyuruda ayrıca `BADGES` olayı gider.
- **Okudum sonrası:** Okudum onayından sonra yalnızca o kullanıcıya `BADGES` olayı gönderilir.

---

## §3 Veritabanı — `V2__feed.sql`

```sql
CREATE TABLE feed_post (
  id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  season_id        bigint        NOT NULL REFERENCES season(id),
  author_id        bigint        NOT NULL REFERENCES app_user(id),
  content          varchar(5000),
  important        boolean       NOT NULL DEFAULT false,
  pinned_at        timestamptz,
  pinned_by        bigint        REFERENCES app_user(id),
  edited_at        timestamptz,
  last_reminder_at timestamptz,
  created_at       timestamptz   NOT NULL DEFAULT now(),
  updated_at       timestamptz   NOT NULL DEFAULT now(),
  deleted_at       timestamptz,
  deleted_by       bigint        REFERENCES app_user(id)
);
CREATE INDEX ix_feed_post_season  ON feed_post (season_id, id DESC) WHERE deleted_at IS NULL;
CREATE INDEX ix_feed_post_author  ON feed_post (author_id, id DESC) WHERE deleted_at IS NULL;
CREATE INDEX ix_feed_post_pinned  ON feed_post (season_id, pinned_at DESC) WHERE pinned_at IS NOT NULL AND deleted_at IS NULL;

CREATE TABLE feed_post_target (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  post_id     bigint      NOT NULL REFERENCES feed_post(id) ON DELETE CASCADE,
  target_type varchar(10) NOT NULL CHECK (target_type IN ('USER','GROUP','BRANCH','ALL')),
  target_id   bigint,
  CONSTRAINT ck_feed_target_id CHECK ((target_type = 'ALL') = (target_id IS NULL))
);
CREATE UNIQUE INDEX ux_feed_target ON feed_post_target (post_id, target_type, COALESCE(target_id, 0));
CREATE INDEX ix_feed_target_lookup ON feed_post_target (target_type, target_id, post_id);

CREATE TABLE feed_post_attachment (
  post_id    bigint   NOT NULL REFERENCES feed_post(id) ON DELETE CASCADE,
  file_id    uuid     NOT NULL REFERENCES stored_file(id),
  sort_order smallint NOT NULL,
  PRIMARY KEY (post_id, file_id)
);

CREATE TABLE feed_comment (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  post_id     bigint        NOT NULL REFERENCES feed_post(id),
  author_id   bigint        NOT NULL REFERENCES app_user(id),
  content     varchar(1000) NOT NULL,
  created_at  timestamptz   NOT NULL DEFAULT now(),
  deleted_at  timestamptz,
  deleted_by  bigint        REFERENCES app_user(id)
);
CREATE INDEX ix_feed_comment_post ON feed_comment (post_id, id) WHERE deleted_at IS NULL;

CREATE TABLE feed_like (
  post_id    bigint      NOT NULL REFERENCES feed_post(id),
  user_id    bigint      NOT NULL REFERENCES app_user(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (post_id, user_id)
);

CREATE TABLE feed_read (
  post_id  bigint      NOT NULL REFERENCES feed_post(id),
  user_id  bigint      NOT NULL REFERENCES app_user(id),
  read_at  timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (post_id, user_id)
);
CREATE INDEX ix_feed_read_user ON feed_read (user_id, post_id);
```

Sayaç kolonu **yoktur**. Beğeni ve yorum sayıları sayfa yüklenirken toplu sorguyla hesaplanır (§5).

---

## §4 Entity ve sınıflar (`feed/`)

| Sınıf | Not |
|---|---|
| `FeedPost`, `FeedPostTarget`, `FeedPostAttachment`(+Id), `FeedComment`, `FeedLike`(+Id), `FeedRead`(+Id) | JPA entity'leri. İlişki anotasyonu kullanılmaz (`00-mimari.md` §4.2). |
| `FeedVisibility` | Görünürlük SQL parçasının tek kaynağıdır (§5.1) |
| `FeedQueries` | JdbcClient ile sayfa, detay, rozet ve okuma durumu sorguları |
| `FeedService` | Gönderi oluşturma, düzenleme, silme, sabitleme |
| `FeedInteractionService` | Beğeni, okudum, hatırlatma, yorum |
| `FeedController`, `FeedDtos`, `FeedErrorCode`, `FeedAuditAction` | |
| `FeedBadgeProvider` | `BadgeProvider` uygulaması; anahtar `feedImportant` |
| `FeedDevSeeder` | Yalnızca `dev` profilinde ve `feed_post` boşsa çalışır (§9) |

---

## §5 Sorgular

### 5.1 Görünürlük parçası (`FeedVisibility.PREDICATE`)

Gönderi tablosunun takma adı `p`, parametresi `:uid` olur:

```sql
(p.author_id = :uid OR EXISTS (
   SELECT 1 FROM feed_post_target t
   WHERE t.post_id = p.id AND (
        (t.target_type = 'USER'   AND t.target_id = :uid)
     OR (t.target_type = 'GROUP'  AND t.target_id IN (SELECT gm.group_id FROM staff_group_member gm WHERE gm.user_id = :uid))
     OR (t.target_type = 'BRANCH' AND t.target_id = (SELECT sm.branch_id FROM season_membership sm
                                                     WHERE sm.season_id = p.season_id AND sm.user_id = :uid))
     OR (t.target_type = 'ALL'    AND EXISTS (SELECT 1 FROM season_membership sm2
                                              WHERE sm2.season_id = p.season_id AND sm2.user_id = :uid)))))
```

Bu parça şu yerlerde **aynen** kullanılır: liste, detay, erişim kontrolü (`assertVisible(postId, uid)`), rozet ve sabitli gönderiler. Başka yerde görünürlük mantığı yazılmaz.

### 5.2 Liste — `GET /api/feed/posts`

Taban sorgu:

```sql
SELECT p.id FROM feed_post p
WHERE p.deleted_at IS NULL AND p.season_id = :seasonId AND {PREDICATE}
  {filtre koşulu}
  AND (:before IS NULL OR p.id < :before)
ORDER BY p.id DESC
LIMIT :size + 1
```

`size + 1` satır çekilir; fazladan satır gelirse `nextCursor` hesaplanır.

Filtre koşulları:

| `filter` | Ek koşul |
|---|---|
| `all` (varsayılan) | `AND p.pinned_at IS NULL` — sabitliler ayrı uç noktadan gelir. Yalnızca aktif yıl için geçerlidir; geçmiş yılda bu koşul eklenmez. |
| `important` | `AND p.important` |
| `unreadImportant` | `AND p.important AND p.author_id <> :uid AND NOT EXISTS (SELECT 1 FROM feed_read r WHERE r.post_id = p.id AND r.user_id = :uid)` |
| `direct` | `AND EXISTS (SELECT 1 FROM feed_post_target t2 WHERE t2.post_id = p.id AND t2.target_type = 'USER' AND t2.target_id = :uid)` |
| `mine` | `AND p.author_id = :uid` |
| `group` (+`groupId`) | `AND EXISTS (SELECT 1 FROM feed_post_target t3 WHERE t3.post_id = p.id AND t3.target_type = 'GROUP' AND t3.target_id = :groupId)` |

`filter=group` ile istenen grubun üyesi değilse boş liste döner; hata verilmez. Görünürlük zaten bunu sağlar.

### 5.3 Sayfa doldurma (N+1 yok)

Bulunan `ids` listesi için `FeedQueries.loadCards(ids, uid)` aşağıdaki **sabit sayıda** sorguyu çalıştırır ve sonuçları birleştirir:

1. Gönderi ve yazar bilgisi. Yazarın unvanı ve şubesi, gönderinin yılındaki atamadan gelir:

   ```sql
   SELECT p.*, u.first_name, u.last_name, u.status AS author_status, sm.title AS author_title, b.name AS author_branch
   FROM feed_post p JOIN app_user u ON u.id = p.author_id
   LEFT JOIN season_membership sm ON sm.season_id = p.season_id AND sm.user_id = p.author_id
   LEFT JOIN branch b ON b.id = sm.branch_id
   WHERE p.id IN (:ids)
   ```
2. Hedefler ve hedef adları. `GROUP`, `BRANCH` ve `USER` adları JOIN ile alınır; `ALL` için ad "Tüm Kurum" olur.
3. Ekler: `feed_post_attachment` JOIN `stored_file`, `sort_order` sıralı.
4. Beğeni sayısı ve beğenip beğenmediğim: `SELECT post_id, count(*), bool_or(user_id = :uid) FROM feed_like WHERE post_id IN (:ids) GROUP BY post_id`
5. Yorum sayısı: `SELECT post_id, count(*) FROM feed_comment WHERE post_id IN (:ids) AND deleted_at IS NULL GROUP BY post_id`
6. Son 2 yorum: `ROW_NUMBER() OVER (PARTITION BY post_id ORDER BY id DESC) <= 2`. Sonuç kronolojik sıraya çevrilir.
7. Önemli gönderilerde okudum bilgisi: `SELECT post_id FROM feed_read WHERE user_id = :uid AND post_id IN (:importantIds)`
8. Yazarı ben olan önemli gönderiler için `readStats`: her biri için `AudienceResolver` + `feed_read` sayımı yapılır. Sayfada genellikle 0–2 gönderi bu durumdadır.

Bu yöntem pinned, detay ve oluşturma yanıtında da aynen kullanılır.

---

## §6 API

### 6.1 DTO'lar

```ts
type FeedAuthor = { id: number; fullName: string; title?: string; branchName?: string; former: boolean }; // former: status=PASSIVE
type FeedAttachment = { fileId: string; name: string; contentType: string; sizeBytes: number; kind: 'IMAGE'|'VIDEO'|'FILE' };
type CommentDto = { id: number; postId: number; author: FeedAuthor; content: string; createdAt: string; canDelete: boolean };
type PostDto = {
  id: number; seasonId: number; archived: boolean;          // archived: seasonId != aktif yıl
  author: FeedAuthor; content?: string; important: boolean; pinned: boolean;
  editedAt?: string; createdAt: string;
  targets: TargetView[];                                      // 01-cekirdek §6.4
  attachments: FeedAttachment[];
  likeCount: number; liked: boolean; commentCount: number; latestComments: CommentDto[];
  readByMe?: boolean;                                         // yalnızca important ve yazar ben değilsem
  readStats?: { read: number; total: number };                // yalnızca important ve yazar bensem
  canEdit: boolean; canDelete: boolean; canPin: boolean;      // archived ise hepsi false
};
type CreatePostRequest = { targets: TargetRef[]; content?: string; fileIds: string[]; important: boolean };
type FeedSeason = { id: number; name: string; active: boolean };
type ReadStatusResponse = { read: ReadUser[]; unread: ReadUser[]; lastReminderAt?: string };
type ReadUser = { userId: number; fullName: string; title?: string; branchName?: string; readAt?: string };
```

`kind` alanı şöyle belirlenir: `contentType` `image/` ile başlıyorsa `IMAGE`, `video/` ile başlıyorsa `VIDEO`, diğer durumlarda `FILE`.

### 6.2 Uç noktalar

Tüm uçlarda `me.requireMember()` çağrılır. Gönderi id'si alan uçlarda önce `assertVisible` çalışır; görünmüyorsa `FEED_POST_NOT_FOUND` döner.

**Okuma uçları**

| Uç nokta | Yanıt | Kurallar |
|---|---|---|
| `GET /api/feed/seasons` | `FeedSeason[]` | Kullanıcının atandığı yıllar; `name DESC` |
| `GET /api/feed/posts?seasonId=&filter=&groupId=&before=&size=10` | `CursorResponse<PostDto>` | `seasonId` varsayılan olarak aktif yıldır. Kullanıcı o yıla atanmamışsa liste boş döner. `size` en fazla 30. |
| `GET /api/feed/pinned` | `PostDto[]` | Aktif yılda görünen sabitli gönderiler, `pinned_at DESC`, en fazla 3 |
| `GET /api/feed/posts/{id}` | `PostDto` | |
| `GET /api/feed/posts/{id}/comments?after=&size=50` | `CursorResponse<CommentDto>` | `id ASC` sıralı (eskiden yeniye); `after` ile devam edilir |
| `GET /api/feed/posts/{id}/files/{fileId}?download=false` | dosya akışı | Dosya bu gönderinin eki değilse `FILE_NOT_FOUND`. `download=true` ise `attachment`, değilse `inline` gönderilir (`00-mimari.md` §9). |

**Gönderi işlemleri**

| Uç nokta | İstek | Yanıt | Kurallar |
|---|---|---|---|
| `POST /api/feed/posts` | `CreatePostRequest` | 201 `{ post: PostDto, recipientCount: number }` | §2.1 ve §6.3 doğrulamaları. Dosyalar `attach(fileId, me, FEED)` ile bağlanır; `sort_order` gönderilen sıraya göre verilir. Bildirim ve WS §2.6'ya göre gönderilir. |
| `PUT /api/feed/posts/{id}` | `{ content?: string }` | `PostDto` | §2.4. `requireWritable` çağrılır. Yazar değilse `FEED_EDIT_FORBIDDEN`. |
| `DELETE /api/feed/posts/{id}` | — | 204 | §2.4. Yetki yoksa `FEED_DELETE_FORBIDDEN`. Denetim kaydı (yazar değilse): `FEED_POST_DELETED`, detayda `{ authorId, snippet }`. |
| `POST /api/feed/posts/{id}/pin` | — | 204 | Yönetici değilse `FEED_PIN_FORBIDDEN`. Ayrıca `FEED_PIN_LIMIT`. Zaten sabitliyse bir şey değişmez. Denetim: `FEED_POST_PINNED`. |
| `POST /api/feed/posts/{id}/unpin` | — | 204 | Denetim: `FEED_POST_UNPINNED` |

**Etkileşim**

| Uç nokta | İstek | Yanıt | Kurallar |
|---|---|---|---|
| `POST /api/feed/posts/{id}/like` | — | `{ liked: true, likeCount }` | Beğeni zaten varsa bir şey değişmez (`ON CONFLICT DO NOTHING`) |
| `DELETE /api/feed/posts/{id}/like` | — | `{ liked: false, likeCount }` | |
| `POST /api/feed/posts/{id}/read` | — | 204 | Önemli değilse `FEED_NOT_IMPORTANT`. Yazar ise işlem yapılmaz. `ON CONFLICT DO NOTHING`. Ardından kullanıcıya `BADGES` WS olayı. |
| `GET /api/feed/posts/{id}/reads` | — | `ReadStatusResponse` | Önemli değilse `FEED_NOT_IMPORTANT`, yazar değilse `FEED_READS_FORBIDDEN`. Listeler Türkçe alfabetik sıralıdır. |
| `POST /api/feed/posts/{id}/remind` | — | `{ notified: number }` | Yazar değilse `FEED_READS_FORBIDDEN`. Ayrıca `FEED_REMIND_TOO_SOON({saat})` ve `FEED_REMIND_NONE`. `last_reminder_at = now` yapılır. |
| `POST /api/feed/posts/{id}/comments` | `{ content }` | 201 `CommentDto` | `requireWritable`. Bildirim: `FEED_COMMENT`. |
| `DELETE /api/feed/comments/{id}` | — | 204 | Yorum yoksa veya gönderisi görünmüyorsa `FEED_COMMENT_NOT_FOUND`. Yetki yoksa `FEED_COMMENT_DELETE_FORBIDDEN`. Denetim kaydı (yorumu yazan kişi değilse): `FEED_COMMENT_DELETED`. |

`FEED_REMIND_TOO_SOON` mesajındaki saat `last_reminder_at + 24 saat` değeridir ve "14 Kasım 17:20" biçiminde, Europe/Istanbul saatiyle yazılır.

### 6.3 Doğrulama tablosu

Frontend ve backend aynı kuralları uygular:

| Alan | Kural | Mesaj |
|---|---|---|
| `targets` | En az 1, en fazla 50 | "En az bir alıcı seçin." / FEED_TOO_MANY_TARGETS |
| `content` | Trim edilir, en fazla 5000 karakter. Boş string `null` sayılır. | "Metin en fazla 5000 karakter olabilir." |
| `content` + `fileIds` | İkisinden en az biri dolu olmalı | FEED_EMPTY_POST |
| `fileIds` | En fazla 10, tekrarsız UUID | FEED_TOO_MANY_FILES |
| `important` | `true` yalnızca Yönetici için | FEED_IMPORTANT_FORBIDDEN |
| Yorum `content` | Trim edilir, 1–1000 karakter | "Yorum 1–1000 karakter olmalıdır." |

---

## §7 Hata kodları (`FeedErrorCode`)

| code | HTTP | message |
|---|---|---|
| FEED_POST_NOT_FOUND | 404 | Gönderi bulunamadı veya silinmiş. |
| FEED_EMPTY_POST | 400 | Paylaşım için bir metin yazın veya dosya ekleyin. |
| FEED_TOO_MANY_FILES | 400 | Bir paylaşıma en fazla 10 dosya eklenebilir. |
| FEED_TOO_MANY_TARGETS | 400 | En fazla 50 alıcı seçebilirsiniz. Daha geniş bir kitle için grup veya şube seçin. |
| FEED_IMPORTANT_FORBIDDEN | 403 | Önemli duyuruyu yalnızca yöneticiler yayınlayabilir. |
| FEED_GROUP_NOT_MEMBER | 403 | Yalnızca üyesi olduğunuz gruplarla paylaşım yapabilirsiniz. |
| FEED_EDIT_FORBIDDEN | 403 | Yalnızca kendi gönderinizi düzenleyebilirsiniz. |
| FEED_DELETE_FORBIDDEN | 403 | Bu gönderiyi silme yetkiniz yok. |
| FEED_PIN_FORBIDDEN | 403 | Gönderileri yalnızca yöneticiler sabitleyebilir. |
| FEED_PIN_LIMIT | 409 | En fazla 3 gönderi sabitlenebilir. Önce sabitlenmiş gönderilerden birini kaldırın. |
| FEED_NOT_IMPORTANT | 400 | Bu işlem yalnızca önemli duyurular için yapılabilir. |
| FEED_READS_FORBIDDEN | 403 | Okuma durumunu yalnızca duyuruyu yayınlayan kişi görebilir. |
| FEED_REMIND_TOO_SOON | 429 | Hatırlatma 24 saatte bir gönderilebilir. Bir sonraki hatırlatmayı {0} sonrasında gönderebilirsiniz. |
| FEED_REMIND_NONE | 400 | Duyuruyu okumayan kimse kalmadı. |
| FEED_COMMENT_NOT_FOUND | 404 | Yorum bulunamadı veya silinmiş. |
| FEED_COMMENT_DELETE_FORBIDDEN | 403 | Bu yorumu silme yetkiniz yok. |

Geçmiş yıl işlemlerinde ortak kod `SEASON_READ_ONLY`, hedef hatalarında çekirdeğin `TARGET_*` kodları kullanılır.

**Denetim işlemleri (`FeedAuditAction`):**

| Kod | Etiket |
|---|---|
| FEED_POST_DELETED | Akış gönderisi silindi |
| FEED_POST_PINNED | Akış gönderisi sabitlendi |
| FEED_POST_UNPINNED | Akış sabitlemesi kaldırıldı |
| FEED_COMMENT_DELETED | Akış yorumu silindi |

---

## §8 Ekranlar

### 8.1 Akış sayfası (`/akis`)

**Masaüstü (≥1024px):**

- Orta sütun en fazla 640px genişliktedir.
- 1280px ve üzerinde sağda 300px'lik bir yan sütun açılır. İçinde "Okumanız Gereken Duyurular" kartı bulunur:
  - Okunmamış önemli duyuruların listesi, en fazla 5 öğe. Her öğede yazar adı, metin kısaltması ve `DateText` vardır; tıklanınca `/akis/{id}` açılır.
  - Liste boşsa: yeşil onay ikonu ve "Okunmamış önemli duyurunuz yok."

**Mobil:** Tek sütun. Paylaş butonu sağ altta FAB olarak durur.

**Sayfa düzeni (üstten alta):**

1. **Ana ekrana ekleme / bildirim kartı:** Çekirdekte tanımlı, koşullu olarak görünür.
2. **Paylaşım kutusu:**
   - Masaüstünde ve tablette görünür: avatar ve gri, yuvarlak kenarlı "Ne paylaşmak istersiniz?" alanı. Sağında 📎 ve 🖼 ikonları vardır.
   - Kutunun herhangi bir yerine tıklanınca Paylaşım penceresi açılır (§8.3).
   - Mobilde bu kutu yoktur; yerine FAB "Paylaş" (Plus ikonu) kullanılır.
3. **Okunmamış önemli duyuru şeridi:** Yalnızca `badges.feedImportant > 0` ise ve 1280px'ten dar ekranlarda görünür.
   - Görünüm: amber arka plan (`--important-bg`) ve sol kenarda 4px'lik amber çizgi.
   - Metin: "Okumanız gereken {n} önemli duyuru var." Yanında "Göster" butonu; `filter=unreadImportant` filtresini seçer.
4. **Filtre çipleri:** Yatay kaydırılabilir.
   - Çipler: `Tümü` · `Önemli` · `Bana Özel` · `Paylaşımlarım` · `Gruplar ▾`
   - `Gruplar ▾` açılır menüsü `/api/groups/mine` listesini gösterir. Bir grup seçilince çip "Grup: {ad} ✕" olur.
   - Sağda ⋯ menüsü bulunur: "Eğitim-öğretim yılı" alt menüsü (`/api/feed/seasons`). Geçmiş bir yıl seçilince şerit gösterilir: `ArchiveBanner` ve "Bu yıla dön" bağlantısı.
   - Seçimler URL'de tutulur: `?filtre=onemli|okunmamis|bana-ozel|benim|grup&grup={id}&yil={seasonId}`.
5. **"Yeni paylaşımlar var" uyarısı:**
   - `FEED_NEW_POST` olayı gelirse, olay aktif yıla aitse ve kullanıcı sayfanın en üstünde değilse listenin üstünde sabit duran mavi bir hap belirir: "↑ Yeni paylaşımlar".
   - Tıklanınca sayfa en üste kayar ve liste yenilenir.
   - Kullanıcı zaten en üstteyse hap gösterilmez; liste doğrudan yenilenir.
6. **Sabitli gönderiler:** Yalnızca `Tümü` filtresinde ve aktif yılda gösterilir. Her kartın üstünde "📌 Sabitlendi" etiketi (Pin ikonu ve muted metin) bulunur.
7. **Gönderi listesi:** `InfiniteList` ile `PostCard` kartları; kartlar arası boşluk 12px.
   - İlk yüklemede 3 iskelet kart gösterilir.

**Boş durumlar:**

| Durum | Metin | Buton |
|---|---|---|
| Tümü | "Henüz paylaşım yok." | "Paylaşım Yap" |
| Önemli | "Önemli duyuru yok." | — |
| Okunmamış | "Okunmamış önemli duyurunuz yok. Harika!" | — |
| Bana Özel | "Size özel bir paylaşım yok." | — |
| Paylaşımlarım | "Henüz bir şey paylaşmadınız." | "Paylaşım Yap" |
| Grup | "Bu grupta henüz paylaşım yok." | — |
| Geçmiş yıl | "Bu yılda görüntüleyebileceğiniz paylaşım yok." | — |

### 8.2 `PostCard` bileşeni (sosyal medya görünümü)

Kart beyazdır; kenarlık `rounded-xl` ve `border` ile verilir. Önemli duyurularda ek olarak sol kenarda 4px'lik amber çizgi bulunur ve kartın üst kısmının arka planı `--important-bg` olur.

**1. Başlık satırı:**

- Solda `UserAvatar md`.
- Avatarın sağında **Yazar Adı** (semibold) ve yanında küçük gri metin: unvan · şube. Yazar eski personelse yanında "(Eski personel)" yazar.
- İkinci satır (muted, küçük):
  - `DateText`; üzerine gelince tam tarih tooltip'te görünür.
  - Düzenlenmişse "· düzenlendi" eklenir.
  - Hedefler: "· → Matematik Zümresi, Ali Çelik". En fazla 2 hedef adı yazılır, fazlası "+3" olarak gösterilir. "+3" tıklanınca bir Popover tam listeyi açar.
  - Hedef ikonları: `ALL` → Building2, `BRANCH` → MapPin, `GROUP` → Users, `USER` → User.
- Sağda ⋯ menüsü. Yalnızca yetkili olunan öğeler görünür:
  - "Düzenle"
  - "Sabitle" ya da "Sabitlemeyi kaldır"
  - "Okuma durumu" (yalnızca önemli duyurunun yazarı)
  - "Sil" (kırmızı)
- Önemli duyuruda başlığın üstünde amber bir rozet bulunur: Megaphone ikonu ve "ÖNEMLİ DUYURU".

**2. Metin:**

- `whitespace-pre-wrap break-words`, 15px.
- 6 satırdan uzunsa `line-clamp-6` uygulanır ve altında "Devamını gör" bağlantısı çıkar. Taşma `scrollHeight > clientHeight` ile ölçülür.
- Bağlantılar `lib/linkify.tsx` ile tıklanabilir yapılır:
  - Regex: `/(https?:\/\/[^\s<]+|www\.[^\s<]+)/g`
  - `www.` ile başlayanlara `https://` eklenir.
  - `<a target="_blank" rel="noopener noreferrer" class="text-primary underline-offset-2 hover:underline">`
  - HTML hiçbir koşulda yorumlanmaz.

**3. Ekler:**

Görseller ızgara olarak gösterilir. Her hücre `rounded-lg`, `object-cover`, `loading="lazy"`, `alt={dosya adı}` özelliklerini taşır; kaynak `src=/api/feed/posts/{id}/files/{fileId}`.

| Görsel sayısı | Yerleşim |
|---|---|
| 1 | Tam genişlik, en fazla 480px yükseklik, `object-contain`, gri arka plan |
| 2 | 2 sütun, kare |
| 3 | Solda büyük, sağda alt alta iki küçük |
| 4 ve üzeri | 2×2 ızgara; 4. hücrede koyu katman üzerinde "+{n}" |

- **Ortak bileşenler:** Görsel ızgarası `components/common/AttachmentGrid`, görüntüleyici `components/common/ImageViewer` olarak yazılır; Mesajlaşma modülü de bunları kullanır. Prop olarak `src` üretici fonksiyon alırlar: `(fileId, download) => url`.
- **Görsel görüntüleyici:** Bir görsele tıklanınca tam ekran bir pencere (`Dialog`) açılır.
  - Siyah arka plan, ortada görsel.
  - Sol/sağ oklar ve klavye ok tuşları ile gezinilir.
  - Üstte "{i} / {n}", "İndir" ve "Kapat" bulunur.
  - Mobilde ekranın sol ve sağ üçte birine dokunarak gezinilir.
- **Videolar:** `<video controls preload="metadata" class="rounded-lg w-full max-h-[480px]">`
- **Diğer dosyalar:** Satır kartı olarak gösterilir: tür ikonu (FileText, FileSpreadsheet, FileArchive, File), dosya adı (tek satır, `truncate`), boyut ("2,4 MB") ve sağda İndir ikonu (`?download=true`).
- **Boyut gösterimi:** `lib/format.ts` içindeki `formatBytes()` ile, Türkçe ondalık virgülle yapılır.

**4. Sayaç satırı:** Beğeni veya yorum varsa gösterilir.

- Format: "👍 {likeCount}" ve "{commentCount} yorum". İkisi küçük ve muted metindir.
- Yorum sayısına tıklanınca yorumlar açılır.
- Önemli duyurunun yazarı için ek bir öğe gösterilir: "✓ {read}/{total} okudu". Tıklanınca Okuma Durumu penceresi açılır.

**5. Aksiyon çubuğu:** Üstünde ince bir ayraç, eşit genişlikte butonlar, her biri `ghost` ve `h-10`.

| Buton | Davranış |
|---|---|
| **Beğen** | ThumbsUp ikonu; beğenilmişse dolu ve `primary` renkte. Arayüz hemen güncellenir (optimistic); hata olursa geri alınır. |
| **Yorum Yap** | MessageSquare ikonu. Yorum alanını açar ve odaklanır. |
| **Okudum** | Yalnızca önemli duyuruda, yazar değilse ve okunmamışsa görünür. Belirgin, amber renkte bir buton: "Okudum, anladım". Tıklanınca "✓ Okudunuz" yazan pasif yeşil bir etikete döner. Toast: "Okuduğunuz kaydedildi." |

Geçmiş yılda aksiyon çubuğu gösterilmez; yalnızca sayaçlar kalır.

**6. Yorumlar:**

- `latestComments` gösterilir. Her yorum:
  - `UserAvatar sm`
  - Gri baloncuk içinde **ad** ve metin
  - Altında `DateText relative` ve yetkiliyse "Sil" bağlantısı
- `commentCount > 2` ise "Tüm yorumları gör ({n})" bağlantısı kartın içinde tüm yorumları yükler (`/comments`).
- **Yorum alanı:**
  - `UserAvatar sm` ve otomatik büyüyen bir textarea (en fazla 4 satır). Placeholder: "Yorum yazın…"
  - Gönder ikon butonu; masaüstünde Enter gönderir, Shift+Enter yeni satır açar. Mobilde yalnızca buton gönderir.
  - Alanın altında sayaç yalnızca 900 karakteri geçince görünür.

**Onay pencereleri:**

- **Gönderiyi silme:** "Gönderi silinecek. Bu işlem geri alınamaz." Yazar değilse ek cümle: "Gönderi {yazar} tarafından paylaşılmıştı." Toast: "Gönderi silindi."
- **Yorum silme:** "Yorum silinsin mi?" Toast: "Yorum silindi."

**Düzenleme:** Metin aynı kart içinde textarea'ya dönüşür; altında "Kaydet" ve "Vazgeç" butonları bulunur. Toast: "Gönderi güncellendi."

**Sabitleme toast'ları:** "Gönderi sabitlendi." / "Sabitleme kaldırıldı."

### 8.3 Paylaşım penceresi (`Composer`)

Pencere `ResponsiveDialog` ile açılır: masaüstünde 600px genişliğinde, mobilde tam yükseklikte bir Drawer. Başlık: "Yeni Paylaşım".

**Alanlar (yukarıdan aşağı):**

1. **Kime\*** — `TargetPicker`
   - İzin verilen tipler: Yönetici için `['ALL','BRANCH','GROUP','USER']`, diğerleri için `['GROUP','USER']`.
   - Grup kapsamı: Yönetici için `all`, diğerleri için `mine`.
   - Yönetici için en üstte "Tüm Kurum" hızlı seçim çipi bulunur.
   - Seçilenler çip olarak görünür.
   - Yardım metni (Yönetici olmayan): "Üyesi olduğunuz grupları veya kişileri seçebilirsiniz."
2. **Metin** — otomatik büyüyen textarea; en az 4, en fazla 12 satır. Placeholder: "Ne paylaşmak istersiniz?" 4500 karakteri geçince "{n}/5000" sayacı görünür.
3. **Ekler** — `FileUploader` (`purpose=FEED`, `multiple`, `compressImages`).
   - Görseller küçük önizleme (thumbnail) olarak, diğer dosyalar satır olarak listelenir. Her öğede ✕ bulunur.
   - En fazla 10 dosya; aşılırsa istemcide toast: "Bir paylaşıma en fazla 10 dosya eklenebilir."
4. **Önemli duyuru** — yalnızca Yönetici görür.
   - Anahtar: "Önemli duyuru olarak yayınla".
   - Yardım metni: "Tüm alıcılar bildirim alır ve 'Okudum' onayı vermeleri istenir. Kimlerin okuduğunu görebilirsiniz."
   - Açıkken pencerenin kenarı amber renk alır.

**Butonlar:**

- "Paylaş" (birincil) ve "Vazgeç".
- Dosya yüklemesi sürerken "Paylaş" butonu pasiftir ve "Dosyalar yükleniyor…" yazar.

**Kapatma:** İçerik varken pencere kapatılmak istenirse `ConfirmDialog` sorar: "Paylaşımınız kaydedilmedi. Çıkmak istediğinize emin misiniz?"

**Başarı:**

- Normal: "Paylaşımınız yayınlandı."
- Önemli: "Önemli duyuru yayınlandı. {recipientCount} kişiye bildirim gönderildi."
- Başarıdan sonra liste en üste kaydırılır ve yenilenir.

**zod şeması:** §6.3 ile aynıdır. Metin ve dosya boşsa form düzeyinde hata gösterilir: "Paylaşım için bir metin yazın veya dosya ekleyin."

### 8.4 Gönderi detayı (`/akis/[id]`)

- Üstte "← Akış" geri bağlantısı bulunur.
- Altında tek bir `PostCard` gösterilir; tüm yorumlar açıktır.
- Gönderi bulunamazsa `EmptyState`: "Gönderi bulunamadı veya silinmiş. Size gönderilmemiş bir paylaşım da olabilir." Buton: "Akışa dön".
- Bildirimlerden açılan sayfa budur. Önemli duyuru okunmamışsa "Okudum" butonu görünür durumdadır; otomatik onay **verilmez**.

### 8.5 Okuma Durumu penceresi

- Başlık: "Okuma Durumu".
- Alt başlık: "{read}/{total} kişi okudu".
- Sekmeler: `Okumayanlar ({n})` (varsayılan) ve `Okuyanlar ({n})`.
- Her satırda avatar, ad, unvan · şube; okuyanlarda ayrıca `DateText`.
- Listenin üstünde arama kutusu bulunur; istemci tarafında `fold()` ile filtreler.
- **Altta "Okumayanlara Hatırlat" butonu:**
  - Okumayan yoksa pasiftir.
  - `lastReminderAt` son 24 saat içindeyse pasiftir; yanında "Son hatırlatma: {DateText}" yazar.
  - Toast: "{notified} kişiye hatırlatma gönderildi."

---

## §9 Geliştirme verisi (`FeedDevSeeder`)

`dev` profilinde ve `feed_post` tablosu boşsa çalışır. Çekirdek verisiyle oluşturulan kullanıcılar kullanılır.

| Yazar | Tip | Hedef | İçerik |
|---|---|---|---|
| mudur | Önemli, sabitli | ALL | "Öğretmenler Kurulu 20 Kasım Perşembe 15:00'te konferans salonunda yapılacaktır. Katılım zorunludur." |
| my.kuzey | Normal | BRANCH Kuzey | Kampüs duyurusu ve bir görsel eki |
| zumre.mat | Normal | GROUP Matematik Zümresi | Zümre toplantısı notu ve bir PDF eki. Ali'nin bir yorumu ve bir beğenisi. |
| ogretmen1 | Normal | USER Zeynep Şahin | Kişiye özel mesaj, www bağlantısı içerir |
| mudur | Önemli | GROUP Yönetim Kurulu | Okunmamış durumda |

Eklerde kullanılacak görsel ve PDF dosyaları `backend/src/main/resources/dev/` klasörüne konur: 1 küçük png ve 1 tek sayfalık pdf.

---

## §10 Zorunlu testler

| No | Senaryo |
|---|---|
| T-F1 | **Görünürlük:**<br>(a) G grubuna gönderilen gönderiyi G üyesi görür; üye olmayan göremez (detay isteğinde `FEED_POST_NOT_FOUND`).<br>(b) Üye gruptan çıkarılınca gönderiyi artık göremez.<br>(c) BRANCH hedefini yalnızca o şubenin personeli görür.<br>(d) Alıcı olmayan Yönetici göremez.<br>(e) ALL hedefini yıla atanmış herkes görür. |
| T-F2 | **Yetki:**<br>(a) Yönetici olmayan ALL hedefiyle paylaşınca `TARGET_TYPE_NOT_ALLOWED` döner.<br>(b) Yönetici olmayan `important=true` gönderince `FEED_IMPORTANT_FORBIDDEN` döner.<br>(c) Üyesi olmadığı gruba paylaşınca `FEED_GROUP_NOT_MEMBER` döner. |
| T-F3 | **Önemli duyuru ve arşiv:**<br>(a) `reads` yanıtında okumayanlar = alıcılar − okuyanlar − yazar.<br>(b) 24 saat içinde ikinci hatırlatma `FEED_REMIND_TOO_SOON` döner.<br>(c) Kapanmış yılın gönderisine beğeni `SEASON_READ_ONLY` döner. |
