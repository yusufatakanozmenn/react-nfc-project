function NfcCardRow({ card }) {
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

      <td>
        <button className="edit-button">Düzenle</button>
      </td>
    </tr>
  );
}

export default NfcCardRow;
