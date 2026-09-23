import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import NfcCardRow from "../components/NfcCardRow";
import { successToast, errorToast } from "../utils/toast";
import { apiFetch } from "../services/api";

function Cards() {
  const [cards, setCards] = useState([]);

  useEffect(() => {
    const getCards = async () => {
      try {
        const response = await apiFetch("/api/cards");

        if (!response.ok) {
          throw new Error(`Kartlar alınamadı. HTTP: ${response.status}`);
        }

        const data = await response.json();

        console.log("Spring Boot verisi:", data);

        setCards(data);
      } catch (error) {
        console.error("Kartlar alınamadı:", error);
        errorToast("Kartlar alınırken bir hata oluştu.");
      }
    };

    getCards();
  }, []);

  const handleDelete = async (id) => {
    try {
      const response = await apiFetch(`/api/cards/${id}`, {
        method: "DELETE",
      });

      if (!response.ok) {
        throw new Error(`Kart silinemedi. HTTP: ${response.status}`);
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
      const response = await apiFetch(`/api/cards/${id}/status`, {
        method: "PATCH",
      });

      if (!response.ok) {
        throw new Error(
          `Kart durumu değiştirilemedi. HTTP: ${response.status}`,
        );
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
