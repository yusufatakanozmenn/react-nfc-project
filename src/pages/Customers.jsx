import Modal from "../components/Modal";
import { deleteCustomer } from "../services/customers";
import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { apiJson } from "../services/api";
import { successToast } from "../utils/toast";

const emptyForm = { name: "", email: "", password: "", confirmPassword: "" };

export default function Customers() {
  const [search, setSearch] = useState("");
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState("");
  const [customers, setCustomers] = useState([]);
  const [cards, setCards] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [selectedId, setSelectedId] = useState(null);
  const [cardId, setCardId] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [assigning, setAssigning] = useState(false);
  const [loadError, setLoadError] = useState("");
  const [formError, setFormError] = useState("");
  const [assignError, setAssignError] = useState("");
  const [attempt, setAttempt] = useState(0);
  const request = useRef(null);
  const mounted = useRef(false);

  useEffect(() => {
    mounted.current = true;
    return () => { mounted.current = false; request.current?.abort(); };
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    request.current = controller;
    Promise.all([
      apiJson("/api/admin/customers", { signal: controller.signal }),
      apiJson("/api/cards", { signal: controller.signal }),
    ]).then(([accounts, allCards]) => {
      if (controller.signal.aborted) return;
      setCustomers(accounts); setCards(allCards); setLoadError("");
    }).catch(() => {
      if (!controller.signal.aborted) setLoadError("Müşteriler ve kartlar yüklenemedi.");
    }).finally(() => {
      if (!controller.signal.aborted) setLoading(false);
    });
    return () => controller.abort();
  }, [attempt]);

  const selected = customers.find((customer) => customer.id === selectedId);
  const ownedCards = cards.filter((card) => card.ownerId === selectedId);
  const availableCards = cards.filter((card) => card.ownerId == null);
  const change = (event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }));

  const createCustomer = async (event) => {
    event.preventDefault();
    if (saving || loading || loadError) return;
    setFormError("");
    if (form.password !== form.confirmPassword) { setFormError("Şifreler eşleşmiyor."); return; }
    if (new TextEncoder().encode(form.password).length > 72) { setFormError("Şifre en fazla 72 bayt olabilir."); return; }
    setSaving(true);
    try {
      const customer = await apiJson("/api/admin/customers", {
        method: "POST", body: JSON.stringify({ name: form.name, email: form.email, password: form.password }),
      });
      if (!mounted.current) return;
      setCustomers((current) => [...current, customer].sort((a, b) => a.name.localeCompare(b.name, "tr")));
      setSelectedId(customer.id); setCardId(""); setAssignError(""); setForm(emptyForm);
      successToast(customer.welcomeEmailStatus === "sent"
        ? "Müşteri oluşturuldu ve hoş geldiniz e-postası gönderildi."
        : "Müşteri oluşturuldu. Şimdi kart bağlayabilirsiniz.");
      if (["failed", "disabled"].includes(customer.welcomeEmailStatus)) {
        setFormError("Müşteri kaydı tamamlandı ancak hoş geldiniz e-postası gönderilemedi. Müşteriyi yeniden oluşturmayın; SMTP ayarlarını kontrol edin.");
      }
    } catch (error) {
      if (mounted.current) setFormError(error.message);
      return;
    } finally { if (mounted.current) setSaving(false); }
  };

  const assignCard = async (event) => {
    event.preventDefault();
    if (assigning || !cardId || !selected?.active) return;
    setAssigning(true); setAssignError("");
    try {
      const updated = await apiJson(`/api/cards/${cardId}/owner`, {
        method: "PUT", body: JSON.stringify({ ownerId: selected.id }),
      });
      if (!mounted.current) return;
      setCards((current) => current.map((card) => card.id === updated.id ? updated : card));
      setCardId("");
    } catch (error) {
      if (mounted.current) setAssignError(error.message);
      return;
    } finally { if (mounted.current) setAssigning(false); }
    successToast("Kart müşteriye bağlandı.");
  };

  const filteredCustomers = customers.filter(customer => `${customer.name} ${customer.email}`.toLocaleLowerCase("tr").includes(search.trim().toLocaleLowerCase("tr")));
  const targetCards = cards.filter(card => card.ownerId === deleteTarget?.id);
  const removeCustomer = async () => {
    if (!deleteTarget || deleting || targetCards.length) return;
    setDeleting(true); setDeleteError("");
    try {
      await deleteCustomer(deleteTarget.id);
      if (!mounted.current) return;
      setCustomers(current => current.filter(customer => customer.id !== deleteTarget.id));
      if (selectedId === deleteTarget.id) setSelectedId(null);
      setDeleteTarget(null);
      successToast("Müşteri silindi.");
    } catch (error) { if (mounted.current) setDeleteError(error.message); }
    finally { if (mounted.current) setDeleting(false); }
  };

  return <>
    <div className="page-header"><div><h1>Müşteriler</h1><p>Müşteri hesabı oluşturun ve NFC kartlarını bağlayın.</p></div></div>
    {loading && <p role="status">Müşteriler yükleniyor...</p>}
    {loadError && <div className="form-error" role="alert">{loadError} <button type="button" className="edit-button" onClick={() => { setLoading(true); setAttempt((value) => value + 1); }}>Tekrar Dene</button></div>}
    <div className="customer-grid">
      <section className="form-container customer-create" aria-labelledby="create-customer-title">
        <h2 id="create-customer-title">Yeni Müşteri</h2>
        <p className="field-hint">Müşteri bu e-posta ve şifreyle kendi paneline giriş yapabilir.</p>
        <form onSubmit={createCustomer}>
          <fieldset disabled={saving || loading || Boolean(loadError)}>
            <div className="form-group"><label htmlFor="customer-name">Müşteri / işletme adı</label>
              <input id="customer-name" name="name" value={form.name} onChange={change} maxLength={100} autoComplete="off" required />
            </div>
            <div className="form-group"><label htmlFor="customer-email">E-posta</label>
              <input id="customer-email" name="email" type="email" value={form.email} onChange={change} maxLength={150} autoComplete="off" required />
            </div>
            <div className="form-group"><label htmlFor="customer-password">Giriş şifresi</label>
              <input id="customer-password" name="password" type="password" value={form.password} onChange={change} minLength={6} maxLength={72} autoComplete="new-password" required />
              <p className="field-hint">En az 6 karakter. Şifre kayıt sonrasında gösterilmez.</p>
            </div>
            <div className="form-group"><label htmlFor="customer-confirm">Şifre tekrarı</label>
              <input id="customer-confirm" name="confirmPassword" type="password" value={form.confirmPassword} onChange={change} autoComplete="new-password" required />
            </div>
            {formError && <p role="alert" className="form-error">{formError}</p>}
            <button type="submit" className="primary-button">{saving ? "Oluşturuluyor..." : "Müşteri Oluştur"}</button>
          </fieldset>
        </form>
      </section>
      <section className="customer-list" aria-labelledby="customer-list-title">
        <div className="section-heading"><h2 id="customer-list-title">Kayıtlı Müşteriler <span className="count-badge">{customers.length}</span></h2></div>
        <label className="search-field">Müşteri ara<input type="search" placeholder="İsim veya e-posta…" value={search} onChange={event => setSearch(event.target.value)} /></label>
        <div className="table-container"><table className="cards-table">
          <thead><tr><th>Müşteri</th><th>E-posta</th><th>Kart</th><th>Durum</th><th>İşlem</th></tr></thead>
          <tbody>{filteredCustomers.map((customer) => <tr key={customer.id} className={selectedId === customer.id ? "selected-customer" : ""}>
            <td>{customer.name}</td><td>{customer.email}</td><td>{cards.filter((card) => card.ownerId === customer.id).length}</td>
            <td><span className={customer.active ? "status active-status" : "status passive-status"}>{customer.active ? "Aktif" : "Pasif"}</span></td>
            <td><button type="button" className="edit-button" disabled={assigning || saving} aria-pressed={selectedId === customer.id}
              onClick={() => { setSelectedId(customer.id); setCardId(""); setAssignError(""); }}>Kartları Yönet</button> <button type="button" className="delete-button" aria-label={`${customer.name} müşterisini sil`} disabled={assigning || saving || deleting || loading || Boolean(loadError)} onClick={() => { setDeleteTarget(customer); setDeleteError(""); }}>Sil</button></td>
          </tr>)}
          {!loading && !loadError && filteredCustomers.length === 0 && <tr><td colSpan={5}>{customers.length ? "Aramanızla eşleşen müşteri yok." : "Henüz müşteri eklenmedi."}</td></tr>}
          </tbody>
        </table></div>
        {selected && <section className="form-container customer-cards" aria-labelledby="customer-cards-title">
          <h2 id="customer-cards-title">{selected.name} — Kartları</h2>
          <p className="field-hint">{selected.email}</p>
          {ownedCards.length > 0 ? <ul className="customer-card-list">{ownedCards.map((card) => <li key={card.id}>
            <span>{card.name} <small>({card.code})</small></span><Link className="edit-button" to={`/cards/edit/${card.id}`}>Düzenle</Link>
          </li>)}</ul> : <p>Bu müşteriye henüz kart bağlanmadı.</p>}
          {!selected.active ? <p className="field-hint">Pasif müşteriye yeni kart bağlanamaz.</p> : <>
            {availableCards.length > 0 ? <form onSubmit={assignCard} className="customer-assign-form">
              <div className="form-group"><label htmlFor="customer-card">Bağlanacak kart</label>
                <select id="customer-card" value={cardId} onChange={(event) => setCardId(event.target.value)} disabled={assigning} required>
                  <option value="">Atanmamış kart seçin</option>
                  {availableCards.map((card) => <option key={card.id} value={card.id}>{card.name} ({card.code})</option>)}
                </select>
              </div>
              {assignError && <p role="alert" className="form-error">{assignError}</p>}
              <button className="primary-button" disabled={assigning || !cardId}>{assigning ? "Bağlanıyor..." : "Kartı Müşteriye Bağla"}</button>
            </form> : <p className="field-hint">Bağlanabilecek atanmamış kart yok.</p>}
            <Link className="edit-button customer-new-card" to={`/cards/new?ownerId=${selected.id}`}>Bu Müşteri İçin Yeni Kart</Link>
          </>}
        </section>}
      </section>
    </div>
    {deleteTarget && <Modal title="Müşteriyi sil" onClose={() => setDeleteTarget(null)} busy={deleting}>
      <p><strong>{deleteTarget.name}</strong> ({deleteTarget.email}) hesabını silmek istiyor musunuz?</p>
      {targetCards.length ? <p className="notice-warning">Bu müşteriye bağlı {targetCards.length} kart var. Önce kartları başka bir müşteriye atayın veya kart düzenleme ekranından sahip atamasını kaldırın.</p> : <p className="field-hint">Bu işlem geri alınamaz. Müşteri artık hesabına giriş yapamaz.</p>}
      {deleteError && <p className="form-error" role="alert">{deleteError}</p>}
      <div className="modal-actions"><button className="edit-button" onClick={() => setDeleteTarget(null)} disabled={deleting}>Vazgeç</button><button className="delete-button" onClick={removeCustomer} disabled={deleting || targetCards.length > 0}>{deleting ? "Siliniyor…" : "Müşteriyi sil"}</button></div>
    </Modal>}
  </>;
}
