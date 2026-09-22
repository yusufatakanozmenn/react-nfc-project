import "./css/App.css";

import { Routes, Route } from "react-router-dom";

import Header from "./components/Header";
import Sidebar from "./components/Sidebar";

import Dashboard from "./pages/Dashboard";
import Cards from "./pages/Cards";
import NewCard from "./pages/NewCard";
import Statistics from "./pages/Statistics";
import Settings from "./pages/Settings";

function App() {
  return (
    <div className="App">
      <Header />

      <div className="main-content">
        <Sidebar />

        <main className="content">
          <Routes>
            <Route path="/" element={<Dashboard />} />

            <Route path="/cards" element={<Cards />} />

            <Route path="/cards/new" element={<NewCard />} />

            <Route path="/statistics" element={<Statistics />} />

            <Route path="/settings" element={<Settings />} />
          </Routes>
        </main>
      </div>
    </div>
  );
}

export default App;
