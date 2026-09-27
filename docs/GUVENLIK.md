# Güvenlik çalışması — 27 Eylül 2026

Bu çalışma uygulamayı güçlendirir; kapsamlı bağımsız sızma testi veya sıfır risk garantisi değildir.

## Uygulanan korumalar

- Açık kayıt kapatıldı. Müşteri oluşturma yalnızca ADMIN'e açık; yeni hesap daima USER. Yeni şifrelerde en az 15 karakter ve BCrypt nedeniyle en fazla 72 UTF-8 bayt kuralı var. Mevcut kullanıcı şifreleri değiştirilmedi.
- Hesap başına 10, IP başına 30 giriş denemesi / 15 dakika. Başarılı denemeler de sayılır. Sayaçlar eşzamanlı isteklerde atomiktir, hafıza kullanımı sınırlıdır; kapasite dolduğunda yeni girişleri geçici reddeder. Forwarded başlıkları geliştirmede kabul edilmez. Üretimde sadece loopback proxy güvenilirdir ve Nginx gelen başlığı gerçek istemci IP'siyle değiştirir.
- Bilinmeyen e-posta, yanlış parola ve pasif hesap aynı giriş hatasını alır. Bilinmeyen kullanıcıda da BCrypt kontrolü yapılır. Aşırı uzun parolalar BCrypt'e ulaşmadan reddedilir.
- JWT süresi 24 saatten 1 saate indirildi; her oturum benzersiz kimlik taşır. Token yalnızca HttpOnly / SameSite=Strict cookie'de bulunur. Üretimde Secure ve `__Host-` öneki kullanılır. Token ve kullanıcı bilgisi LocalStorage'a yazılmaz; eski kayıtlar temizlenir. API gövdesinde token dönmez; Bearer yolu kaldırıldı.
- MySQL `auth_sessions` tablosunda tokenın SHA-256 özeti, kullanıcı ID'si ve bitiş zamanı tutulur. İmza/son kullanma/oturum kaydı/aktif kullanıcı ve güncel rol her istekte kontrol edilir. Çıkış kaydı siler; yeniden giriş mevcut cookie'nin oturumunu iptal eder. Süresi dolmuş oturum kayıtları yeni girişlerde temizlenir.
- Giriş ve çıkış dahil tüm yazma işlemleri Spring Security CSRF kontrolü kullanır. CSRF cookie'si HttpOnly'dir; frontend tokenı `/api/auth/csrf` gövdesinden alır. Varsayılan XOR/BREACH koruması korunmuştur. CORS yalnızca açıkça tanımlanan origin'lere ve gereken başlıklara izin verir.
- Varsayılan erişim reddedilir. Kart ve istatistik kapsamı backend'de sahipliğe göre belirlenir. Kartın kodunu/sahibini normal düzenleme isteğiyle değiştirmek veya profil üzerinden ADMIN olmak engellenir.
- Kartta optimistic locking (`@Version`) var: eşzamanlı sahiplik transferi sonrası eski işlem kaydı geri yazamaz; API `409` ile yeniden denemeyi ister. Bu, tarayıcıda eski formdaki metnin yeni bir istekle kaydedilmesini genel olarak engelleyen bir ETag sistemi değildir; işlem sırasında gerçekleşen sahiplik yarışını önler.
- Toast kütüphanesi HTML ürettiği için dinamik mesajlar HTML-escape edilir. Backend güvenlik başlıkları etkin; frontend için CSP/HSTS ve ek başlıklar Nginx örneğinde bulunur.
- Boot'un kullanılmayan otomatik geliştirme hesabı kapatıldı. Parola/token güvenlik test raporlarında yazdırılmaz.

## Bağımlılıklar

Tomcat 11.0.24 için OSV taraması `GHSA-9xv2-5v5q-p794`, `GHSA-gcx9-497g-6cp6`, `GHSA-h3x4-894j-xpx5` kayıtlarını buldu. `pom.xml` Tomcat bileşenlerini 11.0.26'ya sabitler. [Apache güvenlik duyuruları](https://tomcat.apache.org/security-11.html) düzeltmeleri açıklar. Bunların listelenmesi bu uygulamada kullanılmış veya istismar edilmiş oldukları anlamına gelmez.

Güncelleme sonrasında npm ve [OSV API](https://google.github.io/osv.dev/post-v1-querybatch/) ile çözümlenmiş Java bağımlılıkları tekrar tarandı; sonuçlar sıfır bilinen bulgu. OSV taraması test classpath'indeki paketleri de kapsar; işletim sistemi, JDK veya Docker imajının tam taraması değildir. Tekrarlanabilir komutlar README'dedir.

## Yerel MySQL ve şema

- Uygulama DB parolası güçlü rastgele değerle yenilendi; eski parolanın reddedildiği doğrulandı. Parola sadece Git dışındaki 0600 izinli `backend/.env` dosyasında. Git geçmişindeki eski parola artık çalışmıyor; geçmiş yeniden yazılmadı.
- `webonix` hesabı yalnızca `webonix_tap` veritabanında SELECT/INSERT/UPDATE/DELETE yetkilidir. Şema yönetimi ayrı yönetici/migration hesabına aittir. `ddl-auto=validate` tablo değiştirmez; uyuşmayan şemada açılışı durdurur.
- `001_auth_sessions.sql` ve `002_card_version.sql` mevcut proje şemasına ek geçişlerdir. İkinci geçiş bir kez çalıştırılır; otomatik migration runner bulunmuyor. Yeni boş veritabanında önce mevcut app_users/nfc_cards temel şeması kurulmalıdır. Üretim geçişinden önce yedek alınmalı ve geri yükleme denenmelidir.
- MySQL container'ı aynı imaj ve aynı veri volume'ü ile yeniden oluşturuldu. Yayınlanan port yalnızca `127.0.0.1:3306`. Eski container `webonix-mysql-before-security` adıyla durmuş halde korunur; aynı volume ile ikisini birden başlatmayın. Eski container'ın ağ ayarı geniştir; normal çalıştırma için güncel `webonix-mysql` kullanılmalıdır.
- Backend yalnızca `127.0.0.1:8080` dinler. Değişiklikler sırasında 3 kullanıcı ve 2 kart korunmuş, kullanıcı parolaları/sahiplikleri değiştirilmemiştir. Canlı doğrulamadaki tek geçici kayıt test oturumudur; çıkışta silinmiştir.

## Doğrulama ve sınırlar

Son doğrulama: **52 backend testi, 21 frontend testi, ESLint ve üretim build başarılı**. npm taraması ve test bağımlılıkları dahil 116 Java paketi için OSV taraması sıfır bilinen bulgu döndürdü.

Otomatik testler: ADMIN/USER kapsamı, başkasının kartına erişim, rol yükseltme girişimi, CSRF eksik/yanlış cookie, yabancı origin, register engeli, JWT sahteciliği/süresi, DB rolünün esas alınması, oturum iptali, hız sınırının eşzamanlı çalışması ve üretim yapılandırmasının hatalı ayarları reddetmesi. H2 üzerinde gerçek Hibernate işlemleriyle eşzamanlı sahiplik transferi ve oturum kalıcılığı ayrıca kontrol edilir.

Canlı localhost backend + MySQL: `/me`, kartlar, istatistikler ve müşteri listesinde ADMIN okuma; CSRF'siz çıkışın reddi; açık kayıt engeli; geçerli çıkış ve kopyalanmış cookie'nin ardından 401 olması doğrulandı. Bu kontrolde müşteri parolası kullanılmadı; kısa süreli tanılama oturumu oluşturulup temizlendi.

Tarayıcı: ayrı bellek içi test API'siyle USER ve ADMIN girişi, cookie oturumunun sayfa yenilemesinde korunması, USER menü/kart kapsamı, CSRF ile kart durumu güncelleme, müşteri ekranı, çıkış ve oturumsuz korumalı route kontrol edildi. Tarayıcı testi gerçek müşteri verisine bağlanmadı.

## Üretimde ayrıca tamamlanacaklar

1. Gerçek alan adı, TLS sertifikası, Nginx yapılandırması ve güvenlik duvarını kurup dış ağdan kontrol edin. Örnek yapılandırma hazırlanmıştır; canlı TLS kurulumu yapılmamıştır. MySQL'i internetten erişilebilir yapmayın. Uzak DB bağlantısında sertifika doğrulamalı TLS kullanın.
2. `SPRING_PROFILES_ACTIVE=production` ve güçlü, ayrı üretim sırları kullanın. Bu profil Secure cookie, HTTPS CORS, `validate` şema ve root olmayan DB hesabını zorunlu kılar. HTTPS panelden aynı origin `/api/` önerilir. Yerel `.env` dosyalarını sunucuya veya frontend bundle'a taşımayın.
3. Giriş sayaçları şu an tek JVM belleğindedir. Yatay ölçeklemeden önce merkezi Redis/DB tabanlı ortak sayaç ve edge/WAF hız sınırlaması gerekir. Nginx örneği ayrıca IP başına giriş limiti içerir.
4. Yönetici MFA, parola değiştirme/sıfırlama, oturumları listeleyip tümünden çıkış ve güvenlik olaylarına alarm eklenmedi. Bunlar sonraki güvenlik geliştirmeleridir. Mevcut kısa kullanıcı parolaları otomatik değiştirilmedi.
5. Yedekleme/geri yükleme, işletim sistemi/JDK/MySQL yama politikası, izleme ve bağımsız sızma testi canlıya çıkışın parçası olmalıdır. Bu makinedeki Java 21.0.1 çalışma zamanı uygulama bağımlılık taramasının dışındadır; güncel desteklenen Java 21 yamasına geçiş gerekir.

CSRF uygulaması için [Spring Security resmi kılavuzu](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html) esas alınmıştır.
