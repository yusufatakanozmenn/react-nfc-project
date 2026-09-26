import { useEffect } from "react";
import { initializeAuth } from "./auth/session";
import { Routes, Route, Navigate } from "react-router-dom";

import Login from "./pages/Login";
import Customers from "./pages/Customers";
import Dashboard from "./pages/Dashboard";
import Cards from "./pages/Cards";
import NewCard from "./pages/NewCard";
import EditCard from "./pages/EditCard";
import Statistics from "./pages/Statistics";
import Settings from "./pages/Settings";

import AdminRoute from "./components/AdminRoute";
import ProtectedLayout from "./components/ProtectedLayout";

import "./css/App.css";

function App() {
  useEffect(initializeAuth, []);
  return (
    <Routes>
      {/* Herkese açık, bağımsız login ekranı */}
      <Route path="/login" element={<Login />} />

      {/* Bütün yönetim sayfaları korumalı */}
      <Route element={<ProtectedLayout />}>
        <Route element={<AdminRoute />}>
          <Route path="/" element={<Dashboard />} />
          <Route path="/customers" element={<Customers />} />
          <Route path="/cards/new" element={<NewCard />} />
        </Route>

        <Route path="/cards" element={<Cards />} />

        <Route path="/cards/edit/:id" element={<EditCard />} />

        <Route path="/statistics" element={<Statistics />} />

        <Route path="/settings" element={<Settings />} />
      </Route>

      {/* Bilinmeyen adresleri başlangıca gönder */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
