<?php
$escape = static fn (string $value): string => htmlspecialchars($value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
?>
<!doctype html>
<html lang="tr"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Webonix Tap’a hoş geldiniz</title></head>
<body style="margin:0;padding:0;background:#f1f5f9;font-family:Arial,Helvetica,sans-serif;color:#172334">
<div style="display:none;max-height:0;overflow:hidden;opacity:0">Hesabınız hazır. NFC kartlarınızı tek yerden yönetmeye başlayın.</div>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f1f5f9"><tr><td align="center" style="padding:32px 16px">
<table role="presentation" width="600" cellpadding="0" cellspacing="0" style="width:100%;max-width:600px;background:#ffffff;border-radius:20px;overflow:hidden">
<tr><td style="padding:36px 32px;background:#101b2b;color:#ffffff">
<div style="font-size:25px;font-weight:bold;letter-spacing:-1px">Webonix <span style="color:#8ce2c6">Tap</span></div>
<p style="margin:10px 0 0;color:#c4d0df;font-size:13px">Bir dokunuşla bağlantı kur.</p>
</td></tr>
<tr><td style="padding:36px 32px">
<div style="font-size:12px;letter-spacing:2px;font-weight:bold;color:#247460">HESABINIZ HAZIR</div>
<h1 style="font-size:30px;line-height:1.25;margin:14px 0 20px">Webonix Tap’a<br>hoş geldiniz!</h1>
<p style="font-size:16px;line-height:1.7">Merhaba <strong><?= $escape($name) ?></strong>,</p>
<p style="font-size:15px;line-height:1.8;color:#526176">Hesabınız oluşturuldu. İşletmenizin dijital bağlantılarını tek bir panelden yönetmeye hazırsınız.</p>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0"><tr><td style="padding:20px;background:#edf7f3;border-radius:12px">
<div style="font-size:12px;color:#526176;margin-bottom:8px">GİRİŞ E-POSTANIZ</div>
<strong style="font-size:15px;word-break:break-all"><?= $escape($email) ?></strong>
</td></tr></table>
<p style="font-size:15px;line-height:1.9;color:#526176">Size atanan NFC kartlarını düzenleyin, aktif veya pasif yapın, okutma istatistiklerini inceleyin ve hesap bilgilerinizi güncelleyin.</p>
<table role="presentation" cellpadding="0" cellspacing="0" style="margin:24px 0"><tr><td bgcolor="#247460" style="border-radius:10px;text-align:center"><a href="<?= $escape($loginUrl) ?>" style="display:inline-block;padding:17px 28px;color:#ffffff;text-decoration:none;font-weight:bold;font-size:15px">Yönetim paneline giriş yap →</a></td></tr></table>
<p style="font-size:13px;line-height:1.7;color:#68788d">Şifrenizi bilmiyorsanız giriş sayfasındaki <strong>Şifremi unuttum</strong> seçeneğiyle kendi şifrenizi oluşturabilirsiniz.</p>
<p style="font-size:12px;line-height:1.7;color:#68788d">Buton çalışmıyorsa bu adresi tarayıcınıza yapıştırın:<br><a href="<?= $escape($loginUrl) ?>" style="color:#247460;word-break:break-all"><?= $escape($loginUrl) ?></a></p>
</td></tr>
<tr><td style="padding:24px 32px;border-top:1px solid #e8edf2;font-size:12px;line-height:1.8;color:#748196">Webonix Tap · NFC Yönetimi<br>Bu e-posta, sizin için bir müşteri hesabı oluşturulduğunda gönderilir.</td></tr>
</table></td></tr></table>
</body></html>
