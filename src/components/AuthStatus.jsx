import { errorToast } from "../utils/toast";
import { restoreSession, logout } from "../auth/session";

export default function AuthStatus({ status }) {
  return (
    <div className="auth-checking" role="status">
      {status === "checking" ? <>
        <div className="auth-checking-spinner" />
        <p>Oturum kontrol ediliyor...</p>
      </> : <>
        <p>Oturum doğrulanamadı. Sunucu bağlantısını kontrol edip tekrar deneyin.</p>
        <button className="primary-button" onClick={restoreSession}>Tekrar Dene</button>
        <button className="edit-button" onClick={() => logout().catch(() => errorToast("Çıkış tamamlanamadı. Sunucuya bağlanıp tekrar deneyin."))}>Giriş ekranına dön</button>
      </>}
    </div>
  );
}
