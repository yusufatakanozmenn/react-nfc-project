import { useState } from "react";
import { useAuth } from "../auth/useAuth";
import { updateProfile } from "../auth/session";
import { successToast } from "../utils/toast";

export default function Settings() {
  const { user } = useAuth();
  const [form, setForm] = useState({ name: user.name, email: user.email, currentPassword: "" });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const change = (event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  const save = async (event) => {
    event.preventDefault();
    if (saving) return;
    setSaving(true); setError("");
    try {
      const updated = await updateProfile(form);
      setForm({ name: updated.name, email: updated.email, currentPassword: "" });
    } catch (error) {
      setError(error.message); return;
    } finally { setSaving(false); }
    successToast("Hesap bilgileriniz güncellendi.");
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
        <button className="primary-button" disabled={saving}>{saving ? "Kaydediliyor..." : "Bilgilerimi Güncelle"}</button>
      </form>
    </div>
  </>;
}
