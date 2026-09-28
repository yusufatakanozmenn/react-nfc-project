import { useState } from "react";
import { Navigate, Link } from "react-router-dom";

import { successToast, errorToast } from "../utils/toast";

import { useAuth } from "../auth/useAuth";
import { homePath } from "../auth/permissions";
import { login } from "../auth/session";
import AuthStatus from "../components/AuthStatus";

function Login() {
  const { status, user } = useAuth();
  const [formData, setFormData] = useState({
    email: "",
    password: "",
    rememberMe: false,
  });

  const [loading, setLoading] = useState(false);

  const handleChange = (event) => {
    const { name, value, type, checked } = event.target;

    setFormData((currentData) => ({
      ...currentData,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (loading) return;
    setLoading(true);
    try {
      await login(formData);
    } catch (error) {
      errorToast(error.message || "Giriş işlemi başarısız oldu.");
      return;
    } finally {
      setLoading(false);
    }
    successToast("Giriş başarılı.");
  };

  if (status === "checking" || status === "error") return <AuthStatus status={status} />;
  if (status === "authenticated") return <Navigate to={homePath(user)} replace />;

  return (
    <div className="auth-page">
      <div className="auth-background">
        <div className="auth-decoration auth-decoration-one" />
        <div className="auth-decoration auth-decoration-two" />
      </div>

      <div className="auth-container">
        <div className="auth-brand">
          <div className="auth-logo">W</div>

          <div>
            <h1>Webonix Tap</h1>
            <span>NFC Management Platform</span>
          </div>
        </div>

        <div className="auth-card">
          <div className="auth-card-header">
            <span className="auth-badge">Yönetim Paneli</span>

            <h2>Tekrar hoş geldiniz</h2>

            <p>NFC kartlarınızı yönetmek için hesabınıza giriş yapın.</p>
          </div>

          <form onSubmit={handleSubmit}>
            <div className="auth-form-group">
              <label htmlFor="email">E-posta adresi</label>

              <input
                id="email"
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                placeholder="ornek@webonix.com"
                autoComplete="email"
                required
              />
            </div>

            <div className="auth-form-group">
              <label htmlFor="password">Şifre</label>

              <input
                id="password"
                type="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                placeholder="••••••••"
                autoComplete="current-password"
                required
              />
            </div>

            <div className="auth-options">
              <label className="remember-me">
                <input type="checkbox" name="rememberMe" checked={formData.rememberMe} onChange={handleChange} />
                <span>Beni hatırla</span>
              </label>

              <Link to="/forgot-password" className="forgot-password">
                Şifremi unuttum
              </Link>
            </div>

            {formData.rememberMe && <p className="auth-help">Bu cihazda 7 gün boyunca oturumunuz açık kalır. Ortak cihazlarda seçmeyin.</p>}

            <button type="submit" className="auth-submit" disabled={loading}>
              {loading ? "Giriş yapılıyor..." : "Giriş Yap"}
            </button>
          </form>

          <div className="auth-footer">© 2026 Webonix Tap</div>
        </div>
      </div>
    </div>
  );
}

export default Login;
