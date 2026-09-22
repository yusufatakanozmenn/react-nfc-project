import { NavLink } from "react-router-dom";

function Sidebar() {
  return (
    <aside className="sidebar">
      <nav>
        <ul>
          <li>
            <NavLink to="/">Dashboard</NavLink>
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
    </aside>
  );
}

export default Sidebar;
