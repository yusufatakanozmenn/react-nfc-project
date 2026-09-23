import { Navigate, Outlet } from "react-router-dom";
import Header from "./Header";
import Sidebar from "./Sidebar";

function ProtectedLayout() {
  const token = localStorage.getItem("token");

  if (!token) {
    return <Navigate to="/login" replace />;
  }

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
