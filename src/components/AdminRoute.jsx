import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../auth/useAuth";
import { isAdmin } from "../auth/permissions";
export default function AdminRoute() {
  const { user } = useAuth();
  return isAdmin(user) ? <Outlet /> : <Navigate to="/cards" replace />;
}
