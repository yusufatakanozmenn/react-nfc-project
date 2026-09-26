import { useAuth } from "../auth/useAuth";

function Header() {
  const { user } = useAuth();
  return (
    <header className="header">
      <h2>Webonix Tap</h2>

      <div>
        <span>{user.name}</span>
      </div>
    </header>
  );
}

export default Header;
