import { Link } from "react-router-dom";
import NfcCardRow from "../components/NfcCardRow";

function Cards() {
  const cards = [
    {
      id: 1,
      name: "Webonix Google Yorum",
      type: "Google Yorum",
      code: "A8K2XP",
      scans: 1284,
      active: true,
    },
    {
      id: 2,
      name: "Webonix Instagram",
      type: "Instagram",
      code: "B7X92M",
      scans: 542,
      active: true,
    },
    {
      id: 3,
      name: "Webonix WhatsApp",
      type: "WhatsApp",
      code: "C2P81N",
      scans: 92,
      active: false,
    },
  ];

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Kartlarım</h1>
          <p>NFC kartlarınızı buradan yönetebilirsiniz.</p>
        </div>

        <Link to="/cards/new" className="primary-button">
          + Yeni Kart
        </Link>
      </div>

      <div className="table-container">
        <table className="cards-table">
          <thead>
            <tr>
              <th>Kart Adı</th>
              <th>Tür</th>
              <th>Kod</th>
              <th>Okutma</th>
              <th>Durum</th>
              <th>İşlem</th>
            </tr>
          </thead>

          <tbody>
            {cards.map((card) => (
              <NfcCardRow key={card.id} card={card} />
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}

export default Cards;
