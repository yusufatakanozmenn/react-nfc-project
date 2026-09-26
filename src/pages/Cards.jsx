import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import NfcCardRow from "../components/NfcCardRow";
import { successToast, errorToast } from "../utils/toast";
import { apiFetch } from "../services/api";

import { useAuth } from "../auth/useAuth";
import { isAdmin } from "../auth/permissions";

function Cards() {
  const { user } = useAuth();
  const admin = isAdmin(user);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [cards, setCards] = useState([]);

  useEffect(() => {
    const controller = new AbortController();
    const getCards = async () => {
      try {
        const response = await apiFetch("/api/cards", { signal: controller.signal });

        if (!response.ok) {
          throw new Error(`Kartlar alınamadı. HTTP: ${response.status}`);
        }

        const data = await response.json();

        if (!controller.signal.aborted) setCards(data);
      } catch (error) {
        if (controller.signal.aborted) return;
        console.error("Kartlar alınamadı:", error);
        setError("Kartlar yüklenemedi. Lütfen sayfayı yenileyerek tekrar deneyin.");
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    };

    getCards();
    return () => controller.abort();
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
          <h1>{admin ? "Tüm Kartlar" : "Kartlarım"}</h1>
          <p>NFC kartlarınızı buradan yönetebilirsiniz.</p>
        </div>

        {admin && <Link to="/cards/new" className="primary-button">
          + Yeni Kart
        </Link>}
      </div>

      {loading && <p role="status">Kartlar yükleniyor...</p>}
      {error && <p className="form-error" role="alert">{error}</p>}
      <div className="table-container">
        <table className="cards-table">
          <thead>
            <tr>
              <th>Kart Adı</th>
              <th>Tür</th>
              <th>Kod</th>
              {admin && <th>Kart Sahibi</th>}
              <th>Hedef Bağlantı</th>
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
                admin={admin}
                onDelete={handleDelete}
                onToggleStatus={handleToggleStatus}
              />
            ))}
            {!loading && !error && cards.length === 0 && <tr><td colSpan={admin ? 8 : 7}>Henüz {admin ? "kart oluşturulmadı" : "size atanmış bir kart yok"}.</td></tr>}
          </tbody>
        </table>
      </div>
    </>
  );
}

export default Cards;
