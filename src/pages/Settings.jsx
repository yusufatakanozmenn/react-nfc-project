import { useState } from "react";
import { useAuth } from "../auth/useAuth";
import { updateProfile, changePassword } from "../auth/session";
import { successToast } from "../utils/toast";

export default function Settings() {
  const { user } = useAuth();
  const [form, setForm] = useState({ name: user.name, email: user.email, currentPassword: "" });
  const [passwordForm, setPasswordForm] = useState({ currentPassword: "", newPassword: "", confirmPassword: "" });
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [passwordError, setPasswordError] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const change = (event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  const save = async (event) => {
    event.preventDefault();
    if (saving || passwordSaving) return;
    setSaving(true); setError("");
    try {
      const updated = await updateProfile(form);
      setForm({ name: updated.name, email: updated.email, currentPassword: "" });
    } catch (error) {
      setError(error.message); return;
    } finally { setSaving(false); }
    successToast("Hesap bilgileriniz güncellendi.");
  };
  const changePasswordField = (event) => setPasswordForm(current => ({ ...current, [event.target.name]: event.target.value }));
  const savePassword = async (event) => {
    event.preventDefault();
    if (saving || passwordSaving) return;
    setPasswordError("");
    if (passwordForm.newPassword !== passwordForm.confirmPassword) { setPasswordError("Yeni şifreler eşleşmiyor."); return; }
    if (new TextEncoder().encode(passwordForm.newPassword).length > 72) { setPasswordError("Şifre en fazla 72 bayt olabilir."); return; }
    setPasswordSaving(true);
    try {
      await changePassword(passwordForm);
      setPasswordForm({ currentPassword: "", newPassword: "", confirmPassword: "" });
      successToast("Şifreniz değiştirildi. Yeni şifrenizle tekrar giriş yapın.");
    } catch (failure) { setPasswordError(failure.message || "Şifre değiştirilemedi. Lütfen tekrar deneyin."); }
    finally { setPasswordSaving(false); }
  };
  return <>
    <div className="page-header"><div><h1>Ayarlar</h1><p>Kendi hesap bilgilerinizi güncelleyin.</p></div></div>
    <div className="form-container">
      <form onSubmit={save}>
        <div className="form-group"><label htmlFor="profile-name">Ad soyad</label>
          <input id="profile-name" name="name" value={form.name} onChange={change} autoComplete="name" maxLength={100} required />
        </div>
        <div className="form-group"><label htmlFor="profile-email">E-posta</label>
          <input id="profile-email" name="email" type="email" value={form.email} onChange={change} autoComplete="email" maxLength={150} required />
        </div>
        <div className="form-group"><label htmlFor="profile-password">Mevcut şifreniz</label>
          <input id="profile-password" name="currentPassword" type="password" value={form.currentPassword} onChange={change} autoComplete="current-password" required />
          <p className="field-hint">Değişiklikleri kaydetmek için mevcut şifrenizi doğrulayın.</p>
        </div>
        {error && <p className="form-error" role="alert">{error}</p>}
        <button className="primary-button" disabled={saving || passwordSaving}>{saving ? "Kaydediliyor..." : "Bilgilerimi Güncelle"}</button>
      </form>
    </div>
    <section className="form-container settings-password-section" aria-labelledby="password-settings-title">
      <h2 id="password-settings-title">Şifremi değiştir</h2>
      <p className="field-hint">Yeni şifreniz en az 6 karakter olmalıdır. Değişiklikten sonra tüm cihazlardaki oturumlarınız kapanır ve yeniden giriş yapmanız gerekir.</p>
      <form onSubmit={savePassword}>
        <div className="form-group"><label htmlFor="change-current-password">Mevcut şifre</label>
          <input id="change-current-password" name="currentPassword" type="password" autoComplete="current-password" maxLength={72} value={passwordForm.currentPassword} onChange={changePasswordField} required disabled={saving || passwordSaving} />
        </div>
        <div className="form-group"><label htmlFor="change-new-password">Yeni şifre</label>
          <input id="change-new-password" name="newPassword" type="password" autoComplete="new-password" minLength={6} maxLength={72} value={passwordForm.newPassword} onChange={changePasswordField} required disabled={saving || passwordSaving} />
        </div>
        <div className="form-group"><label htmlFor="change-confirm-password">Yeni şifre tekrarı</label>
          <input id="change-confirm-password" name="confirmPassword" type="password" autoComplete="new-password" minLength={6} maxLength={72} value={passwordForm.confirmPassword} onChange={changePasswordField} required disabled={saving || passwordSaving} />
        </div>
        {passwordError && <p className="form-error" role="alert">{passwordError}</p>}
        <button className="primary-button" type="submit" disabled={saving || passwordSaving}>{passwordSaving ? "Şifre değiştiriliyor..." : "Şifremi Değiştir"}</button>
      </form>
    </section>
  </>;
}
