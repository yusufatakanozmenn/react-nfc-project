import { useState } from "react";
import { Navigate, useNavigate } from "react-router-dom";

import { successToast, errorToast } from "../utils/toast";

function Login() {
  const navigate = useNavigate();

  const token = localStorage.getItem("token");

  const [formData, setFormData] = useState({
    email: "",
    password: "",
  });

  const [loading, setLoading] = useState(false);

  if (token) {
    return <Navigate to="/" replace />;
  }

  const handleChange = (event) => {
    const { name, value } = event.target;

    setFormData((currentData) => ({
      ...currentData,
      [name]: value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    try {
      setLoading(true);

      const response = await fetch("http://localhost:8080/api/auth/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(formData),
      });

      if (!response.ok) {
        throw new Error("Email veya şifre hatalı.");
      }

      const data = await response.json();

      localStorage.setItem("token", data.token);

      localStorage.setItem(
        "user",
        JSON.stringify({
          id: data.id,
          name: data.name,
          email: data.email,
          role: data.role,
        }),
      );

      successToast("Giriş başarılı.");

      navigate("/");
    } catch (error) {
      console.error("Login hatası:", error);

      errorToast("E-posta veya şifre hatalı.");
    } finally {
      setLoading(false);
    }
  };

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
                <input type="checkbox" />
                <span>Beni hatırla</span>
              </label>

              <button type="button" className="forgot-password">
                Şifremi unuttum
              </button>
            </div>

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
