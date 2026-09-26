# Proje devri ve login düzeltmesi — 26 Eylül 2026

## Doğrulanan mevcut durum

- React 19.2.8, React Router 7.18.4, Vite 8.3.0, Butterup 2.0.1; Java 21, Spring Boot 4.1.1, JJWT 0.13.0.
- MySQL sunucusu 8.4.10; salt okunur incelemede `app_users` ve `nfc_cards` tabloları bulundu. `Cafe.java`, `cafes` ve `nfc_scans` bulunmadı.
- Kart CRUD ve durum değiştirme backend'de mevcut. Kod backend'de altı karakter üretiliyor; DB unique constraint var. Çakışma kontrolü mevcut, eşzamanlı çakışmada yeniden deneme henüz yok.
- `/r/{code}` için sadece güvenlik izni vardı; redirect Controller/Service uygulaması yok.
- Dashboard sayıları örnek, Statistics ve Settings yer tutucu. Kullanıcı-kart sahipliği ve USER/ADMIN bazında endpoint ayrımı yok.
- Girişte Git çalışma ağacında `App.jsx`, `ProtectedLayout.jsx`, `App.css`, `Login.jsx`, `api.js` değişiklikleri vardı; reset veya geri alma yapılmadı. Commit/push yapılmadı.

## Hatanın nedenleri

1. Güncel `Login.jsx`, `navigate()` çağırmasına rağmen `useNavigate()` sonucunu tanımlamıyordu. Başarılı yanıt ve localStorage yazımından sonra JavaScript `ReferenceError` oluşuyordu; lint bunu doğruladı.
2. `ProtectedLayout`, `/api/auth/me` çağırıyordu ama backend'de bu endpoint ve `CurrentUserResponse` yoktu. Dolayısıyla sadece frontend yönlendirmesini düzeltmek oturum doğrulamasını tamamlamıyordu.
3. Eski Git sürümündeki API istemcisi her `401` ve `403` için tam sayfa login yönlendirmesi yapıyordu. Çalışma ağacında bu davranış daha önce kaldırılmıştı; geri getirilmedi. Yeni sürüm yalnızca mevcut oturumun `401` yanıtını merkezi duruma bildirir.
4. JWT filtresinin bağımsız servlet filtresi ve güvenlik zinciri filtresi olarak iki kez kaydedilebilmesi MVC testinde doğrulandı: JWT imzası doğru olduğu halde önceki filtrede oluşturulan context güvenlik zincirinde temizleniyor ve `401` dönüyordu. Bağımsız kayıt kapatıldı. Bunun kullanıcının önceki canlı oturumlarında gerçekleştiğine dair ayrıca bir kayıt yok; testte yeniden üretildi.
5. Butterup `duration` parametresini okumuyor; kullanılan sürüm `options.toastLife` bekliyor. Bildirimler istenen 3 saniye yerine varsayılan 35 saniye kalıyordu. Bu, ayrı giriş denemelerinin hata ve başarı bildirimlerinin aynı anda görünmesini uzatıyordu. Önceki oturumdaki çift bildirimin tek nedeni olduğu iddia edilmiyor.

StrictMode ilk durumda import edilmiş fakat kullanılmıyordu; o andaki yönlendirme hatasının nedeni değildi. Yeniden etkinleştirildi ve iki kez başlayan effect'lerin eski yanıtlarının oturumu bozmadığı test edildi.

## Değişen dosyalar

| Dosya | Değişiklik |
| --- | --- |
| `src/auth/session.js`, `src/auth/useAuth.js` | Merkezi oturum durumu, backend doğrulaması, login/logout, sekmeler arası senkronizasyon ve geç gelen yanıtların elenmesi. Context yerine React `useSyncExternalStore` ile küçük bir ortak oturum deposu kullanıldı. |
| `src/App.jsx`, `src/main.jsx` | Merkezi oturum başlangıcı ve StrictMode. Mevcut route ağacı korundu. |
| `src/pages/Login.jsx` | Ayrı localStorage/navigate akışı kaldırıldı; merkezi doğrulamadan sonra `Navigate` ile Dashboard'a geçilir. Başarı ve hata aynı try/catch içinde karıştırılmaz. |
| `src/components/ProtectedLayout.jsx`, `ProtectedRoute.jsx`, `AuthStatus.jsx` | Tek oturum kaynağına bağlı koruma ve sunucu hatasında yeniden deneme ekranı. Kullanılmayan eski ProtectedRoute da ayrı token kontrolü yapmaz. |
| `src/components/Header.jsx`, `Sidebar.jsx`, `src/css/App.css` | `/me` kullanıcısının adı, altta logout, mevcut tasarımla mobil panel yerleşimi. |
| `src/services/api.js` | Ortamdan API adresi, Bearer token, istek zaman aşımı, yönlendirme yapmadan merkezi `401` yönetimi; `403` logout değildir. |
| `src/pages/NewCard.jsx`, `EditCard.jsx`, `Cards.jsx` | Oluşturma/düzenleme dahil tüm kart istekleri API istemcisini kullanır. Kullanılmayan frontend kod üreticisi kaldırıldı. Sayfa terk edilince yükleme istekleri iptal edilir; StrictMode çift hata bildirimi önlenir. |
| `src/utils/toast.js` | Butterup'un desteklediği global süre ve adet ayarları. |
| `AuthController.java`, `AuthService.java`, `CurrentUserResponse.java` | Korumalı `/api/auth/me`, güncel DB kullanıcısı, eksik login alanları için `400`, locale bağımsız email normalleştirme. |
| `SecurityConfig.java`, `JwtAuthenticationFilter.java` | Sadece POST login/register ve GET redirect public; yönetim JWT ile korunur. `401/403` ayrımı, tek JWT filtre kaydı, tek seferde JWT ayrıştırma, aktif kullanıcı ve rolün DB'den kontrolü. Hata dispatch'i gerçek HTTP hata durumunu korur. |
| `JwtService.java`, `WebConfig.java`, `application.properties` | JWT anahtarı/DB kimlik bilgileri/CORS ortam yapılandırmasına taşındı; sabit anahtar kaldırıldı. JWT süresi 24 saat olarak korundu. |
| `.gitignore`, `.env.example`, `backend/.env.example` | Sırlar Git dışında; hassas bilgi içermeyen kurulum örnekleri. Yerelde `backend/.env` oluşturuldu, JWT anahtarı yenilendi ve dosya izinleri `0600` yapıldı. |
| `tests/auth.test.js`, `package.json`, `eslint.config.js` | Node'un yerleşik test aracıyla 14 oturum testi ve test komutu; yeni npm bağımlılığı yok. |
| `backend/src/test/java/.../WebonixTapBackendApplicationTests.java` | Veritabanına bağlı boş context testi yerine 14 MVC güvenlik regresyon testi. |
| `README.md`, bu belge | Proje kurulumu, mimari, doğrulama ve yol haritası. |

## Doğrulamalar

- `npm run lint`: başarılı.
- `npm test`: 14 test başarılı. Login, `/me` reddi, token olmayan/geçersiz oturum, yenileme, sunucu hatası ve tekrar deneme, `401/403`, logout, geç yanıtlar ve StrictMode kapsandı.
- `npm run build`: başarılı.
- `./mvnw test`: 14 test başarılı. Gerçek BCrypt login → JWT → `/me` → korumalı kart isteği; eksik/bozuk/süresi dolmuş/farklı anahtarla imzalanmış JWT; pasif/silinmiş kullanıcı; güncel DB kimliği; korumalı CRUD; CORS; eksik endpoint `404` kapsandı. Repository test doubles kullanıldı, MySQL'e yazılmadı.
- Gerçek `localhost:8080` + mevcut MySQL üzerinde salt okunur kontrol: tokensız/geçersiz/süresi dolmuş JWT `/me` → `401`; geçerli JWT `/me` → `200`; dönen isim DB ile eşleşiyor; geçerli JWT `/cards` → `200`.
- Tarayıcıda gerçek frontend, ayrı bir localhost test API'siyle kontrol edildi: başarılı login → Dashboard; yenilemede korunma; korumalı sayfalar arası geçiş; logout; geri tuşunda panelin açılmaması; geçersiz token ile yenilemede login; hatalı şifre; `403` sonrası panelde kalma. Test API'si gerçek müşteri verisi kullanmadı.
- Gerçek frontend `localhost:5173` üzerinde tokensız `/cards/new` → `/login` doğrulandı. Login Header/Sidebar göstermiyor. 390 px mobil görünümde içerik genişliği 390 px, giriş kartı 350 px; yatay taşma yok.

Sınır: Gerçek kullanıcının şifresi istenmedi veya değiştirilmedi; o hesapla uçtan uca tarayıcı login testi yapılmadı. Başarılı şifre doğrulaması backend'de gerçek BCrypt ile repository test verisi üzerinde, tarayıcı akışı ise ayrı test API'siyle doğrulandı. Mevcut MySQL'e kayıt eklenmedi/silinmedi; tablolar sıfırlanmadı.

## Sonraki işler

1. Ticari kullanım öncesi kart sahipliği ve rol yetkileri: şu anda giriş yapan her kullanıcı bütün kartlara erişebilir. Public register politikasını da buna göre netleştir.
2. `/r/{code}`: aktif kart kontrolü, güvenli hedef URL doğrulaması, atomik okutma artışı ve HTTP redirect. Kart güncellemesinde kod sabit kalmalı.
3. Gerçek dashboard: toplam/aktif/pasif kart ve toplam okutma; bugün/ay için zaman damgalı `nfc_scans` verisi ve saat dilimi kararı.
4. API validation, merkezi exception handling, doğru kart `404/409` yanıtları, eşzamanlı kısa kod çakışması, login rate limiting ve test kapsamı.
5. Üretim yapılandırması: DB migrations, HTTPS, üretim CORS/domain ayarları, secret yönetimi; geçmişte Git'e giren DB parolası için ayrı kontrollü parola yenileme. Yeni JWT anahtarı eski anahtarı geçersiz kılar; geçmiş Git içeriği bu çalışmada yeniden yazılmadı.
6. Statistics/Settings, kart sayfalarında loading/error durumları, şu anda işlevsiz “Beni hatırla” ve “Şifremi unuttum” alanları. HttpOnly Cookie/refresh-token ve sunucu tarafı oturum iptali daha sonra planlanmalı.
