# Webonix Tap

NFC kart yönetim paneli. Frontend React 19 + Vite 8 + React Router 7; backend Java 21 + Spring Boot 4.1.1 + Spring Security + JPA + MySQL. Backend katmanları Controller → Service → Repository şeklindedir.

## Yerel çalıştırma

Frontend için Node.js 22+, backend için Java 21 ve mevcut MySQL veritabanı gerekir. Maven Wrapper backend içinde bulunur.

```sh
npm ci
npm run dev
```

Frontend varsayılan adresi `http://localhost:5173`, API adresi `http://localhost:8080` olur. API adresini değiştirmek için kökte `.env.example` dosyasını `.env` olarak kopyalayıp `VITE_API_URL` ayarlayın. `VITE_` değişkenleri tarayıcıya gönderilir; buraya sır koymayın.

Backend için `backend/.env.example` dosyasını `backend/.env` olarak kopyalayın; mevcut veritabanı kullanıcı bilgilerini ve güçlü, rastgele bir `JWT_SECRET` girin. Bu dosya zaten varsa üzerine yazmayın. JWT anahtarı en az 32 bayt olmalıdır; örneğin güvenilir bir parola yöneticisiyle üretilmiş 64 bayt rastgele değer kullanılabilir.

```sh
cd backend
./mvnw spring-boot:run
```

Backend `.env` dosyasını Java properties biçiminde otomatik okur; değerleri tırnak içine almayın veya `export` yazmayın. Alternatif olarak `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `DB_URL`, `CORS_ALLOWED_ORIGINS` süreç ortamından verilebilir. `.env` dosyaları Git dışında tutulur. Anahtar değişirse eski JWT'ler geçersiz olur ve tekrar giriş gerekir.

Mevcut geliştirme ayarı `ddl-auto=update` olarak korunmuştur. Bu proje açılışta veritabanını sıfırlayan bir komut içermez; üretim öncesinde şema geçişleri ayrı planlanmalıdır.

## Oturum akışı

- `src/auth/session.js` tek oturum kaynağıdır; `useAuth` bileşenleri bu kaynağa abone eder.
- Uygulama açılışında kayıtlı token `/api/auth/me` ile doğrulanır. LocalStorage'daki kullanıcı nesnesi yetki kaynağı değildir.
- Login önce `/api/auth/login`, ardından alınan JWT ile `/api/auth/me` çağırır. İkisi de başarılıysa ADMIN için Dashboard, USER için Kartlarım açılır.
- Token yok/geçersiz ise login; sunucu geçici olarak ulaşılamıyorsa yeniden deneme ekranı gösterilir.
- Korumalı API'den gelen `401` oturumu temizler. `403` oturumu kapatmaz. Eski istekler yeni oturumu silemez.
- Logout tokenı, kullanıcı önbelleğini ve React'in merkezi durumunu temizler. Sekmeler arası değişiklikler dinlenir.
- `/login` Header/Sidebar içermez; tüm yönetim sayfaları `ProtectedLayout` altındadır.

JWT'ler şu anda 24 saat geçerlidir. Logout tarayıcıdaki oturumu sonlandırır; önceden kopyalanmış bir JWT'yi sunucu tarafında iptal eden bir token listesi henüz yoktur. Hesabı pasifleştirmek veya anahtarı yenilemek backend erişimini keser.

## Doğrulama

```sh
npm run lint
npm test
npm run build
```

```sh
cd backend
./mvnw test
```

Frontend testleri ağ yanıtlarını taklit ederek oturum davranışlarını doğrular. Backend testleri gerçek Controller/Service/JWT/BCrypt/Security zincirini MockMvc ile çalıştırır; repository yerine test verisi kullanır ve MySQL'e yazmaz.

Ayrıntılı inceleme, dosya değişiklikleri, doğrulama sınırları ve sonraki işler: [Devir ve auth incelemesi](docs/AUTH-INCELEME.md).

## Roller ve kullanıcıya kart atama

ADMIN bütün kartları yönetir; USER yalnızca kendi kartlarını görür, düzenler ve aktif/pasif yapar. Oluşturma, silme ve kullanıcıya atama admin'e özeldir. Atanmamış kartlar yalnızca admin'e görünür. İstatistikler de aynı sahiplik kapsamıyla hesaplanır.

Admin, kartın Düzenle → Kullanıcıya Ata bölümünden sahip seçebilir. Ayarlar sayfasında oturum sahibi mevcut şifresini doğrulayarak kendi adını ve e-postasını günceller. JWT kullanıcı ID'sine bağlıdır; e-posta değişikliği oturumu başka hesaba taşımaz. Önceki e-posta tabanlı JWT'ler için bir kez tekrar giriş gerekir.

[Güncel yetki tablosu, endpointler ve testler](docs/ROLLER-VE-SAHIPLIK.md).

## Müşteri ekleme ve kart bağlama

Admin menüsünde **Müşteriler** ekranını açın. Müşteri/işletme adı, e-posta ve giriş şifresini girerek müşteri oluşturun. Hesap aktif `USER` olarak kaydedilir; admin oturumu değişmez. Müşteri e-posta ve belirlediğiniz şifreyle kendi paneline giriş yapar.

Müşteriyi **Kartları Yönet** ile seçip atanmamış bir kartı **Kartı Müşteriye Bağla** düğmesiyle bağlayın. **Bu Müşteri İçin Yeni Kart** bağlantısı yeni kart formunu o müşteri seçili olarak açar. Başka müşteriye atanmış kartın sahipliğini değiştirmek için mevcut kart düzenleme ekranını kullanın.

Şifre en az 8 karakter ve BCrypt sınırı nedeniyle en fazla 72 UTF-8 baytı olabilir; kayıt sonrasında listelerde ve API yanıtlarında gösterilmez. Aynı e-posta tekrar kullanılamaz. Müşteri ekranı ve `/api/admin/customers` GET/POST endpointleri yalnızca ADMIN'e açıktır.
