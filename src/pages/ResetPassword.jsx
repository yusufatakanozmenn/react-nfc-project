import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { apiJson } from "../services/api";
import { invalidateSession } from "../auth/session";
import RecoveryLayout from "../components/RecoveryLayout";

export default function ResetPassword() {
  const location = useLocation();
  const navigate = useNavigate();
  const [token, setToken] = useState(
    () => new URLSearchParams(location.hash.slice(1)).get("token") || "",
  );
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => {
    // Keep the single-use secret only in this component's memory, never in storage/history.
    navigate("/reset-password", { replace: true });
  }, [navigate]);
  const submit = async (event) => {
    event.preventDefault();
    if (loading) return;
    if (password !== confirmation) {
      setError("Şifreler eşleşmiyor.");
      return;
    }
    if (new TextEncoder().encode(password).length > 72) {
      setError("Şifre en fazla 72 bayt olabilir.");
      return;
    }
    setLoading(true);
    setError("");
    try {
      await apiJson("/api/auth/reset-password", {
        method: "POST",
        auth: false,
        body: JSON.stringify({ token, password }),
      });
      setToken("");
      setPassword("");
      setConfirmation("");
      setDone(true);
      invalidateSession();
    } catch (failure) {
      setError(failure.message || "Şifre yenilenemedi. Lütfen tekrar deneyin.");
    } finally {
      setLoading(false);
    }
  };
  return (
    <RecoveryLayout
      title="Yeni şifre belirle"
      description="Yeni şifreniz en az 6 karakter olmalıdır. İşlem tamamlandığında diğer cihazlardaki oturumlarınız da kapatılır."
    >
      {done ? (
        <p className="auth-message" role="status">
          Şifreniz yenilendi. Yeni şifrenizle giriş yapabilirsiniz.
        </p>
      ) : !/^[A-Fa-f0-9]{64}$/.test(token) ? (
        <p className="auth-error" role="alert">
          Bağlantı eksik veya geçersiz.{" "}
          <Link to="/forgot-password">Yeni bağlantı isteyin.</Link>
        </p>
      ) : (
        <form onSubmit={submit}>
          <div className="auth-form-group">
            <label htmlFor="new-password">Yeni şifre</label>
            <input
              id="new-password"
              type="password"
              autoComplete="new-password"
              minLength={6}
              maxLength={72}
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={loading}
            />
          </div>
          <div className="auth-form-group">
            <label htmlFor="confirm-password">Yeni şifre tekrarı</label>
            <input
              id="confirm-password"
              type="password"
              autoComplete="new-password"
              minLength={6}
              maxLength={72}
              required
              value={confirmation}
              onChange={(e) => setConfirmation(e.target.value)}
              disabled={loading}
            />
          </div>
          {error && (
            <p className="auth-error" role="alert">
              {error} <Link to="/forgot-password">Yeni bağlantı isteyin.</Link>
            </p>
          )}
          <button type="submit" className="auth-submit" disabled={loading}>
            {loading ? "Şifre yenileniyor..." : "Şifremi yenile"}
          </button>
        </form>
      )}
    </RecoveryLayout>
  );
}
