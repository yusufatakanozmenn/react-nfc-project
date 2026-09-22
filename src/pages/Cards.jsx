import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import NfcCardRow from "../components/NfcCardRow";

function Cards() {
  const [cards, setCards] = useState([]);

  useEffect(() => {
    const savedCards = JSON.parse(localStorage.getItem("nfcCards")) || [];

    setCards(savedCards);
  }, []);

  const saveCards = (updatedCards) => {
    setCards(updatedCards);

    localStorage.setItem("nfcCards", JSON.stringify(updatedCards));
  };

  const handleDelete = (id) => {
    const updatedCards = cards.filter((card) => card.id !== id);

    saveCards(updatedCards);
  };

  const handleToggleStatus = (id) => {
    const updatedCards = cards.map((card) =>
      card.id === id ? { ...card, active: !card.active } : card,
    );

    saveCards(updatedCards);
  };
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
              <NfcCardRow
                key={card.id}
                card={card}
                onDelete={handleDelete}
                onToggleStatus={handleToggleStatus}
              />
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}

export default Cards;
