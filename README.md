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

Şema ayarı artık `ddl-auto=validate`: uygulama tablo oluşturmaz, değiştirmez veya silmez. Mevcut Webonix şemasına ek geçişler `backend/migrations/` altında; bunları ayrı migration hesabıyla bir kez uygulayın. Yerel veritabanına güvenlik geçişleri uygulandı. Uygulama DB hesabı yalnızca SELECT/INSERT/UPDATE/DELETE kullanır.

## Oturum akışı

- `src/auth/session.js` merkezi oturum kaynağıdır. Açılışta kimlik `/api/auth/me` üzerinden doğrulanır.
- Giriş JWT'si yalnızca HttpOnly, SameSite=Strict cookie'de taşınır; API yanıtında veya LocalStorage'da bulunmaz. Eski LocalStorage kayıtları otomatik temizlenir. Bearer giriş yöntemi kapalıdır.
- Oturum bir saat geçerlidir. JWT'nin SHA-256 özeti ve bitiş zamanı MySQL `auth_sessions` tablosunda tutulur; her istekte aktif oturum, kullanıcı ve güncel rol doğrulanır.
- Çıkış, sunucudaki oturumu iptal eder ve cookie'yi siler. Kopyalanmış eski cookie tekrar kullanılamaz. Ağ hatasında başarılı çıkış yapılmış gibi davranılmaz.
- POST/PUT/PATCH/DELETE işlemleri, giriş/çıkış dahil CSRF doğrulaması ister. Frontend önce `/api/auth/csrf` çağırır, sonra `X-XSRF-TOKEN` başlığını gönderir; fetch `credentials: include` kullanır.
- `401` mevcut oturumu kapatır; `403` kapatmaz. Eski yanıtlar yeni oturumu silemez. Sekmeler arasında yalnızca değişiklik bildirimi paylaşılır.
- ADMIN Dashboard'a, USER Kartlarım'a girer. Müşteri hesabını sadece ADMIN `/api/admin/customers` üzerinden oluşturur; `/api/auth/register` kapalıdır.
- Giriş 15 dakikada hesap başına 10, IP başına 30 denemeyle sınırlıdır; aşımda `429` ve `Retry-After` döner. Sayaçlar tek sunucunun belleğindedir; yeniden başlatmada sıfırlanır. Çoklu sunucuda ortak sayaç gerekir.

Yerel geliştirmede hem paneli hem API'yi `localhost` adıyla kullanın; `127.0.0.1` ile `localhost` adreslerini karıştırmayın. Üretimde Secure ve `__Host-` cookie'leri için HTTPS zorunludur.

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

Frontend testleri ağ yanıtlarını taklit eder. Backend testleri gerçek Controller/Service/JWT/BCrypt/Security zincirini MockMvc ile çalıştırır; repository test verileri kullanılır. Ayrıca bellek içi H2 üzerinde gerçek Hibernate işlemleriyle oturum kalıcılığı ve eşzamanlı kart sahipliği değişimi test edilir. Bu testler mevcut MySQL müşteri verilerine yazmaz.

Ayrıntılı inceleme, dosya değişiklikleri, doğrulama sınırları ve sonraki işler: [Devir ve auth incelemesi](docs/AUTH-INCELEME.md).

## Roller ve kullanıcıya kart atama

ADMIN bütün kartları yönetir; USER yalnızca kendi kartlarını görür, düzenler ve aktif/pasif yapar. Oluşturma, silme ve kullanıcıya atama admin'e özeldir. Atanmamış kartlar yalnızca admin'e görünür. İstatistikler de aynı sahiplik kapsamıyla hesaplanır.

Admin, kartın Düzenle → Kullanıcıya Ata bölümünden sahip seçebilir. Ayarlar sayfasında oturum sahibi mevcut şifresini doğrulayarak kendi adını ve e-postasını günceller. JWT kullanıcı ID'sine bağlıdır; e-posta değişikliği oturumu başka hesaba taşımaz. Önceki e-posta tabanlı JWT'ler için bir kez tekrar giriş gerekir.

[Güncel yetki tablosu, endpointler ve testler](docs/ROLLER-VE-SAHIPLIK.md).

## Müşteri ekleme ve kart bağlama

Admin menüsünde **Müşteriler** ekranını açın. Müşteri/işletme adı, e-posta ve giriş şifresini girerek müşteri oluşturun. Hesap aktif `USER` olarak kaydedilir; admin oturumu değişmez. Müşteri e-posta ve belirlediğiniz şifreyle kendi paneline giriş yapar.

Müşteriyi **Kartları Yönet** ile seçip atanmamış bir kartı **Kartı Müşteriye Bağla** düğmesiyle bağlayın. **Bu Müşteri İçin Yeni Kart** bağlantısı yeni kart formunu o müşteri seçili olarak açar. Başka müşteriye atanmış kartın sahipliğini değiştirmek için mevcut kart düzenleme ekranını kullanın.

Yeni müşteri şifresi en az 15 karakter ve BCrypt sınırı nedeniyle en fazla 72 UTF-8 baytı olabilir; kayıt sonrasında listelerde ve API yanıtlarında gösterilmez. Aynı e-posta tekrar kullanılamaz. Müşteri ekranı ve `/api/admin/customers` GET/POST endpointleri yalnızca ADMIN'e açıktır.

## Güvenlik ve yayınlama

27 Eylül 2026 güvenlik düzenlemeleri, test sonuçları ve doğrulama sınırları: [Güvenlik notları](docs/GUVENLIK.md).

Üretim için `SPRING_PROFILES_ACTIVE=production`, açık bir `DB_URL`, ayrı DB hesabı ve tam HTTPS adreslerinden oluşan `CORS_ALLOWED_ORIGINS` ayarlayın. Frontend'i aynı alan adından sunmak için `VITE_API_URL= npm run build` kullanın; `/api/` isteklerini backend'e yönlendirin. [Nginx örneği](deploy/nginx.conf.example) alan adı, sertifika ve dosya yolları doldurulmadan kullanılamaz. Burada canlı alan adı/sertifika kurulumu yapılmadı.

Yerel MySQL artık yalnızca `127.0.0.1:3306`, backend `127.0.0.1:8080` dinler. DB parolası yenilendi; güncel değer sadece Git dışındaki `backend/.env` dosyasındadır. Uygulama kullanıcılarının giriş şifreleri değiştirilmedi. Önceki MySQL container'ı `webonix-mysql-before-security` adıyla durmuş olarak korunur; aynı veri diskini kullandıkları için eski ve yeni container birlikte başlatılmamalıdır.

Bağımlılık taramasını tekrarlamak için:

```sh
npm audit
# Backend testlerini çalıştırdıktan sonra:
python3 scripts/audit-java.py
```

Java taraması OSV'ye yalnızca çözümlenmiş Maven paket adlarını/sürümlerini gönderir. Sıfır bulgu, gelecekte veya henüz bilinmeyen açık olmadığı garantisi değildir.
