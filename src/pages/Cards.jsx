import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import NfcCardRow from "../components/NfcCardRow";

function Cards() {
  const [cards, setCards] = useState([]);

  useEffect(() => {
    fetch("http://localhost:8080/api/cards")
      .then((response) => response.json())
      .then((data) => {
        console.log("Spring Boot verisi:", data);
        setCards(data);
      })
      .catch((error) => {
        console.error("Kartlar alınamadı:", error);
      });
  }, []);

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
