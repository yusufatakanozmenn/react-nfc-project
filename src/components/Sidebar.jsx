import { logout } from "../auth/session";
import { NavLink } from "react-router-dom";

function Sidebar() {
  return (
    <aside className="sidebar">
      <nav>
        <ul>
          <li>
            <NavLink to="/" end>Dashboard</NavLink>
          </li>

          <li>
            <NavLink to="/cards">Kartlarım</NavLink>
          </li>

          <li>
            <NavLink to="/cards/new">Yeni Kart</NavLink>
          </li>

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
