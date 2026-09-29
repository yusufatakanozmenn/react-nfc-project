import CardShare from "../components/CardShare";
import Modal from "../components/Modal";
import { cardTypes } from "../utils/nfc";
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
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState("all");
  const [type, setType] = useState("all");
  const [sharedCard, setSharedCard] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [busy, setBusy] = useState(false);
  const [deleteError, setDeleteError] = useState("");

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
    if (busy) return;
    setBusy(true); setDeleteError("");
    try {
      const response = await apiFetch(`/api/cards/${id}`, {
        method: "DELETE",
      });

      if (!response.ok) {
        throw new Error(`Kart silinemedi. HTTP: ${response.status}`);
      }

      setCards((currentCards) => currentCards.filter((card) => card.id !== id));

      setDeleteTarget(null);
      successToast("Kart başarıyla silindi.");
    } catch (error) {
      console.error("Silme hatası:", error);

      setDeleteError("Kart silinemedi. Lütfen tekrar deneyin.");
    } finally { setBusy(false); }
  };

  const handleToggleStatus = async (id) => {
    if (busy) return;
    setBusy(true);
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
    } finally { setBusy(false); }
  };

    const visibleCards = cards.filter(card =>
    `${card.name} ${card.code} ${card.ownerName ?? ""}`.toLocaleLowerCase("tr").includes(search.trim().toLocaleLowerCase("tr")) &&
    (filter === "all" || (filter === "active" ? card.active : !card.active)) && (type === "all" || card.type === type));

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

      {!loading && !error && <div className="card-summary"><span><strong>{cards.length}</strong> kart</span><span><strong>{cards.filter(card => card.active).length}</strong> aktif kart</span><span><strong>{cards.reduce((sum, card) => sum + Number(card.scans || 0), 0).toLocaleString("tr-TR")}</strong> toplam okutma</span></div>}
      <div className="list-toolbar"><label className="search-field">Kart ara<input type="search" placeholder="Kart adı, kod veya müşteri…" value={search} onChange={event => setSearch(event.target.value)} /></label>
        <label>Durum<select value={filter} onChange={event => setFilter(event.target.value)}><option value="all">Tüm durumlar</option><option value="active">Aktif</option><option value="passive">Pasif</option></select></label>
        <label>Kart türü<select value={type} onChange={event => setType(event.target.value)}><option value="all">Tüm türler</option>{Object.entries(cardTypes).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
      </div>
      {loading && <p role="status">Kartlar yükleniyor...</p>}
      {error && <p className="form-error" role="alert">{error}</p>}
      <div className="table-container">
        <table className="cards-table responsive-cards">
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
            {visibleCards.map((card) => (
              <NfcCardRow
                key={card.id}
                card={card}
                admin={admin}
                onDelete={card => { setDeleteTarget(card); setDeleteError(""); }}
                onShare={setSharedCard}
                busy={busy}
                onToggleStatus={handleToggleStatus}
              />
            ))}
            {!loading && !error && visibleCards.length === 0 && <tr><td colSpan={admin ? 8 : 7}>{cards.length ? "Filtrelere uygun kart bulunamadı." : admin ? "Henüz kart oluşturulmadı." : "Henüz size atanmış bir kart yok."}</td></tr>}
          </tbody>
        </table>
      </div>
      {sharedCard && <CardShare key={sharedCard.id} card={sharedCard} onClose={() => setSharedCard(null)} />}
      {deleteTarget && <Modal title="Kartı sil" onClose={() => setDeleteTarget(null)} busy={busy}>
        <p><strong>{deleteTarget.name}</strong> kartı kalıcı olarak silinecek. NFC bağlantısı artık çalışmayacak.</p>
        {deleteError && <p className="form-error" role="alert">{deleteError}</p>}
        <div className="modal-actions"><button className="edit-button" disabled={busy} onClick={() => setDeleteTarget(null)}>Vazgeç</button><button className="delete-button" disabled={busy} onClick={() => handleDelete(deleteTarget.id)}>{busy ? "Siliniyor…" : "Kartı sil"}</button></div>
      </Modal>}
    </>
  );
}

export default Cards;
