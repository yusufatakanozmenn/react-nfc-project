import { Link } from "react-router-dom";
import { cardTypes } from "../utils/nfc";

export default function NfcCardRow({ card, admin, onDelete, onToggleStatus, onShare, busy }) {
  return <tr>
    <td data-label="Kart"><strong>{card.name}</strong></td>
    <td data-label="Tür">{cardTypes[card.type] ?? card.type}</td>
    <td data-label="Kod"><code>{card.code}</code></td>
    {admin && <td data-label="Sahibi">{card.ownerName ?? "Atanmamış"}</td>}
    <td data-label="Hedef" className="card-destination">{card.destinationUrl}</td>
    <td data-label="Okutma">{Number(card.scans ?? 0).toLocaleString("tr-TR")}</td>
    <td data-label="Durum"><span className={`status ${card.active ? "active-status" : "passive-status"}`}>{card.active ? "Aktif" : "Pasif"}</span></td>
    <td className="action-buttons">
      <button className="share-button" onClick={() => onShare(card)} disabled={busy}>NFC bağlantısı</button>
      <Link to={`/cards/edit/${card.id}`} className="edit-button">Düzenle</Link>
      <button className="status-button" onClick={() => onToggleStatus(card.id)} disabled={busy}>{card.active ? "Pasife al" : "Aktif et"}</button>
      {admin && <button className="delete-button" onClick={() => onDelete(card)} disabled={busy}>Sil</button>}
    </td>
  </tr>;
}
