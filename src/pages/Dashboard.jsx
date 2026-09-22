import StatCard from "../components/StatCard";

function Dashboard() {
  return (
    <>
      <h1>Dashboard</h1>

      <div className="stat-cards">
        <StatCard title="Toplam Kart" value={12} />
        <StatCard title="Bugünkü Okutma" value={35} />
        <StatCard title="Bu Ay Okutma" value={542} />
        <StatCard title="Toplam Okutma" value={1284} />
      </div>
    </>
  );
}

export default Dashboard;
