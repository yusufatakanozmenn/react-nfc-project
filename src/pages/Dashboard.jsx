import CardStatistics from "../components/CardStatistics";
export default function Dashboard() {
  return <>
    <div className="page-header"><div><h1>Dashboard</h1><p>Tüm NFC kartlarına genel bakış.</p></div></div>
    <CardStatistics detail={false} />
  </>;
}
