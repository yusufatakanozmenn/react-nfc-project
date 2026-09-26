import { Navigate, Outlet } from "react-router-dom";
import Header from "./Header";
import Sidebar from "./Sidebar";
import AuthStatus from "./AuthStatus";
import { useAuth } from "../auth/useAuth";

function ProtectedLayout() {
  const { status } = useAuth();
  if (status === "guest") return <Navigate to="/login" replace />;
  if (status !== "authenticated") return <AuthStatus status={status} />;

  // Kullanıcı doğrulandı, paneli göster
  return (
    <div className="panel-layout">
      <Header />

      <div className="panel-body">
        <Sidebar />

        <main className="panel-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export default ProtectedLayout;
