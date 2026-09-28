import { useState } from "react";
import { apiJson } from "../services/api";
import RecoveryLayout from "../components/RecoveryLayout";

export default function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const submit = async (event) => {
    event.preventDefault();
    if (loading) return;
    setLoading(true); setError(""); setMessage("");
    try {
      const result = await apiJson("/api/auth/forgot-password", { method: "POST", auth: false, body: JSON.stringify({ email }) });
      setMessage(result.message);
    } catch (failure) { setError(failure.message || "İstek gönderilemedi. Lütfen tekrar deneyin."); }
    finally { setLoading(false); }
  };
  return <RecoveryLayout title="Şifremi unuttum" description="Hesabınızın e-posta adresini girin. Şifrenizi yenileyebilmeniz için bir bağlantı gönderelim.">
    {message ? <p className="auth-message" role="status">{message}</p> : <form onSubmit={submit}>
      <div className="auth-form-group"><label htmlFor="recovery-email">E-posta adresi</label>
        <input id="recovery-email" type="email" autoComplete="email" maxLength={150} required value={email} onChange={e => setEmail(e.target.value)} disabled={loading} />
      </div>
      {error && <p className="auth-error" role="alert">{error}</p>}
      <button type="submit" className="auth-submit" disabled={loading}>{loading ? "İstek gönderiliyor..." : "Sıfırlama bağlantısı gönder"}</button>
    </form>}
  </RecoveryLayout>;
}
