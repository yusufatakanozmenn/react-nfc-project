import { useState } from "react";
import Modal from "./Modal";
import { API_URL } from "../services/api";
import { nfcLink, safeDestination } from "../utils/nfc";

export default function CardShare({ card, onClose }) {
  const [notice, setNotice] = useState("");
  const link = nfcLink(card.code, import.meta.env.VITE_NFC_BASE_URL ?? API_URL, window.location.origin);
  const destination = safeDestination(card.destinationUrl);
  const copy = async () => {
    try { await navigator.clipboard.writeText(link); setNotice("NFC bağlantısı kopyalandı."); }
    catch { setNotice("Kopyalama izni alınamadı. Aşağıdaki bağlantıyı seçip kopyalayabilirsiniz."); }
  };
  return <Modal title="NFC kart bağlantısı" onClose={onClose}>
    <div className="share-card"><img src="/webonix-mark.svg" width="32" height="32" alt="" /><span>{card.name}<small>{card.code}</small></span><span className={`status ${card.active ? "active-status" : "passive-status"}`}>{card.active ? "Aktif" : "Pasif"}</span></div>
    {!card.active && <p className="notice-warning">Bu kart pasif. Bağlantının çalışması için kartı aktif hâle getirin.</p>}
    {link ? <>
      <label className="share-label" htmlFor="nfc-url">Karta yazılacak sabit bağlantı</label>
      <input id="nfc-url" className="share-url" readOnly value={link} onFocus={event => event.target.select()} />
      <div className="modal-actions"><button className="primary-button" onClick={copy}>Bağlantıyı kopyala</button></div>
      {notice && <p className="field-hint" role="status">{notice}</p>}
      <div className="nfc-instructions"><h3>Kartı kullanıma hazırlayın</h3><ol><li>NFC yazma uygulamanızda “URL / bağlantı” kaydı seçin.</li><li>Yukarıdaki sabit bağlantıyı NFC karta yazın.</li><li>Telefonunuzu karta yaklaştırarak bağlantıyı deneyin.</li></ol><p>Hedef adresi panelden değiştirseniz de karttaki bağlantı aynı kalır.</p></div>
      <a className="edit-button" href={link} target="_blank" rel="noopener noreferrer">Kartı test et ↗</a><p className="field-hint">Test açılışı okutma sayısına eklenebilir.</p>
    </> : <p role="alert" className="form-error">Bu kartın NFC kodu geçersiz.</p>}
    {destination && <p className="field-hint">Hedef: <a href={destination} target="_blank" rel="noopener noreferrer">{destination}</a></p>}
  </Modal>;
}
