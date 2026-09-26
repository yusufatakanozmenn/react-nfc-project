import { logout } from "../auth/session";
import { NavLink } from "react-router-dom";

import { useAuth } from "../auth/useAuth";
import { isAdmin } from "../auth/permissions";

function Sidebar() {
  const { user } = useAuth();
  const admin = isAdmin(user);
  return (
    <aside className="sidebar">
      <nav>
        <ul>
          {admin && <li><NavLink to="/" end>Dashboard</NavLink></li>}

          <li>
            <NavLink to="/cards">{admin ? "Tüm Kartlar" : "Kartlarım"}</NavLink>
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
      <button className="sidebar-logout" onClick={logout}>Çıkış Yap</button>
    </aside>
  );
}

export default Sidebar;
