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
| D-30 | Plan metninde ayrıntısı yok; onay bekliyor. |
| D-31 | Plan metninde ayrıntısı yok; onay bekliyor. |
| D-32 | Plan metninde ayrıntısı yok; onay bekliyor. |
| D-33 | Önemli Duyuru'nun okuyanlar ve okumayanlar listesini yalnızca gönderinin yazarı görebilir. |
| D-34 | Plan metninde ayrıntısı yok; onay bekliyor. |

## Fotokopi

| No | Karar |
|---|---|
| D-13 | Talep formunun alanları:<br>• Dosya (ya da "basılı orijinali getireceğim" seçeneği)<br>• Açıklama<br>• Sayfa sayısı (PDF'ten otomatik okunur, değiştirilebilir)<br>• Kopya adedi<br>• Renk: Siyah-Beyaz / Renkli<br>• Baskı yüzü: Tek / Çift<br>• Kâğıt: A4 / A3<br>• Gizli (sınav evrakı)<br>• İstenen teslim tarihi ve saati<br>• Not |
| D-14 | Talep durumları: Onay Bekliyor → Onaylandı veya Reddedildi (gerekçe zorunlu) → Hazırlanıyor → Hazır → Teslim Edildi.<br>• Talep sahibi, hazırlık başlamadan önce (Onay Bekliyor veya Onaylandı durumunda) iptal edebilir; onaylandıktan sonraki iptalde fotokopi sorumluları bilgilendirilir. (D-35, kullanıcı onayı: 2026-11-15)<br>• "Düzeltme iste" adımı yoktur; reddedilen talep "Kopyala ve yeniden gönder" ile tekrar açılır.<br>• İkinci onay, zaman aşımı ve otomatik üst seviyeye yükseltme yoktur. |
| D-15 | Onaylayıcı, bir tarih aralığı için kendine vekil tanımlayabilir. Bu aralıkta gelen talepler vekile gider. |
| D-16 | Tek bir rapor ekranı vardır: ay seçilir, kişi bazında siyah-beyaz ve renkli toplam baskı listelenir ve CSV olarak indirilebilir. |
| D-17 | Teslim edilen taleplerin dosyaları `application.yml`'de belirlenen gün sayısı kadar saklanır, sonra otomatik silinir; talep kaydı kalır. Gizli talebin dosyasını yalnızca talep sahibi, onaylayıcı ve fotokopi sorumlusu görür. |
| D-35 | Talep sahibi, hazırlık başlamadan önce (Onay Bekliyor veya Onaylandı durumunda) talebini iptal edebilir. Onaylandı durumundan iptalde şubenin fotokopi sorumlularına bildirim gider. |
| D-36 | Talebin kuyruk şubesi, talep anında talep sahibinin aktif yıldaki şubesidir. Talep formunda şube seçimi yoktur. |
| D-37 | Plan metninde ayrıntısı yok; onay bekliyor. |
| D-38 | Plan metninde ayrıntısı yok; onay bekliyor. |
| D-39 | Aylık rapor, toplam baskıyı `total_pages` (sayfa sayısı × kopya adedi) değeriyle hesaplar. |
| D-40 | Fotokopi sorumlusu, Onaylandı durumundaki talebi "Hazırlanıyor" adımından geçirmeden doğrudan Hazır yapabilir; hazırlık adımı atlanabilir. |
| D-41 | Dosya silme işi her gece 04:00'te çalışır. Durumu Teslim Edildi, Reddedildi veya İptal Edildi olan ve kapanışı saklama süresinden eski taleplerin dosyaları silinir; talep kaydı kalır ve arayüzde "Dosya saklama süresi dolduğu için silindi" yazar. |
| D-42 | Yönetici tekil fotokopi talebini göremez; yalnızca aylık raporu görür. |

## Mesajlaşma

| No | Karar |
|---|---|
| D-18 | Üç tür sohbet vardır: birebir sohbet, grupların otomatik sohbeti ve herkesin seçtiği kişilerle açabileceği serbest grup sohbeti. |
| D-19 | Metin, görsel ve dosya gönderilebilir; mesaja alıntılı yanıt verilebilir.<br>• Kişi kendi mesajını herkesten silebilir; yerinde "silindi" izi kalır.<br>• Birebir sohbette "Görüldü" bilgisi gösterilir, grup sohbetinde gösterilmez.<br>• Düzenleme, iletme, yıldızlama, sabitleme, "yazıyor…" göstergesi ve son görülme yoktur. |
| D-43 | Her üye her sohbeti sessize alabilir. Sessizdeki sohbet için Web Push gönderilmez ve sohbet rozetinde sayılmaz; listede okunmamış sayısı gri gösterilir. |
| D-44 | Sohbete yeni katılan üye sohbetin geçmiş mesajlarını görür. Katılırken okundu işareti o anki son mesaja eşitlenir; geçmiş mesajlar okunmamış sayılmaz. |
| D-45 | Kişi yalnızca kendi mesajını ve yalnızca aktif yıla ait ise silebilir; süre sınırı yoktur. Silinen mesajın içeriği ve ekleri kaldırılır, listede "Bu mesaj silindi" izi kalır ve silinmiş mesaja yanıt verilemez. |
| D-46 | Serbest grup sohbetinin kurucusu sahip olur. Sahip ayrılır, çıkarılır ya da pasifleşirse sahiplik kalan üyelerden en erken katılana geçer (eşitlikte küçük kullanıcı numarası). Hiç üye kalmazsa sohbet salt okunur olur ve kimsenin listesinde görünmez. |
| D-47 | Birebir sohbette karşı taraf kişinin kendisi olamaz ve aktif personel olmalıdır; karşı tarafın yıl ataması aranmaz. Henüz mesaj içermeyen birebir sohbetler listede görünmez. |
| D-48 | Plan metninde ayrıntısı yok; onay bekliyor. |

## Dokümanlar (DYS)

| No | Karar |
|---|---|
| D-20 | Yetkiler alan düzeyinde ve üç seviyedir:<br>• Görüntüle<br>• Düzenle: yükleme, klasör açma, yeniden adlandırma, silme<br>• Yönet: izin verme<br><br>Diğer kurallar:<br>• Klasöre veya dosyaya özel izin yoktur.<br>• Sürüm geçmişi yoktur. Aynı adla dosya yüklenirse kullanıcıya "üzerine yaz / farklı adla kaydet" diye sorulur.<br>• 30 günlük çöp kutusu vardır.<br>• PDF ve görseller tarayıcıda önizlenir; diğer dosyalar indirilir.<br>• Alanı Yönetici veya "Doküman alanı açabilir" işareti olan kişi açar. |
| D-49 | Doküman taşıma için Düzenle yetkisi gerekir. Hedef aynı alandaki bir klasör ya da ana dizin olmalıdır; klasör kendisine veya alt klasörüne taşınamaz. Hedefte aynı ad varsa ya da derinlik 10'u aşarsa taşıma yapılamaz. |
| D-50 | Doküman alanını silmek için Yönet yetkisi gerekir ve alanda hiç klasör ya da dosya (çöp kutusu dahil) bulunmamalıdır. Alan sahipliği yalnızca kişi pasifleştirilince devralan kişiye geçer; elle devretme yoktur. |
| D-51 | Doküman alanı bir kişiye yetkiyle paylaşıldığında (yeni yetki satırı) o kişiye bildirim gider; seviye değişikliğinde gönderilmez. Pasifleştirmede alanın sahipliği devredilen kişiye de bildirim gider. Grup, şube ve Tüm Kurum yetkilerinde bildirim yoktur. |
| D-52 | Silinen dosya ve klasörler çöp kutusuna taşınır; klasörle birlikte altındaki öğeler de gider ve klasör geri yüklenince birlikte döner. Üst klasörü artık yoksa öğe alanın ana dizinine, aynı ad varsa yeni adla yüklenir. Kalıcı silme Yönet yetkisi gerektirir; çöp kutusundaki öğeler 30 gün sonra gece işiyle kalıcı silinir. |
| D-53 | Plan metninde ayrıntısı yok; onay bekliyor. |
| D-54 | Plan metninde ayrıntısı yok; onay bekliyor. |

## Takvim

| No | Karar |
|---|---|
| D-21 | Takvim türleri: tek Kurum takvimi, her şube için otomatik Şube takvimi, herkese bir varsayılan (en fazla 9 ek) Kişisel takvim, her grup için otomatik Grup takvimi ve Yöneticinin tanımladığı Salon/Laboratuvar takvimi. Kurum ve şube takvimlerini herkes görür, Yönetici düzenler. Kişisel takvim kişi veya grupla paylaşılabilir; grup takvimini grubun üyeleri ve Yöneticiler görür. |
| D-22 | Davet, katılım yanıtı, hatırlatıcı ve tekrarlayan kayıt **yoktur**; takvim bilgi amaçlıdır. Kayıt alanları: başlık, tüm gün ya da saatli tarih, konum, açıklama ve en fazla 1 dosya. |
| D-27 | Kişisel takvimi yalnızca sahibi paylaşabilir; kişi (aktif yıla atanmış) ya da arşivlenmemiş grupla paylaşılır, kendisiyle paylaşım yapılamaz. Diğer takvim türleri paylaşılamaz. |
| D-28 | Plan metninde ayrıntısı yok; onay bekliyor. |
| D-29 | Salon ve laboratuvar takvimlerini Yönetici tanımlar; rezervasyonu herkes yapabilir. Salonun şubesi isteğe bağlıdır ve yalnızca etiket olarak gösterilir. |
| D-55 | Kişisel takvim paylaşımının iki seviyesi vardır: Görüntüleme ve Düzenleme. Aynı özneye yeniden paylaşımda seviye güncellenir; bir takvimde en fazla 50 paylaşım olabilir. |
| D-56 | Takvim kayıtları eğitim-öğretim yılına bağlı değildir (`season_id` yoktur); böylece yaz döneminde yeni yılın takvimi hazırlanabilir. Geçmiş tarihli kayıtlar da yetkiye göre düzenlenebilir. |
| D-57 | Her Yönetici kurum takvimini ve tüm şube takvimlerini düzenleyebilir; şube ayrımı yoktur. |
| D-58 | Aynı salon takviminde iki rezervasyonun zaman aralığı kesişemez; uç uca gelen rezervasyonlar serbesttir. Çakışma hem serviste hem veritabanı kısıtıyla engellenir. |
| D-59 | Takvim görünürlük seçimleri tarayıcıda saklanır. Varsayılan olarak Kurum, kendi şube, kişisel, paylaşılan ve üyesi olunan grup takvimleri görünür; diğer şubeler, diğer gruplar ve salonlar gizlidir. |
| D-60 | Takvim modülünde tek bildirim vardır: kişisel takvim bir kişiyle paylaşıldığında o kişiye bildirim gider. Kayıt değişikliği bildirimi, WebSocket olayı ve rozet yoktur. |
| D-61 | Takvim kaydı fiziksel olarak silinir ve dosyası da silinir; çöp kutusu yoktur. Silen kişi kaydı ekleyen değilse denetim kaydı yazılır. |

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
| D-62 | Fotokopi vekâlet süresi sınırı `application.yml` içinde `app.print.max-delegation-days` ayarıdır (varsayılan 90). Süre, başlangıç ve bitiş günü dahil gün sayısıdır. |
| D-63 | Talep sahibi pasifleştirilince devralan kişi talep sahibinin kendisi çıkıp talep otomatik onaylanırsa `approver_id` NULL kalır (§2.2 ile tutarlı). |
| D-64 | Takvim açıklaması en fazla 500 karakterdir; aşılırsa mesaj "Açıklama en fazla 500 karakter olabilir." olur (06 §5.3 tablosuna eklenmemiş, kodda bu metinle durur). |
| D-65 | Arayüz LinkedIn benzeri tasarlanır (kullanıcı isteği; `00-mimari.md` §12.1 ve §12.2'nin yerine geçer):<br>• Sol menü kalktı; üstte sabit bir gezinti çubuğu vardır (logo, yıl çipi, Akış / Mesajlar / Fotokopi / Dokümanlar / Takvim / Yönetim, Bildirimler, "Ben" menüsü). Mobilde alt sekme çubuğu aynen durur.<br>• Renkler: arka plan `#F4F2EE`, birincil `#0A66C2`, kenarlık `#E0DFDC`; kart yarıçapı 8px; butonlar hap biçimlidir.<br>• Akış iki sütundur (D-66'ya bakın): ortada akış, sağda özet sütunu.<br>• Sayfa başlıkları beyaz karttır. |
| D-66 | Akış sayfasının sol sütunu kalktı (kullanıcı isteği); akış alanı genişledi. Sağ sütun `lg` ve üzeri ekranlarda görünür ve yukarıdan aşağı şunları içerir:<br>• Profil kartı ve Gruplarım<br>• Okumanız Gereken Duyurular<br>• Onay Bekleyenler (fotokopi; yalnızca bekleyen varsa görünür, en fazla 5)<br>• Takvimde Yaklaşanlar (önümüzdeki 7 gün, en fazla 5)<br>• Aktif Fotokopi Taleplerim (onay bekliyor, onaylandı, hazırlanıyor, hazır; en fazla 5)<br>• Son Eklenen Dokümanlar (görüntüleme yetkisi olunan alanlardaki en yeni 5 dosya; `GET /api/dms/recent`) |
| D-67 | PWA renkleri D-65 ile hizalandı (`00-mimari.md` §13 tablosundaki renklerin yerine geçer): manifest `background_color` `#F4F2EE`, `theme_color` ve `viewport.themeColor` `#FFFFFF` (üst çubuk beyazdır). Kök `viewport` ayarına `viewportFit: "cover"` eklendi; böylece iPhone'da `env(safe-area-inset-*)` değerleri çalışır. Service worker bildirim linkini yalnızca `/` ile başlayan göreli yol ise açar, aksi halde `/akis` açılır. |
| D-68 | Gizli talep (sınav evrakı) onaylayıcı karar vermeden iptal edilirse fotokopi sorumlusu o talebi ve dosyasını göremez; yalnızca hiç onaylanmamış olmayan (onaylanmış veya onaylayıcısız otomatik onaylanmış) iptal edilmiş talepleri görür. |
| D-69 | Yıl aktifleştirme ön izlemesi, yeni yılda hiç Yönetici yoksa ya da üyesi olan bir aktif şubede Fotokopi Sorumlusu yoksa 'Uyarı:' satırı gösterir; aktifleştirmeyi engellemez. |
