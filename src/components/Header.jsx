import { Link } from "react-router-dom";
import { useAuth } from "../auth/useAuth";
import { homePath } from "../auth/permissions";
import Brand from "./Brand";
export default function Header() {
  const { user } = useAuth();
  return <header className="header"><Link className="brand-link" to={homePath(user)} aria-label="Webonix Tap ana sayfa"><Brand /></Link>
    <Link className="account-link" to="/settings"><span className="account-avatar" aria-hidden="true">{user.name.slice(0, 1).toLocaleUpperCase("tr")}</span><span>{user.name}<small>{user.role === "ADMIN" ? "Yönetici hesabı" : "Müşteri hesabı"}</small></span></Link>
  </header>;
}
