# Webonix Tap — PHP / CodeIgniter 4 backend

React panelinin kullandığı PHP API. Java/VDS gerekmez; PHP ve MySQL destekleyen hostingde çalışır. `backend/` önceki Java uygulamasıdır. Kilitli CodeIgniter sürümü 4.7.4, gereken PHP sürümü **8.2 veya üzeri**; `intl`, `mbstring`, `mysqli` ve `openssl` uzantıları açık olmalıdır.

## Kurulum

1. `composer install` çalıştırın. Hosting paketi için `composer install --no-dev --optimize-autoloader` kullanın. Hostingde Composer yoksa bunu yerelde çalıştırıp `vendor/` dizinini de yükleyin.
2. `.env.example` dosyasını `.env` olarak kopyalayın; var olan dosyanın üzerine yazmayın. DB ve SMTP bilgilerini doldurun. Dosya Git dışında kalır; mümkünse izinleri `0600` yapın.
3. **Boş DB:** `database/schema.sql` dosyasını bir migration hesabıyla çalıştırın. **Mevcut Java DB:** yedeğini alın; `app_users`, `nfc_cards` (`owner_id`, `version` dâhil), `auth_sessions`, `password_resets` tablolarını koruyun. Eksik eski geçişler `../backend/migrations/` altındadır. Ardından `database/004_auth_rate_limits.sql` uygulanır. Eski DB üzerinde şema veya müşteri verisi silmeyin; yeni şemayı bir veri taşıma aracı gibi kullanmayın.
4. Uygulamanın DB kullanıcısına yalnızca kendi veritabanında `SELECT`, `INSERT`, `UPDATE`, `DELETE` verin; şema kurulumunu ayrı yetkili hesapla yapın.
5. `writable/` dizini PHP işleminin yazabildiği, web'den erişilemeyen bir dizin olmalıdır.

Yerelde `.env` için `CI_ENVIRONMENT=development`, `COOKIE_SECURE=0`, `APP_ORIGIN=http://localhost:5173`, `app.baseURL=http://localhost:8081/` kullanın:

```sh
cd backend2
composer install
php spark serve --host 127.0.0.1 --port 8081
```

Ayrı terminalde proje kökünde `npm run dev` çalıştırın. Paneli `http://localhost:5173` üzerinden açın; React yerelde varsayılan olarak `http://localhost:8081` API'sini kullanır. API URL'sini `VITE_API_URL` ile değiştirebilirsiniz. Panel ve API için `localhost` ile `127.0.0.1` isimlerini karıştırmayın.

## Paylaşımlı hosting

- API alt alan adının belge kökü **`backend2/public`** olmalı. Tüm `backend2` klasörünü doğrudan internete açmayın. `app`, `vendor`, `.env`, `database`, `tests`, `writable`, SQL yedekleri ve ZIP dosyaları public dizinde bulunmamalı.
- Hosting belge kökünü değiştirmenize izin vermiyorsa PHP uygulamasını `public_html` dışında tutun; yalnızca `public` içeriğini API'nin `public_html` klasörüne taşıyıp `index.php` içindeki `../vendor/autoload.php` ve `../app/Config/Paths.php` yollarını gerçek özel dizine göre düzenleyin.
- Apache/LiteSpeed için `public/.htaccess` URL yeniden yazımını yapar ve dizin listelemeyi kapatır. Uygulama kökündeki `.htaccess` özel dosyaların yanlışlıkla sunulmasına karşı ek korumadır.
- API ve panel için HTTPS sertifikaları açık olmalı. Örnek üretim ayarları: `CI_ENVIRONMENT=production`, `COOKIE_SECURE=1`, `APP_ORIGIN=https://nfc.webonix.com.tr`, `app.baseURL=https://nfc-api.webonix.com.tr/`. `APP_ORIGIN` sonunda `/` içermez, `app.baseURL` içerir.
- React'i `VITE_API_URL=https://nfc-api.webonix.com.tr npm run build` ile derleyip `dist/` içeriğini panel alan adına yükleyin. Panel hostinginde SPA fallback, `/cards`, `/settings`, `/reset-password` gibi yolları `index.html` dosyasına yönlendirmeli.
- HTTPS bir ters proxy'de sonlanıyorsa yalnızca gerçek proxy IP'lerini `Config\App::$proxyIPs` içinde tanımlayın. Genel `X-Forwarded-*` başlıklarına güvenmeyin. Eksik HTTPS/cookie yapılandırmasında üretim API'si istekleri reddeder.
- Sürüm değişince eski `SESSION` veya Java JWT cookie'si taşınmaz; kullanıcılar yeniden giriş yapar. Mevcut BCrypt şifreleri kullanılabilir, yeniden belirlenmeleri gerekmez.

## API ve yetkiler

Mevcut React yolları korunur: `/api/auth/*`, `/api/cards/*`, `/api/admin/users`, `/api/admin/customers`, `/api/statistics`.

- ADMIN tüm kartları/müşterileri yönetir. USER yalnızca kendi kartlarını ve istatistiklerini görür; kendi kartını düzenleyip aktif/pasif yapabilir. Kart oluşturma, silme ve sahip atama ADMIN'e aittir.
- Profil ve şifre değiştirme mevcut şifreyi doğrular; hedef kullanıcı yalnızca oturumdan belirlenir. İstek içindeki `id`, `role`, `active`, `scans` gibi yetkisiz alanlar kabul edilmez.
- Yeni şifre en az 6 karakter, en fazla 72 UTF-8 bayttır. Şifre değişince **tüm** oturumlar ve sıfırlama bağlantıları iptal olur. Otomatik giriş yapılmaz.
- Cookie `HttpOnly`, `SameSite=Strict`; üretimde ayrıca `Secure` ve `__Host-` önekli, Domain alanı olmadan kullanılır. Normal oturum 1 saat/sesssion cookie, “Beni hatırla” 7 gün/kalıcı cookie. DB'de rastgele oturum anahtarının sadece SHA-256 özeti tutulur; JWT kullanılmaz.
- Tüm POST/PUT/PATCH/DELETE istekleri CSRF kontrolünden geçer. `/api/auth/csrf` yanıtındaki `token`, `X-XSRF-TOKEN` başlığıyla gönderilir. Tarayıcı istekleri `credentials: include` kullanır. Yalnızca tanımlı origin kabul edilir.
- Hatalar JSON'dur: oturumsuz `401`, rol engeli `403`, görünmeyen/bulunmayan kart `404`, geçersiz alan `400`, tekrar eden kayıt `409`, deneme sınırı `429`.
- Kullanıcı ve kart satırları transaction içinde kilitlenir. Sahibi değişen kart eski sahibi tarafından gecikmiş bir istekle güncellenemez. Aynı şifre sıfırlama anahtarı eşzamanlı iki istekte kullanılamaz.

NFC karta yazılacak bağlantı: `https://nfc-api.webonix.com.tr/r/KART_KODU`. Aktif kartın kayıtlı HTTP(S) adresine `302` yönlendirir ve okutma toplamını atomik artırır. Pasif/bulunmayan/geçersiz hedefli kart `404` verir. Sayaç toplam ziyaret sayısıdır; tekil kişi, bot ayrımı veya günlük/aylık olay analizi değildir.

## E-posta ve deneme sınırları

`.env` içinde `MAIL_ENABLED=1`, `MAIL_FROM`, `MAIL_FROM_NAME`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_ENCRYPTION` ayarlanır. Hostinger için sunucu `smtp.hostinger.com`, port `587`, şifreleme `tls`. Gerçek parolayı kaynak koduna veya frontend ortamına koymayın.

Bağlantı 15 dakika geçerli ve tek kullanımlıktır. DB'de yalnızca özeti tutulur; yeni istek eski bağlantıyı geçersiz kılar. E-posta değişen veya pasifleşen hesaba eski bağlantı ile şifre atanamaz. SMTP hatasında önceki geçerli bağlantı korunur. Gönderici e-posta içeriğini, tokenı veya SMTP dökümünü hata günlüğüne yazmaz. SMTP kapalı/eksik yapılandırılmışsa `503`, çalışırken bilinen/bilinmeyen hesaplara aynı `202` yanıtı verilir.

15 dakikalık sınırlar MySQL'de, PHP işlemleri arasında ortak tutulur; hesap ve IP ayrı ayrı sayılır:

| İşlem | Hesap başına | IP başına |
| --- | ---: | ---: |
| Giriş | 10 | 30 |
| Sıfırlama e-postası isteme | 3 | 20 |
| Sıfırlama bağlantısını kullanma | — | 10 |
| Profil/şifre değiştirme (ayrı sayaçlar) | 10 | 30 |

Aşımda `Retry-After` döner. Eski deneme kayıtları sınırlı partilerle temizlenir. SMTP senkron çalışır; gerçek gönderim ile bilinmeyen hesap yanıtı arasında zaman farkı olabilir. Yoğun kullanım ve bu zaman farkının azaltılması için hosting cron'una bağlı kalıcı bir e-posta kuyruğu ayrı geliştirme konusudur. Bu sürümde canlı SMTP teslimi veya hosting kurulumu yapılmadı.

## Testler

`composer test` gerçek HTTP sunucusu + **silinip yeniden doldurulabilen ayrı bir MySQL DB** ile çalışır. PHP `curl` uzantısı ve Python 3 (yerel sahte SMTP için) gerekir. Yalnızca adı `_test` ile biten ve açıkça verilen DB kabul edilir. Test giriş noktası gerçek `.env` dosyasını yüklemez. **Test DB içindeki kullanıcı, kart, oturum ve rate-limit kayıtları her senaryoda temizlenir.**

Test bağlantısını ortam değişkenleriyle verin: `TEST_DB_NAME`, `TEST_DB_PASSWORD`, isteğe bağlı `TEST_DB_USER` (varsayılan root), `TEST_DB_PORT` (13306), `TEST_HTTP_PORT` (18081), `TEST_SMTP_PORT` (18082). Parolayı shell geçmişine yazmak yerine güvenli yerel ortamdan yükleyin. DB hostu test güvenliği için `127.0.0.1` sabittir.

```sh
composer test
composer validate --no-check-publish
composer audit --locked --no-dev
```

Eski Java şemasını test etmek için yeni, boş başka bir `_test` DB üzerinde `TEST_LEGACY_SCHEMA=1` kullanın. `tests/fixtures/legacy-schema.sql` yalnızca DDL içerir; müşteri verisi/şifre içermez.

Testler; cookie süreleri, CSRF/CORS, rol/sahiplik, alan doğrulama, şifre değişimi, tek kullanımlı sıfırlama, paralel istekler, yerel SMTP başarı/hata, NFC yönlendirme ve BCrypt uyumluluğunu kapsar. Gerçek kişilere e-posta göndermez.

Transaction davranışı için [CodeIgniter 4 belgesi](https://codeigniter.com/user_guide/database/transactions.html) esas alınmıştır.
