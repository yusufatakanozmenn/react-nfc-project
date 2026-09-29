import Brand from "./Brand";
import { Link } from "react-router-dom";

export default function RecoveryLayout({ title, description, children }) {
  return <div className="auth-page">
    <div className="auth-background"><div className="auth-decoration auth-decoration-one" /><div className="auth-decoration auth-decoration-two" /></div>
    <div className="auth-container">
      <div className="auth-brand"><Brand light /></div>
      <div className="auth-card">
        <div className="auth-card-header"><span className="auth-badge">Hesap güvenliği</span><h2>{title}</h2><p>{description}</p></div>
        {children}
        <div className="auth-recovery-back"><Link to="/login">Giriş ekranına dön</Link></div>
        <div className="auth-footer">© 2026 Webonix Tap</div>
      </div>
    </div>
  </div>;
}
