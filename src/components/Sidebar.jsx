import { useState } from "react";
import { errorToast } from "../utils/toast";
import { logout } from "../auth/session";
import { NavLink } from "react-router-dom";

import { useAuth } from "../auth/useAuth";
import { isAdmin } from "../auth/permissions";

function Sidebar() {
  const { user } = useAuth();
  const [loggingOut, setLoggingOut] = useState(false);
  const handleLogout = async () => {
    setLoggingOut(true);
    try { await logout(); }
    catch { errorToast("Çıkış tamamlanamadı. Bağlantınızı kontrol edip tekrar deneyin."); }
    finally { setLoggingOut(false); }
  };
  const admin = isAdmin(user);
  return (
    <aside className="sidebar">
      <nav aria-label="Ana menü">
        <ul>
          {admin && <li><NavLink to="/" end>Genel Bakış</NavLink></li>}

          <li>
            <NavLink to="/cards" end>{admin ? "Tüm Kartlar" : "Kartlarım"}</NavLink>
          </li>

          {admin && <li><NavLink to="/customers">Müşteriler</NavLink></li>}

          {admin && <li><NavLink to="/cards/new">Yeni Kart</NavLink></li>}

          <li>
            <NavLink to="/statistics">İstatistikler</NavLink>
          </li>

          <li>
            <NavLink to="/settings">Ayarlar</NavLink>
          </li>
        </ul>
      </nav>
      <button className="sidebar-logout" onClick={handleLogout} disabled={loggingOut}>{loggingOut ? "Çıkış yapılıyor…" : "Çıkış Yap"}</button>
    </aside>
  );
}

export default Sidebar;
