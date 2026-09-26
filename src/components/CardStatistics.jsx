import { useEffect, useState } from "react";
import { apiJson } from "../services/api";
import StatCard from "./StatCard";

export default function CardStatistics({ detail = true }) {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    apiJson("/api/statistics", { signal: controller.signal })
      .then((data) => { if (!controller.signal.aborted) { setData(data); setError(""); } })
      .catch(() => { if (!controller.signal.aborted) setError("İstatistikler yüklenemedi."); });
    return () => controller.abort();
  }, [attempt]);
  if (error) return <div role="alert"><p>{error}</p><button className="primary-button" onClick={() => setAttempt((value) => value + 1)}>Tekrar Dene</button></div>;
  if (!data) return <p role="status">İstatistikler yükleniyor...</p>;
  return <>
    <div className="stat-cards">
      <StatCard title="Toplam Kart" value={data.totalCards} />
      <StatCard title="Aktif Kart" value={data.activeCards} />
      <StatCard title="Pasif Kart" value={data.inactiveCards} />
      <StatCard title="Toplam Okutma" value={data.totalScans} />
    </div>
    {detail && <div className="table-container statistics-table">
      <table className="cards-table"><thead><tr><th>Kart</th><th>Kod</th><th>Durum</th><th>Toplam Okutma</th></tr></thead>
        <tbody>{data.cards.map((card) => <tr key={card.id}><td>{card.name}</td><td>{card.code}</td><td>{card.active ? "Aktif" : "Pasif"}</td><td>{card.scans ?? 0}</td></tr>)}
          {data.cards.length === 0 && <tr><td colSpan={4}>Henüz görüntülenecek kart yok.</td></tr>}
        </tbody>
      </table>
    </div>}
  </>;
}
