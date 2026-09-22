import { Link } from "react-router-dom";

function NfcCardRow({ card, onDelete, onToggleStatus }) {
  return (
    <tr>
      <td>{card.name}</td>
      <td>{card.type}</td>
      <td>{card.code}</td>
      <td>{card.scans}</td>

      <td>
        <span
          className={
            card.active ? "status active-status" : "status passive-status"
          }
        >
          {card.active ? "Aktif" : "Pasif"}
        </span>
      </td>

      <td className="action-buttons">
        <Link to={`/cards/edit/${card.id}`} className="edit-button">
          Düzenle
        </Link>

        <button
          className="status-button"
          onClick={() => onToggleStatus(card.id)}
        >
          {card.active ? "Pasife Al" : "Aktif Et"}
        </button>

        <button className="delete-button" onClick={() => onDelete(card.id)}>
          Sil
        </button>
      </td>
    </tr>
  );
}

export default NfcCardRow;
