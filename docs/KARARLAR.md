# Onaylı Kararlar

Bu dosya, kullanıcıyla birlikte alınan iş kararlarını tutar. Plan dokümanlarıyla çelişki olursa **bu dosya geçerlidir**. Yeni karar alındığında en alta `D-xx` numarasıyla eklenir.

## Genel

| No | Karar |
|---|---|
| D-01 | Sistem tek kurum içindir ve şu an 2 şubesi vardır. Şubeler yönetim ekranından eklenip çıkarılabilir. |
| D-02 | Giriş e-posta ve şifreyle yapılır. Hesabı Sistem Yöneticisi açar; kişi şifresini e-postayla gelen bağlantıdan kendisi belirler. |
| D-03 | Sistem okulun kendi sunucusunda, Docker Compose ile tek sunucu olarak çalışır. |
| D-04 | Okulun mevcut SMTP hesabı yalnızca kullanılır, yapılandırmasına müdahale edilmez. E-posta **yalnızca** iki iş için gönderilir: hesap kurulumu (şifre belirleme) ve şifre sıfırlama. E-posta bildirimi yoktur. |
| D-05 | Ayrı bir mobil uygulama yazılmaz. Telefona tam uyumlu ve ana ekrana eklenebilen bir web uygulaması (PWA) yapılır. Bildirimler uygulama içinde ve Web Push ile gelir. iPhone'da push alabilmek için uygulamanın ana ekrana eklenmesi gerekir. |
| D-06 | Sabit roller vardır: Sistem Yöneticisi (kullanıcıya ait işaret), Yönetici, Fotokopi Sorumlusu, Personel. Ek olarak "Doküman alanı açabilir" işareti vardır. Son üçü ve bu işaret her eğitim-öğretim yılı için ayrı tutulur. Unvan yalnızca bir etikettir, yetki vermez. Yetki matrisi veya kapsam motoru kurulmaz. |
| D-07 | Organizasyon şeması veya ast-üst hiyerarşisi tutulmaz. Her personelin yıllık kaydında doğrudan bir "Fotokopi onaylayıcısı" seçilir. Bu alan boşsa talep onaysız olarak kuyruğa düşer. |
| D-08 | Eğitim-öğretim yılı kuralları:<br>• Aynı anda yalnızca bir yıl aktif olabilir.<br>• Kullanıcı ancak aktif yıla atanmışsa giriş yapabilir; Sistem Yöneticisi bu kuraldan muaftır.<br>• Geçmiş yılların verisi görüntülenebilir ama değiştirilemez.<br>• Her kayıt, oluşturulduğu andaki aktif yıla bağlanır; yılın tarihleri yalnızca bilgi amaçlıdır.<br>• Yıl devri adımları: yeni yıl oluşturulur → önceki yılın personeli işaretli bir listeden kopyalanır → düzenlenir → yıl aktifleştirilir.<br>• Aktifleştirilen yeni yıl, eski yılı otomatik olarak kapatır. Eski yılın açık fotokopi talepleri iptal edilir.<br>• Doküman modülü (DYS) yıldan bağımsız çalışır. |
| D-09 | Gruplar:<br>• Tek tiptir ve yıldan bağımsız, kalıcıdır.<br>• Grubu Yönetici veya Sistem Yöneticisi oluşturur ve üyelerini belirler.<br>• Açık/özel/gizli ayrımı ve kurala göre otomatik üyelik yoktur.<br>• Her grubun otomatik bir grup sohbeti olur.<br>• "Tüm Kurum" ve "Şube" birer grup değil, hazır hedef seçenekleridir. |

## Akış

| No | Karar |
|---|---|
| D-10 | Personel, üyesi olduğu gruplara ve seçtiği kişilere paylaşım yapabilir. Yönetici bunlara ek olarak Tüm Kurum'a ve şubelere paylaşabilir, Önemli Duyuru yayınlayabilir. |
| D-11 | İki gönderi tipi vardır: Normal ve Önemli Duyuru. Önemli Duyuru'da "Okudum" onayı, okumayanlar listesi ve tek tıkla hatırlatma vardır. Anket yoktur. |
| D-12 | Gönderilere tek seviye yorum yapılır ve tek bir "Beğen" tepkisi verilebilir. Metin düz yazıdır, içindeki bağlantılar tıklanabilir. Görsel ve dosya eklenebilir. Sabitlemeyi yalnızca Yönetici yapar. Zengin metin editörü, bağlantı önizlemesi, @bahsetme ve kaydetme yoktur. |

## Fotokopi

| No | Karar |
|---|---|
| D-13 | Talep formunun alanları:<br>• Dosya (ya da "basılı orijinali getireceğim" seçeneği)<br>• Açıklama<br>• Sayfa sayısı (PDF'ten otomatik okunur, değiştirilebilir)<br>• Kopya adedi<br>• Renk: Siyah-Beyaz / Renkli<br>• Baskı yüzü: Tek / Çift<br>• Kâğıt: A4 / A3<br>• Gizli (sınav evrakı)<br>• İstenen teslim tarihi ve saati<br>• Not |
| D-14 | Talep durumları: Onay Bekliyor → Onaylandı veya Reddedildi (gerekçe zorunlu) → Hazırlanıyor → Hazır → Teslim Edildi.<br>• Talep sahibi, talep onaylanmadan önce iptal edebilir.<br>• "Düzeltme iste" adımı yoktur; reddedilen talep "Kopyala ve yeniden gönder" ile tekrar açılır.<br>• İkinci onay, zaman aşımı ve otomatik üst seviyeye yükseltme yoktur. |
| D-15 | Onaylayıcı, bir tarih aralığı için kendine vekil tanımlayabilir. Bu aralıkta gelen talepler vekile gider. |
| D-16 | Tek bir rapor ekranı vardır: ay seçilir, kişi bazında siyah-beyaz ve renkli toplam baskı listelenir ve CSV olarak indirilebilir. |
| D-17 | Teslim edilen taleplerin dosyaları `application.yml`'de belirlenen gün sayısı kadar saklanır, sonra otomatik silinir; talep kaydı kalır. Gizli talebin dosyasını yalnızca talep sahibi, onaylayıcı ve fotokopi sorumlusu görür. |

## Mesajlaşma

| No | Karar |
|---|---|
| D-18 | Üç tür sohbet vardır: birebir sohbet, grupların otomatik sohbeti ve herkesin seçtiği kişilerle açabileceği serbest grup sohbeti. |
| D-19 | Metin, görsel ve dosya gönderilebilir; mesaja alıntılı yanıt verilebilir.<br>• Kişi kendi mesajını herkesten silebilir; yerinde "silindi" izi kalır.<br>• Birebir sohbette "Görüldü" bilgisi gösterilir, grup sohbetinde gösterilmez.<br>• Düzenleme, iletme, yıldızlama, sabitleme, "yazıyor…" göstergesi ve son görülme yoktur. |

## Dokümanlar (DYS)

| No | Karar |
|---|---|
| D-20 | Yetkiler alan düzeyinde ve üç seviyedir:<br>• Görüntüle<br>• Düzenle: yükleme, klasör açma, yeniden adlandırma, silme<br>• Yönet: izin verme<br><br>Diğer kurallar:<br>• Klasöre veya dosyaya özel izin yoktur.<br>• Sürüm geçmişi yoktur. Aynı adla dosya yüklenirse kullanıcıya "üzerine yaz / farklı adla kaydet" diye sorulur.<br>• 30 günlük çöp kutusu vardır.<br>• PDF ve görseller tarayıcıda önizlenir; diğer dosyalar indirilir.<br>• Alanı Yönetici veya "Doküman alanı açabilir" işareti olan kişi açar. |

## Takvim — AÇIK (06-takvim planı yazılmadan önce netleşecek)

| No | Karar |
|---|---|
| D-21 | Takvim paylaşımı, ek takvimler, grup takvimi ve salon/laboratuvar takvimi **olacak**. Kurum ve şube takvimlerini herkes görür, Yönetici düzenler. Ayrıntılar kullanıcıya soruldu; cevap gelince güncellenecek. |
| D-22 | Davet ve katılım yanıtı (etkinlik) **yoktur**; takvim bilgi amaçlıdır. Kayıt alanlarının kapsamı kullanıcıya soruldu. |

## Teknik çerçeve (onaylı)

| No | Karar |
|---|---|
| D-23 | Altyapı:<br>• Dosyalar sunucu diskinde tutulur (MinIO yok).<br>• Virüs taraması yoktur; dosya tipi ve boyut kontrolü yapılır.<br>• Oturum güvenli çerezle tutulur (JWT yok).<br>• WebSocket yalnızca sunucudan istemciye olay iletmek için kullanılır.<br>• Sistem geneli arama yoktur; her modül kendi içinde arama yapar.<br>• Denetim kaydı yalnızca kritik işlemler için tutulur.<br>• KVKK aydınlatma metni ilk girişte gösterilir. |
| D-24 | Arayüz Türkçe, kod İngilizcedir. Plan, repoya konan Markdown dosyalarından oluşur. |
| D-25 | Sistem Yöneticisi CSV dosyasıyla toplu personel ekleyebilir. Bu, ilk kurulum içindir ve kullanıcının onayı bekleniyor. |

## Geliştirme ortamı

| No | Karar |
|---|---|
| D-26 | Geliştirmede yeni bir PostgreSQL konteyneri açılmaz. Yerelde çalışan mevcut `postgres` konteyneri (5432) kullanılır; kullanıcı ve şifre `genixo`/`genixo`, veritabanı `iletisim`. `deploy/docker-compose.dev.yml` yalnızca `mailpit` içerir. Backend dev'de `POSTGRES_USER=genixo POSTGRES_PASSWORD=genixo` ortam değişkenleriyle çalışır. |
