# 04 — Mesajlaşma

WhatsApp benzeri, sade bir kurum içi mesajlaşma modülüdür. Üç sohbet türü vardır:

| Tür | Kod | Açıklama |
|---|---|---|
| Birebir | `DIRECT` | İki kişi arasındaki sohbet |
| Grup sohbeti | `GROUP` | Çekirdekteki gruba bağlıdır; otomatik oluşur |
| Serbest grup sohbeti | `FREE` | Herkesin seçtiği kişilerle açabildiği sohbet |

Bağlı kararlar: D-09, D-18, D-19, D-43 – D-48. Ortak altyapı (`RealtimePublisher`, `pushOnly`, dosya servisi) `00-mimari.md` §9–§10'da tanımlıdır.

---

## §1 Kapsam

### Var

- Üç sohbet türü
- Metin, en fazla 10 ek (görsel, video, dosya) ve alıntılı yanıt
- Kişinin kendi mesajını herkesten silmesi; yerinde "Bu mesaj silindi" izi kalır
- Birebir sohbette "Görüldü" bilgisi
- Sohbet başına okunmamış sayısı, menü rozeti, Web Push (sohbet bazında gruplanır)
- Sessize alma
- Serbest grup yönetimi: ad değiştirme, üye ekleme ve çıkarma, ayrılma
- Geçmiş yıl mesajları için salt-okunur ayırıcı
- Gerçek zamanlı iletim (WebSocket)

### Yok

Aşağıdakiler **yapılmaz**:

- Mesaj düzenleme
- Mesaj iletme
- Yıldızlama
- Sabitleme
- "Yazıyor…" göstergesi
- Son görülme
- Grup sohbetinde "kim okudu" bilgisi
- Mesaj içinde arama
- Sesli mesaj
- Emoji tepkileri
- Sistem mesajları ("X gruba eklendi" gibi)
- Otomatik mesaj silme
- Yönetici erişimi veya dışa aktarma

---

## §2 İş kuralları

### 2.1 Sohbet türleri

| Tür | Nasıl oluşur | Üyeler | Ad | Yönetim |
|---|---|---|---|---|
| `DIRECT` | `POST /api/chat/direct` çağrılınca; varsa mevcut sohbet döner. Her kişi çifti için tek sohbet olur: `direct_key = "{küçükId}:{büyükId}"`. | 2 kişi | Karşı tarafın adı | Yok |
| `GROUP` | Çekirdekte grup oluşunca (`GroupCreatedEvent`) | Çekirdekteki grubun üyeleri; olaylarla eşitlenir | Grubun adı | Yalnızca `/yonetim/gruplar` üzerinden; sohbet içinden üye değiştirilemez |
| `FREE` | Herhangi bir personel tarafından | Kurucu dahil 3–200 kişi | Kurucunun verdiği ad (2–100 karakter) | Sahip: ad değiştirir, üye ekler ve çıkarır. Herkes ayrılabilir. |

**Birebir sohbet kuralları (D-47):**

- Karşı taraf kişinin kendisi olamaz → `CHAT_DIRECT_SELF`.
- Karşı taraf `ACTIVE` olmalıdır → `CHAT_USER_INACTIVE`. Karşı tarafın yıl ataması aranmaz.
- Henüz mesaj içermeyen birebir sohbetler listede görünmez.

**Serbest grup sahipliği (D-46):**

- Kurucu sahip (`OWNER`) olur.
- Sahip ayrılır, çıkarılır ya da pasifleşirse sahiplik kalan üyelerden en erken katılana geçer; eşitlikte küçük `user_id` seçilir.
- Hiç üye kalmazsa sohbet `read_only = true` olur ve kimsenin listesinde görünmez.

### 2.2 Erişim

- **Kural:** Bir sohbeti ve mesajlarını yalnızca `chat_member` tablosunda kaydı olan kişi görür.
- **Bulunamadı yanıtı:** Üye olmayan biri istek atarsa `CHAT_CONVERSATION_NOT_FOUND` döner; sohbetin varlığı açığa vurulmaz.
- **Üyelikten çıkan kişi:** Gruptan çıkarılan, serbest gruptan ayrılan ya da çıkarılan kişi sohbete erişimini tamamen kaybeder. Sohbet listesinden de kaybolur.
- **Yöneticiler:** Yöneticiler ve Sistem Yöneticisi başkalarının sohbetlerine erişemez. Bunun için bir API yoktur.
- **Yeni katılan üye (D-44):** Sohbetin geçmiş mesajlarını görür. Katılırken `last_read_message_id` o anki son mesaja eşitlenir; böylece geçmiş mesajlar okunmamış sayılmaz.

### 2.3 Gönderme kuralları

Mesaj gönderebilmek için şu koşulların hepsi sağlanmalıdır:

1. Kişi sohbetin üyesidir.
2. Sohbet `read_only` değildir.
3. Sohbet birebir ise karşı taraf `ACTIVE` durumdadır.

Koşul sağlanmazsa `CHAT_READ_ONLY({neden})` döner. Neden metinleri:

| Durum | `readOnlyReason` |
|---|---|
| `GROUP` ve grup arşivli | "Bu grup arşivlendi; yeni mesaj gönderilemez." |
| `DIRECT` ve karşı taraf pasif | "{Ad Soyad} artık aktif personel değil; mesaj gönderilemez." |
| `FREE` ve sohbet sona erdi | "Bu sohbet sona erdi." |

Mesaj kaydedilirken:

- `season_id = me.seasonId()` yazılır.
- **Tekrarlanan istek (idempotency):** İstekte `clientId` (UUID) bulunur. Aynı gönderici aynı `clientId` ile tekrar gönderirse yeni kayıt açılmaz; mevcut mesaj döner (201 yerine 200). Bu, ağ kesintisinde tekrar denemeyi güvenli yapar.

### 2.4 Silme (D-45)

**Kim, ne zaman silebilir:**

- Kişi yalnızca **kendi** mesajını, yalnızca mesaj **aktif yıla** aitse silebilir (`requireWritable`). Süre sınırı yoktur.
- Başkasının mesajını silmeye çalışınca `CHAT_DELETE_FORBIDDEN` döner.

**Silme sırasında yapılanlar:**

- `deleted_at = now`, `content = NULL` yapılır.
- Ekler fiziksel olarak silinir ve ek satırları kaldırılır.

**Silinmiş mesajın görünümü:**

- Mesaj listede "Bu mesaj silindi" olarak kalır.
- Silinmiş mesaja yanıt verilemez → `CHAT_REPLY_INVALID`.
- Daha önce verilmiş yanıtların alıntısında "Silinmiş mesaj" yazar.

### 2.5 Okundu ve Görüldü

- **Okundu işaretleme:** Sohbet ekranı açık ve görünürken istemci `POST …/read { lastMessageId }` gönderir. Sunucu yalnızca daha büyük bir değeri kaydeder: `last_read_message_id = GREATEST(mevcut, yeni)`.
- **Okunmamış sayısı:** `id > last_read_message_id AND sender_id <> me AND deleted_at IS NULL` koşuluna uyan mesajların sayısıdır.
- **Görüldü (yalnızca `DIRECT`):**
  - Benim son mesajımın id'si karşı tarafın `last_read_message_id` değerinden küçük ya da ona eşitse, o mesajın altında "Görüldü" yazar.
  - Karşı taraf okuduğunda bana `CHAT_READ` olayı gelir.
- **Gruplarda** kim okudu bilgisi tutulmaz ve gösterilmez.

### 2.6 Sessize alma (D-43)

- Her üye her sohbet için `muted` bayrağını açıp kapatabilir.
- Sessizdeki sohbet için Web Push gönderilmez ve sohbet `chat` rozetinde sayılmaz.
- Listede okunmamış sayısı gri renkte gösterilir.

### 2.7 Eğitim-öğretim yılı

- **Kalıcı sohbetler:** Sohbetler yıl değişince kapanmaz; her yıl aynı sohbet devam eder. Yıl bilgisi her mesajda `season_id` olarak tutulur.
- **Geçmiş yıl mesajları:** Silinemez. Arayüzde ilgili yerde şu ayırıcı gösterilir: "— {yıl} eğitim-öğretim yılı —". Mesajlar biraz soluk görünür ve menülerinde yalnızca "Kopyala" seçeneği vardır.

### 2.8 Çekirdek olaylarının işlenmesi (`ChatGroupSync`)

| Olay | İşlem |
|---|---|
| `GroupCreatedEvent(id, name, memberIds)` | `GROUP` sohbeti oluşturulur (`title = name`, `staff_group_id = id`). Üyeler `MEMBER` rolüyle eklenir. |
| `GroupUpdatedEvent(id, name)` | `title = name` |
| `GroupMembersAddedEvent(id, userIds)` | Üye eklenir (`ON CONFLICT DO NOTHING`, `last_read_message_id = conversation.last_message_id`). Eklenenlere `CHAT_CONVERSATION_CHANGED` gönderilir. |
| `GroupMembersRemovedEvent(id, userIds)` | Üye satırları silinir. Çıkarılanlara `CHAT_CONVERSATION_CHANGED` gönderilir. |
| `GroupArchivedEvent(id, archived)` | `read_only = archived`. Üyelere `CHAT_CONVERSATION_CHANGED` gönderilir. |
| `UserDeactivatedEvent(userId)` | Kişi tüm `FREE` sohbetlerden çıkarılır; gerekirse sahiplik devredilir (§2.1). `DIRECT` sohbetler kalır. |

**Eksik sohbetlerin tamamlanması:** `ChatGroupSyncRunner` (`ApplicationRunner`, `@Order(100)`) her açılışta çalışır. Sohbeti olmayan grupları bulur ve bu gruplar için sohbet oluşturur. Bu, Mesajlaşma modülünden önce oluşturulmuş gruplar içindir. Arşivli gruplar için sohbet `read_only` olarak açılır.

### 2.9 Canlı olaylar ve push

Tüm WebSocket olayları `/user/queue/events` kanalından gider.

| type | Kime | payload |
|---|---|---|
| `CHAT_MESSAGE` | Tüm üyeler (gönderen dahil; diğer cihazları için) | `{ conversationId, message: MessageDto }`. `mine` ve `canDelete` alanları alıcıya göre hesaplanır. |
| `CHAT_MESSAGE_DELETED` | Tüm üyeler | `{ conversationId, messageId }` |
| `CHAT_READ` | `DIRECT` sohbette karşı taraf | `{ conversationId, lastReadMessageId }` |
| `CHAT_CONVERSATION_CHANGED` | Etkilenen üyeler | `{ conversationId }` |
| `BADGES` | Yeni mesajda gönderen dışındaki üyeler; okundu işaretinde kişinin kendisi | — |

**Push bildirimi:** `pushOnly` kullanılır; bildirim tablosuna satır yazılmaz, yani zilde görünmez.

- **Alıcılar:** Gönderen dışındaki, sohbeti sessize almamış üyeler.
- **Başlık:** `DIRECT` için gönderenin adı soyadı, diğer sohbetlerde sohbet adı.
- **Gövde:**
  - `DIRECT` için mesaj özeti.
  - Diğer sohbetlerde "{gönderenAdı}: {özet}".
- **Diğer alanlar:** `link = /mesajlar/{id}`, `tag = chat-{id}`. Aynı sohbetten gelen bildirimler telefonda tek bildirimde toplanır.

**Özet kuralları:**

- Metin varsa ilk 80 karakter alınır ve satır sonları boşluğa çevrilir.
- Metin yoksa ilk eke göre: "📷 Fotoğraf", "🎥 Video" veya "📎 Dosya".
- Birden fazla ek varsa sonuna "(+{n})" eklenir.

**Rozet (`ChatBadgeProvider`):** `chat` anahtarı, sessize alınmamış ve okunmamış mesajı olan sohbet sayısını verir.

---

## §3 Veritabanı — `V4__chat.sql`

```sql
CREATE TABLE chat_conversation (
  id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  type             varchar(6)   NOT NULL CHECK (type IN ('DIRECT','GROUP','FREE')),
  title            varchar(100),
  direct_key       varchar(41),
  staff_group_id   bigint       REFERENCES staff_group(id),
  read_only        boolean      NOT NULL DEFAULT false,
  last_message_id  bigint,
  last_message_at  timestamptz,
  created_by       bigint       REFERENCES app_user(id),
  created_at       timestamptz  NOT NULL DEFAULT now(),
  updated_at       timestamptz  NOT NULL DEFAULT now(),
  CONSTRAINT ux_chat_direct_key UNIQUE (direct_key),
  CONSTRAINT ux_chat_group      UNIQUE (staff_group_id),
  CONSTRAINT ck_chat_direct CHECK ((type = 'DIRECT') = (direct_key IS NOT NULL)),
  CONSTRAINT ck_chat_group  CHECK ((type = 'GROUP')  = (staff_group_id IS NOT NULL)),
  CONSTRAINT ck_chat_title  CHECK (type = 'DIRECT' OR title IS NOT NULL)
);

CREATE TABLE chat_member (
  conversation_id      bigint      NOT NULL REFERENCES chat_conversation(id),
  user_id              bigint      NOT NULL REFERENCES app_user(id),
  role                 varchar(6)  NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('OWNER','MEMBER')),
  muted                boolean     NOT NULL DEFAULT false,
  last_read_message_id bigint      NOT NULL DEFAULT 0,
  joined_at            timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (conversation_id, user_id)
);
CREATE INDEX ix_chat_member_user ON chat_member (user_id);

CREATE TABLE chat_message (
  id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  conversation_id  bigint        NOT NULL REFERENCES chat_conversation(id),
  season_id        bigint        NOT NULL REFERENCES season(id),
  sender_id        bigint        NOT NULL REFERENCES app_user(id),
  content          varchar(4000),
  reply_to_id      bigint        REFERENCES chat_message(id),
  client_id        uuid          NOT NULL,
  created_at       timestamptz   NOT NULL DEFAULT now(),
  deleted_at       timestamptz,
  CONSTRAINT ux_chat_message_client UNIQUE (sender_id, client_id)
);
CREATE INDEX ix_chat_message_conv ON chat_message (conversation_id, id DESC);

CREATE TABLE chat_message_attachment (
  message_id  bigint   NOT NULL REFERENCES chat_message(id),
  file_id     uuid     NOT NULL REFERENCES stored_file(id),
  sort_order  smallint NOT NULL,
  PRIMARY KEY (message_id, file_id)
);
```

**Son mesaj alanları:** Her yeni mesajda aynı transaction içinde `last_message_id` ve `last_message_at` güncellenir:

```sql
UPDATE chat_conversation SET last_message_id = :mid, last_message_at = now() WHERE id = :cid
```

Son mesaj silinirse bu alanlar değişmez; listede "Bu mesaj silindi" görünür.

---

## §4 Sınıflar (`chat/`)

| Sınıf | Görev |
|---|---|
| `ChatConversation`, `ChatMember`(+Id), `ChatMessage`, `ChatMessageAttachment`(+Id) | Entity'ler. Enum'lar: `ConversationType`, `ChatRole`. |
| `ChatQueries` | Sohbet listesi, detay, mesaj sayfası, okunmamış sayıları (JdbcClient) |
| `ChatConversationService` | `DIRECT` aç, `FREE` oluştur, ad değiştir, üye ekle/çıkar, ayrıl, sessize al, sahiplik devri |
| `ChatMessageService` | Gönder (idempotent), sil, okundu işaretle, dosya akışı, WS ve push |
| `ChatGroupSync` (`@EventListener`), `ChatGroupSyncRunner` | §2.8 |
| `ChatBadgeProvider` | `chat` rozeti |
| `ChatController`, `ChatDtos`, `ChatErrorCode` | |
| `ChatDevSeeder` | Yalnızca `dev` profilinde ve `chat_message` boşsa (§8) |

Mesajlaşma modülü denetim kaydı yazmaz. Mesajlar kişiseldir ve yönetim işlemi içermez.

---

## §5 Sorgular

### 5.1 Sohbet listesi — tek sorgu

```sql
SELECT c.id, c.type, c.title, c.read_only, c.last_message_at, cm.muted,
       (SELECT count(*) FROM chat_member x WHERE x.conversation_id = c.id) AS member_count,
       o.id AS other_id, o.first_name AS other_first, o.last_name AS other_last, o.status AS other_status,
       lm.id AS lm_id, lm.content AS lm_content, lm.deleted_at AS lm_deleted, lm.sender_id AS lm_sender,
       ls.first_name AS lm_sender_first,
       (SELECT count(*) FROM chat_message m WHERE m.conversation_id = c.id AND m.id > cm.last_read_message_id
          AND m.sender_id <> :uid AND m.deleted_at IS NULL) AS unread_count
FROM chat_member cm
JOIN chat_conversation c ON c.id = cm.conversation_id
LEFT JOIN chat_member om ON c.type = 'DIRECT' AND om.conversation_id = c.id AND om.user_id <> :uid
LEFT JOIN app_user o ON o.id = om.user_id
LEFT JOIN chat_message lm ON lm.id = c.last_message_id
LEFT JOIN app_user ls ON ls.id = lm.sender_id
WHERE cm.user_id = :uid AND (c.type <> 'DIRECT' OR c.last_message_id IS NOT NULL)
ORDER BY c.last_message_at DESC NULLS LAST, c.id DESC
```

Son mesaj yalnızca eklerden oluşuyorsa, özet için bu mesajların ek türleri ikinci bir toplu sorguyla alınır: `message_id IN (...)`.

Birebir sohbette karşı tarafın unvanı ve şubesi aktif yıl atamasından gelir. Bu bilgi liste satırında değil, yalnızca sohbet detayında gösterilir.

### 5.2 Mesaj sayfası

```sql
SELECT m.*, u.first_name, u.last_name, u.status FROM chat_message m JOIN app_user u ON u.id = m.sender_id
WHERE m.conversation_id = :cid AND (:before IS NULL OR m.id < :before)
ORDER BY m.id DESC LIMIT :size + 1
```

Sayfadaki mesajlar için iki toplu sorgu daha çalışır: ekler ve `reply_to_id` alıntıları (gönderen adı, özet, silinmiş mi). N+1 sorgu olmaz.

---

## §6 API

### 6.1 DTO'lar

```ts
type ChatPerson = { id: number; fullName: string; title?: string; branchName?: string; former: boolean };
type ChatAttachment = { fileId: string; name: string; contentType: string; sizeBytes: number; kind: 'IMAGE'|'VIDEO'|'FILE' };
type MessageDto = {
  id: number; conversationId: number; seasonId: number; archived: boolean; clientId: string;
  sender: ChatPerson; content?: string; attachments: ChatAttachment[];
  replyTo?: { id: number; senderName: string; snippet: string; deleted: boolean };
  createdAt: string; deleted: boolean; mine: boolean; canDelete: boolean;
};
type ConversationRow = {
  id: number; type: 'DIRECT'|'GROUP'|'FREE'; title: string;            // DIRECT: karşı tarafın adı
  other?: { id: number; fullName: string; former: boolean };           // yalnızca DIRECT
  memberCount: number; muted: boolean; readOnly: boolean; unreadCount: number;
  lastMessage?: { mine: boolean; senderFirstName: string; snippet: string; createdAt: string; deleted: boolean };
};
type ConversationDetail = ConversationRow & {
  members: (ChatPerson & { role: 'OWNER'|'MEMBER' })[];               // Türkçe alfabetik, sahip en üstte
  canSend: boolean; readOnlyReason?: string; canManage: boolean; canLeave: boolean;
  otherLastReadMessageId?: number;                                     // yalnızca DIRECT
};
type SendMessageRequest = { clientId: string; content?: string; fileIds: string[]; replyToId?: number };
```

`kind` alanı Akış'taki kuralla aynıdır. `canManage` yalnızca `FREE` sohbetin sahibinde, `canLeave` yalnızca `FREE` sohbetin üyelerinde `true` olur.

### 6.2 Uç noktalar

Tüm uçlarda `me.requireMember()` çağrılır. Sohbet id'si alan uçlarda önce üyelik kontrol edilir; üye değilse `CHAT_CONVERSATION_NOT_FOUND` döner.

**Sohbetler**

| Uç nokta | İstek | Yanıt | Kurallar |
|---|---|---|---|
| `GET /api/chat/conversations` | — | `ConversationRow[]` | §5.1. Sayfalama yoktur. |
| `POST /api/chat/direct` | `{ userId }` | 200 `ConversationRow` | §2.1. Sohbet yoksa iki üyeli olarak oluşturulur. |
| `POST /api/chat/groups` | `{ title, memberIds: number[] }` | 201 `ConversationRow` | `type = FREE`, `me = OWNER`. `memberIds`'den kişinin kendi id'si çıkarılır; geriye 2–199 kişi kalmalı. Üyeler `ACTIVE` ve aktif yıla atanmış olmalı. Hatalar: `CHAT_GROUP_TOO_SMALL`, `CHAT_GROUP_TOO_LARGE`, `CHAT_MEMBER_INVALID`. Üyelere `CHAT_CONVERSATION_CHANGED`. |
| `GET /api/chat/conversations/{id}` | — | `ConversationDetail` | |
| `PUT /api/chat/conversations/{id}` | `{ title }` | `ConversationDetail` | `canManage` değilse `CHAT_MANAGE_FORBIDDEN`. Üyelere `CHAT_CONVERSATION_CHANGED`. |
| `POST /api/chat/conversations/{id}/members` | `{ userIds (1–100) }` | `ConversationDetail` | `CHAT_MANAGE_FORBIDDEN`. Üye kuralları aynıdır; toplam en fazla 200 (`CHAT_GROUP_TOO_LARGE`). Zaten üye olanlar atlanır. `last_read_message_id` §2.2'deki gibi ayarlanır. |
| `DELETE /api/chat/conversations/{id}/members/{userId}` | — | 204 | `CHAT_MANAGE_FORBIDDEN`. Sahip kendini bu uçla çıkaramaz; ayrılmak için `leave` kullanır (`CHAT_USE_LEAVE`). Çıkarılan kişiye `CHAT_CONVERSATION_CHANGED`. |
| `POST /api/chat/conversations/{id}/leave` | — | 204 | Yalnızca `FREE` sohbette; değilse `CHAT_LEAVE_NOT_ALLOWED`. Sahip ayrılırsa sahiplik devredilir (§2.1). |
| `POST /api/chat/conversations/{id}/mute` | `{ muted: boolean }` | 204 | Ardından kişiye `BADGES`. |

**Mesajlar**

| Uç nokta | İstek | Yanıt | Kurallar |
|---|---|---|---|
| `GET /api/chat/conversations/{id}/messages?before=&size=50` | — | `CursorResponse<MessageDto>` | Yeniden eskiye sıralı. `size` en fazla 100. |
| `POST /api/chat/conversations/{id}/messages` | `SendMessageRequest` | 201 (yeni) veya 200 (aynı `clientId`) `MessageDto` | §2.3 ve §6.3 doğrulamaları. Dosyalar `attach(fileId, me, CHAT)` ile bağlanır. Ardından WS ve push gönderilir (§2.9). |
| `DELETE /api/chat/messages/{id}` | — | 204 | §2.4. Mesaj yoksa veya kişi üye değilse `CHAT_MESSAGE_NOT_FOUND`. Zaten silinmişse 204 döner. |
| `POST /api/chat/conversations/{id}/read` | `{ lastMessageId }` | 204 | §2.5. `lastMessageId` bu sohbete ait olmalıdır; değilse yok sayılır. |
| `GET /api/chat/conversations/{id}/files/{fileId}?download=false` | — | dosya akışı | Dosya bu sohbetteki silinmemiş bir mesajın eki değilse `FILE_NOT_FOUND`. |

### 6.3 Doğrulama tablosu

Frontend ve backend aynı kuralları uygular:

| Alan | Kural | Mesaj |
|---|---|---|
| `clientId` | Zorunlu, geçerli bir UUID | "Mesaj gönderilemedi. Sayfayı yenileyip tekrar deneyin." (REQUEST_UNREADABLE ile aynı) |
| `content` | Trim edilir, en fazla 4000 karakter | "Mesaj en fazla 4000 karakter olabilir." |
| `content` + `fileIds` | En az biri dolu olmalı | CHAT_EMPTY_MESSAGE |
| `fileIds` | En fazla 10 | CHAT_TOO_MANY_FILES |
| `replyToId` | Aynı sohbette olmalı ve silinmemiş olmalı | CHAT_REPLY_INVALID |
| `title` (FREE) | Trim edilir, 2–100 karakter | "Sohbet adı 2–100 karakter olmalıdır." |
| `memberIds` | §6.2'deki kurallar | CHAT_GROUP_TOO_SMALL / CHAT_GROUP_TOO_LARGE / CHAT_MEMBER_INVALID |

---

## §7 Hata kodları (`ChatErrorCode`)

| code | HTTP | message |
|---|---|---|
| CHAT_CONVERSATION_NOT_FOUND | 404 | Sohbet bulunamadı veya bu sohbete erişiminiz yok. |
| CHAT_MESSAGE_NOT_FOUND | 404 | Mesaj bulunamadı. |
| CHAT_DIRECT_SELF | 400 | Kendinizle sohbet başlatamazsınız. |
| CHAT_USER_INACTIVE | 400 | Bu kişi aktif personel olmadığı için mesaj gönderilemez. |
| CHAT_READ_ONLY | 409 | {0} |
| CHAT_EMPTY_MESSAGE | 400 | Boş mesaj gönderilemez. Bir metin yazın veya dosya ekleyin. |
| CHAT_TOO_MANY_FILES | 400 | Bir mesaja en fazla 10 dosya eklenebilir. |
| CHAT_REPLY_INVALID | 400 | Yanıtlamak istediğiniz mesaj bulunamadı veya silinmiş. |
| CHAT_DELETE_FORBIDDEN | 403 | Yalnızca kendi mesajlarınızı silebilirsiniz. |
| CHAT_GROUP_TOO_SMALL | 400 | Grup sohbeti için en az 2 kişi seçin. Tek kişiyle yazışmak için birebir sohbet açın. |
| CHAT_GROUP_TOO_LARGE | 400 | Bir grup sohbetinde en fazla 200 kişi olabilir. |
| CHAT_MEMBER_INVALID | 400 | Seçilen kişilerden bazıları bu yıl aktif personel değil. |
| CHAT_MANAGE_FORBIDDEN | 403 | Bu sohbeti yalnızca sohbeti oluşturan kişi yönetebilir. |
| CHAT_USE_LEAVE | 400 | Sohbetten çıkmak için "Sohbetten ayrıl" seçeneğini kullanın. |
| CHAT_LEAVE_NOT_ALLOWED | 400 | Bu sohbetten ayrılamazsınız. Grup sohbetlerinin üyeliği grup yönetiminden belirlenir. |

---

## §8 Ekranlar

### 8.1 Yerleşim

Sohbet listesi `/mesajlar`, açık sohbet `/mesajlar/[id]` adresindedir.

**Masaüstü (≥1024px):**

- İki panel yan yana: solda 340px sohbet listesi, sağda sohbet.
- Sohbet seçili değilse sağ panelde `EmptyState` gösterilir: MessageCircle ikonu ve "Bir sohbet seçin veya yeni bir sohbet başlatın."
- `/mesajlar/[id]` sayfası iki paneli birlikte çizer.

**Mobil:**

- `/mesajlar` yalnızca listeyi, `/mesajlar/[id]` yalnızca sohbeti gösterir.
- Sohbet ekranında alt sekme çubuğu **gizlenir**; tam ekran kullanılır.
- Üst çubukta "←" geri butonu bulunur.

### 8.2 Sohbet listesi

**Üst kısım:**

- Başlık: "Mesajlar".
- Sağda "Yeni Sohbet" ikon butonu (SquarePen).
- Altında arama kutusu: "Sohbetlerde ara". İstemci tarafında `fold()` ile `title` üzerinde filtreler.

**Liste öğesi** (64px yüksekliğinde):

- **Avatar:**
  - `DIRECT`: karşı tarafın `UserAvatar`'ı
  - `GROUP`: Users ikonlu yuvarlak, `accent` arka plan
  - `FREE`: sohbet adının baş harfleri, renk sohbet id'sinden türetilir
- **Ad:** Okunmamış mesaj varsa kalın yazılır. Birebir sohbette karşı taraf eski personelse yanında "(Eski personel)" yazar.
- **İkinci satır (özet):**
  - Kendi mesajımsa: "Sen: {özet}"
  - Grup sohbetinde başkasının mesajıysa: "{ad}: {özet}"
  - Silinmişse: "Bu mesaj silindi" (italik)
  - Hiç mesaj yoksa: "Henüz mesaj yok"
- **Sağ üst:** Son mesaj zamanı. Bugünse saat, dünse "Dün", bu hafta içindeyse gün adı, daha eskiyse "12.11.2026".
- **Sağ alt:**
  - Okunmamış sayısı: yuvarlak `primary` rozet; sohbet sessizdeyse gri
  - Sessizdeyse ayrıca BellOff ikonu
  - Salt-okunur sohbette Lock ikonu
- Açık olan sohbet `accent` arka planla vurgulanır.

**Canlı güncelleme:**

- `CHAT_MESSAGE` olayında ilgili satırın özeti, zamanı ve okunmamış sayısı istemcide güncellenir ve satır en üste taşınır. Sohbet listede yoksa liste yeniden çekilir.
- `CHAT_CONVERSATION_CHANGED` olayında liste yeniden çekilir. Kişi artık üye değilse ve o sohbet açıksa ekran `/mesajlar` adresine döner ve toast gösterilir: "Bu sohbete artık erişiminiz yok."

**Boş liste:** "Henüz mesajınız yok." Buton: "Yeni Sohbet".

### 8.3 Yeni Sohbet penceresi

`ResponsiveDialog` içinde iki sekme bulunur:

1. **Kişi**
   - `UserPicker` (tekli). Kişi seçilince `POST /direct` çağrılır ve `/mesajlar/{id}` açılır.
2. **Grup sohbeti**
   - Alanlar: "Sohbet adı\*" ve "Kişiler\*" (çoklu `UserPicker`; en az 2).
   - Buton: "Sohbeti Oluştur".
   - Toast: "Grup sohbeti oluşturuldu."
   - Bilgi notu: "Grup sohbetlerini (zümre, kurul vb.) yönetim otomatik oluşturur; burada kendi küçük gruplarınızı açabilirsiniz."

### 8.4 Sohbet ekranı

**Üst çubuk:**

- Avatar ve ad. Altında gri metin:
  - `DIRECT`: unvan · şube
  - Grup sohbetleri: "{n} üye"
- Ad alanına tıklanınca Sohbet Bilgisi paneli açılır (§8.5).
- Sağda ⋯ menüsü: "Sohbet bilgisi", "Sessize al" / "Sesi aç" ve `FREE` sohbetlerde "Sohbetten ayrıl" (kırmızı).
- Ayrılma onayı: "Sohbetten ayrılacaksınız ve mesajları artık göremeyeceksiniz." Toast: "Sohbetten ayrıldınız."

**Mesaj alanı** (`--background` gri zemin, kaydırılabilir):

- **İlk açılış:** Sohbet en alttan başlar. Yukarı kaydırıldıkça eski mesajlar yüklenir; yüklemede kaydırma konumu korunur.
- **Gün ayırıcısı:** Ortada küçük bir hap: "Bugün", "Dün" veya "12 Kasım 2026".
- **Yıl ayırıcısı:** Ardışık iki mesajın yılı değişince şerit gösterilir: "— 2025-2026 eğitim-öğretim yılı —". Geçmiş yıl mesajları %75 opaklıkla gösterilir.
- **Gruplama:** Aynı kişinin 5 dakika içinde gönderdiği ardışık mesajlar gruplanır.
  - Grup sohbetinde, başkasının grubunun ilk mesajında avatar ve renkli ad gösterilir.
  - Grubun diğer balonlarının köşeleri daha az yuvarlatılır.

**Mesaj balonu:**

| Özellik | Değer |
|---|---|
| Kendi mesajım | Sağda, `primary` zemin, beyaz yazı |
| Başkasının mesajı | Solda, beyaz zemin, `border` |
| En büyük genişlik | %75 (mobilde %85) |
| Köşeler | `rounded-2xl`; kuyruk tarafı düz |

Balonun içeriği yukarıdan aşağı:

1. **Alıntı (varsa):** Sol kenarı 3px çizgili, yarı saydam bir kutu. Kutuda gönderen adı (kalın) ve tek satırlık özet bulunur. Alıntı silinmişse "Silinmiş mesaj" yazar. Kutuya tıklanınca alıntılanan mesaja kaydırılır ve mesaj 1 saniye vurgulanır. Mesaj yüklenmemiş sayfadaysa toast gösterilir: "Mesaj daha eski sayfada."
2. **Ekler:**
   - Görseller ızgara olarak gösterilir (Akış'taki kurallar, en fazla 240px yükseklik). Tıklanınca aynı görüntüleyici açılır; bu bileşen `components/common/ImageViewer` olarak Akış ile ortaktır.
   - Videolar `<video controls>` ile oynatılır.
   - Dosyalar satır olarak gösterilir: ikon, ad, boyut ve İndir.
3. **Metin:** `whitespace-pre-wrap` ve `linkify`.
4. **Saat:** Sağ altta, küçük ve yarı saydam ("14:32").

Ek durumlar:

- **Silinmiş mesaj:** Balon yerine italik gri metin: Ban ikonu ve "Bu mesaj silindi".
- **"Görüldü":** Yalnızca `DIRECT` sohbette, kendi son mesajımın altında küçük gri metinle gösterilir.

**Mesaj menüsü:**

- Masaüstünde balonun üzerine gelince ⋯ butonu, mobilde uzun basma (500 ms) menüyü açar.
- Seçenekler:
  - "Yanıtla"
  - "Kopyala" (yalnızca metinli mesajda)
  - "Sil" (`canDelete` ise, kırmızı)
- Silme onayı: "Mesaj herkesten silinecek. Bu işlem geri alınamaz." Toast yoktur; balon "Bu mesaj silindi" durumuna döner.

**Yeni mesaj gelince:**

- Kullanıcı en alttaysa ekran otomatik kayar.
- Değilse altta bir hap belirir: "↓ Yeni mesajlar ({n})".

**Okundu bildirimi:**

- Sohbet görünürken (`visibilityState`) en alttaki mesajın id'si `POST read` ile gönderilir. Bu istek en fazla 2 saniyede bir yapılır.
- Sohbet ilk açılınca da aynı istek gönderilir.

**Yazma alanı** (en altta sabit, beyaz):

- **Yanıt çubuğu:** Yanıt verilecek mesaj seçildiyse yazma alanının üstünde görünür: "{ad} kişisine yanıt" + özet + ✕.
- **Seçilen ekler:** Yanıt çubuğunun altında küçük resimler ve dosya çipleri olarak gösterilir. Her birinde ilerleme çubuğu ve ✕ bulunur.
- **Satır öğeleri:**
  - Ataç butonu: `FileUploader` (`purpose=CHAT`, `multiple`, `compressImages`) dosya seçicisini açar.
  - Textarea: otomatik büyür, en fazla 5 satır. Placeholder: "Mesaj yazın…"
  - Gönder butonu: yuvarlak, `primary`. İçerik yoksa veya yükleme sürüyorsa pasiftir.
- **Klavye:** Masaüstünde Enter gönderir, Shift+Enter yeni satır açar. Mobilde Enter yeni satır açar.
- **Salt-okunur sohbet:** Yazma alanı yerine gri bir şerit `readOnlyReason` metnini gösterir.

**Hemen gösterme ve hata durumu:**

1. Gönderilen mesaj, istemcinin ürettiği `clientId` (`crypto.randomUUID()`) ile hemen listeye eklenir. Saatin yerinde Clock ikonu görünür.
2. Sunucudan yanıt ya da `CHAT_MESSAGE` olayı gelince mesaj `clientId` ile eşleştirilip gerçek kayıtla değiştirilir.
3. İstek başarısız olursa balonun altında kırmızı metin çıkar: "Gönderilemedi · Tekrar dene". Tekrar denemede aynı `clientId` kullanılır.
4. Hata ayrıca toast ile de gösterilir (`00-mimari.md` §6.4).

### 8.5 Sohbet Bilgisi paneli

Masaüstünde sağdan açılan 360px'lik bir `Sheet`, mobilde tam ekrandır.

**Üst kısım:** Büyük avatar ve ad.

- `FREE` sohbetin sahibi için adın yanında kalem ikonu bulunur; tıklanınca ad düzenlenebilir hale gelir. Toast: "Sohbet adı güncellendi."
- `DIRECT` sohbette unvan, şube ve branş gösterilir.

**Üyeler listesi:**

- Avatar, ad ve unvan; sahibin yanında "Sahip" rozeti.
- `canManage` ise her üyenin yanında ✕ bulunur. Onay: "{ad} sohbetten çıkarılsın mı?" Toast: "{ad} sohbetten çıkarıldı."
- `canManage` ise listenin üstünde "Kişi Ekle" butonu çoklu `UserPicker` açar. Toast: "{n} kişi eklendi."

**`GROUP` sohbette** üye listesinin altında not bulunur: "Bu sohbetin üyeleri '{grup adı}' grubunun üyeleridir. Üyelik okul yönetimi tarafından düzenlenir."

**Alt kısım:** "Sessize al" anahtarı ve `FREE` sohbetlerde "Sohbetten Ayrıl" butonu.

---

## §9 Geliştirme verisi (`ChatDevSeeder`)

`dev` profilinde ve `chat_message` tablosu boşsa çalışır.

| Sohbet | Mesajlar | Özellik |
|---|---|---|
| ogretmen1 ↔ ogretmen2 (`DIRECT`) | 6 mesaj; biri görselli, biri alıntılı yanıt | ogretmen2 son mesajı görmüş |
| "Matematik Zümresi" (`GROUP`, olaydan oluşur) | 4 mesaj | Biri silinmiş |
| "Kuzey Gezi Planı" (`FREE`) | 3 mesaj | Sahip ogretmen2; üyeler my.kuzey ve fotokopi.kuzey; my.kuzey sohbeti sessize almış |

---

## §10 Zorunlu testler

| No | Senaryo |
|---|---|
| T-M1 | **Birebir sohbet:**<br>(a) `POST /direct` iki kez çağrılınca aynı sohbet döner.<br>(b) Bir mesaj gönderilince karşı tarafın `unreadCount` değeri 1 olur.<br>(c) Karşı taraf `read` çağırınca sayı 0 olur ve gönderenin detayında `otherLastReadMessageId` mesajın id'sine eşit olur. |
| T-M2 | **Erişim:**<br>(a) Üye olmayan biri mesajları isteyince `CHAT_CONVERSATION_NOT_FOUND` alır.<br>(b) Çekirdekte gruptan çıkarılan üye, grup sohbetine erişimini kaybeder. |
| T-M3 | **Grup olayları:**<br>(a) Grup oluşturulunca `GROUP` sohbeti çekirdekteki grubun üyeleriyle oluşur.<br>(b) Grup arşivlenince mesaj gönderme `CHAT_READ_ONLY` döner. |
| T-M4 | **Mesaj:**<br>(a) Kişi kendi mesajını silince `deleted=true` olur ve `content` null olur.<br>(b) Başkasının mesajını silmeye çalışınca `CHAT_DELETE_FORBIDDEN` döner.<br>(c) Aynı `clientId` ile ikinci gönderim yeni kayıt oluşturmaz ve aynı id'yi döner. |
| T-M5 | **Serbest grup:** Sahip ayrılınca sahiplik en erken katılan üyeye geçer. |
