import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import NfcCardRow from "../components/NfcCardRow";
import { successToast, errorToast } from "../utils/toast";
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
  const handleDelete = async (id) => {
    try {
      const response = await fetch(`http://localhost:8080/api/cards/${id}`, {
        method: "DELETE",
      });

      if (!response.ok) {
        throw new Error("Kart silinemedi.");
      }

      setCards((currentCards) => currentCards.filter((card) => card.id !== id));

      successToast("Kart başarıyla silindi.");
    } catch (error) {
      console.error("Silme hatası:", error);

      errorToast("Kart silinirken bir hata oluştu.");
    }
  };
  const handleToggleStatus = async (id) => {
    try {
      const response = await fetch(
        `http://localhost:8080/api/cards/${id}/status`,
        {
          method: "PATCH",
        },
      );

      if (!response.ok) {
        throw new Error("Kart durumu değiştirilemedi.");
      }

      const updatedCard = await response.json();

      setCards((currentCards) =>
        currentCards.map((card) => (card.id === id ? updatedCard : card)),
      );

      if (updatedCard.active) {
        successToast("Kart aktif hale getirildi.");
      } else {
        successToast("Kart pasife alındı.");
      }
    } catch (error) {
      console.error("Durum değiştirme hatası:", error);

      errorToast("Kart durumu değiştirilirken hata oluştu.");
    }
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
