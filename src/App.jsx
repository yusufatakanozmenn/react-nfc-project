import { Routes, Route } from "react-router-dom";

import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import Cards from "./pages/Cards";
import NewCard from "./pages/NewCard";
import EditCard from "./pages/EditCard";
import Statistics from "./pages/Statistics";
import Settings from "./pages/Settings";

import ProtectedLayout from "./components/ProtectedLayout";

import "./css/App.css";

function App() {
  return (
    <Routes>
      {/* Tamamen bağımsız login */}
      <Route path="/login" element={<Login />} />

      {/* Giriş yapılması gereken alanlar */}
      <Route element={<ProtectedLayout />}>
        <Route path="/" element={<Dashboard />} />
        <Route path="/cards" element={<Cards />} />
        <Route path="/cards/new" element={<NewCard />} />
        <Route path="/cards/edit/:id" element={<EditCard />} />
        <Route path="/statistics" element={<Statistics />} />
        <Route path="/settings" element={<Settings />} />
      </Route>
    </Routes>
  );
}

export default App;
