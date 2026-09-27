# Roller, kart sahipliği ve hesap ayarları

> Bu dosya önceki geliştirmelerin kaydını da içerir. Güncel oturum, şema ve kayıt politikası için [Güvenlik notları](GUVENLIK.md) geçerlidir.

## Yetki tablosu

| İşlem | ADMIN | USER |
| --- | --- | --- |
| Dashboard | Tüm kartların özeti | Kartlarım'a yönlenir |
| Kart listesi / kart bilgisi | Tüm kartlar | Yalnızca kendisine atanmış kartlar |
| Kart düzenleme / aktif-pasif | Tüm kartlar | Yalnızca kendi kartları |
| Yeni kart / kart silme | Evet | Hayır (`403`) |
| Kartı kullanıcıya atama / atamayı kaldırma | Evet | Hayır (`403`) |
| İstatistikler | Tüm kartlar | Yalnızca kendi kartları |
| Hesap ayarları | Kendi ad/e-postası | Kendi ad/e-postası |

USER menüsünde yalnızca Kartlarım, İstatistikler, Ayarlar ve Çıkış vardır. Doğrudan URL veya HTTP isteğiyle menü kısıtlamaları aşılamaz. Başkasına ait, atanmamış ve bulunmayan kartlar USER için aynı `404` yanıtını verir.

## Veri modeli ve mevcut kayıtlar

`nfc_cards.owner_id` nullable bir foreign key olarak `app_users.id` alanına bağlanır; indekslidir. Sahiplik alanı mevcut veriye uygulanmıştır. Güncel ayar `ddl-auto=validate`; yeni geçişler ayrı migration hesabıyla uygulanır. Atanmamış kartları yalnızca ADMIN görür. Yeni kart oluştururken ya da kartın Düzenle → Kullanıcıya Ata bölümünde aktif bir hesap seçilebilir. Sahip değiştirilince önceki kullanıcı sonraki isteğinde karta ve o kartın istatistiklerine erişemez.

Kullanıcının açık onayıyla mevcut ID 1 (Yusuf Atakan) hesabının rolü ADMIN yapıldı. Mevcut bir kart korunarak atanmamış bırakıldı. Başka hesap oluşturulmadı; kart sahipliği tahminen atanmadı.

## API ve uygulama değişiklikleri

- `SecurityConfig`: admin endpointleri, oluşturma/silme/atama sınırları; okuma ve kendi kartını düzenleme için USER/ADMIN erişimi.
- `NfcCard`, `NfcCardRepository`, `NfcCardService`, `NfcCardController`: sahiplik ilişkisi; backend'de kullanıcı kapsamlı sorgular; kendi kartında düzenleme/durum; admin atama; HTTP/HTTPS URL kontrolü. Güncelleme DTO'su kod veya sahiplik değişikliğine izin vermez.
- `GET /api/admin/users`: kart ataması için admin'e kullanıcı ID, ad, e-posta ve aktiflik listesi. Parola/hash dönmez.
- `PUT /api/cards/{id}/owner`: `{ "ownerId": 123 }`; `null` ile atama kaldırılır. Yalnızca admin.
- `GET /api/statistics`: toplam/aktif/pasif kart, toplam okutma ve kart bazlı okutma listesi. Sunucu, frontend'den kullanıcı ID'si almaz; oturumdaki kimliğin erişebildiği kartları hesaplar.
- `PUT /api/auth/me`: ad, e-posta ve mevcut şifre alır; sadece oturum sahibini günceller. İstemci `id`, `role`, `active` göndererek başka hesabı veya yetkisini değiştiremez. Hatalı şifre/geçersiz alan `400`, kullanılan e-posta `409` döner.
- JWT subject ve Spring Security principal artık değişmez kullanıcı ID'sidir. Kullanıcı ve güncel rol her istekte DB'den bulunur; token içindeki rol yetki kaynağı değildir. E-posta değişse bile mevcut oturum aynı hesaba bağlı kalır. Eski e-posta subject'li tokenlar reddedilir; geçişte yeniden giriş gerekir.
- `AuthService`, profil DTO'su ve `ApiExceptionHandler`: profil doğrulama, benzersiz e-posta, güvenli hata yanıtı.
- `AdminRoute`, `permissions`, `App`, `Sidebar`, `Login`: role göre menüler/route koruması ve giriş sonrası başlangıç sayfası.
- `Cards`, `NfcCardRow`, `NewCard`, `EditCard`, `OwnerSelect`: role uygun işlemler, kart sahibi sütunu ve admin atama formu.
- `Statistics`, `Dashboard`, `CardStatistics`: örnek sayılar yerine backend toplamları ve kart bazlı liste.
- `Settings`, `auth/session`: profil formu, mevcut şifre doğrulama, Header ve merkezi oturumun kayıttan sonra güncellenmesi. Geç gelen profil yanıtı logout sonrasında oturumu yeniden açamaz.

Ad ve e-posta güncellemesi uygulanmıştır; şifre değiştirme ve diğer kullanıcıların hesaplarını yönetme ekranı bu kapsamda eklenmemiştir.

## İstatistiklerin kapsamı

Veritabanındaki `nfc_cards.scans` toplamları kullanılır. Tarihli okutma olayları ve `/r/{code}` redirect henüz yoktur; bu nedenle günlük/aylık gerçek ölçüm sunulmaz ve örnek sayılar gösterilmez. Sonraki adım redirect ile okutma kaydı üretimini tamamlamaktır.

## Doğrulama

- Frontend lint/build ve 18 Node testi: oturum, rolün başlangıç sayfası, profil güncellemesi ve geç gelen yanıtlar.
- Backend 27 MockMvc testi: gerçek Security/Controller/Service/JWT/BCrypt ile iki USER + bir ADMIN; başka kullanıcının kartını okuma/düzenleme/durum değiştirme engeli, sahiplik transferi, oluşturma/silme sınırları, istatistik kapsamı, profil ve rol yükseltme girişimleri. Repository test verisi kullanıldı; müşteri kayıtları test için değiştirilmedi.
- Mevcut MySQL ve çalışan backend: onaylanan ADMIN rolü, bir kartın korunması/atanmamış kalması; `/me`, `/admin/users`, `/cards`, `/statistics` `200`.
- Tarayıcıda gerçek frontend, ayrı bellek içi test API'si: USER'ın üç menüsü, sadece kendi kartı, admin route engeli, düzenleme/aktif-pasif, kendi istatistikleri, profil kaydı/yenileme/Header; ADMIN menüsü, tüm toplamlar ve kullanıcıya kart atama doğrulandı.

Tarayıcı testleri gerçek müşteri şifresini kullanmadı. Canlı veritabanındaki tek değişiklik onaylanan rol güncellemesi ve Hibernate'in eklediği sahiplik şemasıdır; gerçek kart üzerinde test mutasyonu yapılmadı.

## Müşteri ekranı eklemesi

- `src/pages/Customers.jsx`: müşteri oluşturma, müşteri listesi/kart sayısı, seçili müşterinin kartları ve aynı ekrandan atanmamış kart bağlama. Şifre tekrarı, kaydetme/yüklenme/hata durumları ve başarılı kayıtta şifre alanlarını temizleme.
- `App.jsx`, `Sidebar.jsx`, `App.css`: admin'e özel `/customers` route'u, Müşteriler menüsü ve responsive form/liste yerleşimi.
- `NewCard.jsx`: müşteri ekranından gelen `ownerId` parametresiyle kart sahibini önceden seçme; backend sahibi ayrıca doğrular.
- `AdminUserController`, `AuthService`, `AppUserRepository`: `GET/POST /api/admin/customers`, sadece USER hesaplarının listelenmesi, yeni aktif USER kaydı, BCrypt, ad/e-posta/şifre doğrulaması ve tekrarlanan e-postada `409`. Müşteri oluşturma yalnızca admin üzerinden yapılır; public register güvenlik çalışmasıyla kapatılmıştır. Müşteri oluşturma yanıtı token veya parola içermez.
- Yeni veritabanı tablosu/kolonu gerekmedi. Mevcut kullanıcı ve kart sahipliği modeli kullanıldı.
- Backend testleri 34'e çıktı (7 yeni senaryo); frontend mevcut 18 test, lint ve build başarılı. Yeni senaryolar: admin kısıtı, müşteri listesinin kapsamı, sabit USER rolü, BCrypt saklama, e-posta çakışması ve yarış durumu, geçersiz alanlar, yeni müşteri login → admin kart atama → yalnızca kendi kart/istatistiklerine erişim.
- Tarayıcıda ayrı test API'siyle müşteri oluşturma, admin oturumunun korunması, aynı ekrandan kart bağlama, müşteri seçili yeni kart formu ve yeni müşterinin yalnızca atanmış kartı görmesi doğrulandı. Canlı backend'de müşteri listesi GET ile kontrol edildi. Gerçek veritabanına örnek müşteri eklenmedi veya kart sahipliği değiştirilmedi.
