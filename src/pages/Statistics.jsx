import CardStatistics from "../components/CardStatistics";
import { useAuth } from "../auth/useAuth";
import { isAdmin } from "../auth/permissions";
export default function Statistics() {
  const { user } = useAuth();
  return <>
    <div className="page-header"><div><h1>İstatistikler</h1><p>{isAdmin(user) ? "Tüm kartların" : "Size atanmış kartların"} toplam okutma bilgileri.</p></div></div>
    <CardStatistics />
  </>;
}
