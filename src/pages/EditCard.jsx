import DestinationField from "../components/DestinationField";
import OwnerSelect from "../components/OwnerSelect";
import { useAuth } from "../auth/useAuth";
import { isAdmin } from "../auth/permissions";
import { apiFetch, apiJson } from "../services/api";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import { successToast, errorToast } from "../utils/toast";

function EditCard() {
  const { user } = useAuth();
  const admin = isAdmin(user);
  const [ownerId, setOwnerId] = useState("");
  const [assigning, setAssigning] = useState(false);
  const { id } = useParams();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    name: "",
    type: "",
    destinationUrl: "",
  });

  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const controller = new AbortController();
    const getCard = async () => {
      try {
        const response = await apiFetch(`/api/cards/${id}`, { signal: controller.signal });

        if (!response.ok) {
          throw new Error("Kart bulunamadı.");
        }

        const card = await response.json();
        if (controller.signal.aborted) return;

        setOwnerId(card.ownerId ?? "");
        setFormData({
          name: card.name,
          type: card.type,
          destinationUrl: card.destinationUrl,
        });
      } catch (error) {
        if (controller.signal.aborted) return;
        console.error("Kart getirme hatası:", error);

        errorToast("Kart bilgileri alınamadı.");

        navigate("/cards");
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    };

    getCard();
    return () => controller.abort();
  }, [id, navigate]);

  const handleChange = (event) => {
    const { name, value } = event.target;

    setFormData((currentData) => ({
      ...currentData,
      [name]: value,
      ...(name === "type" ? { destinationUrl: "" } : {}),
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    try {
      await apiJson(`/api/cards/${id}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(formData),
      });

      successToast("Kart başarıyla güncellendi.");

      navigate("/cards");
    } catch (error) {
      console.error("Kart güncelleme hatası:", error);

      errorToast(error.message);
    }
  };

  const assignOwner = async () => {
    if (assigning) return;
    setAssigning(true);
    try {
      await apiJson(`/api/cards/${id}/owner`, {
        method: "PUT", body: JSON.stringify({ ownerId: ownerId === "" ? null : Number(ownerId) }),
      });
      successToast("Kart sahibi güncellendi.");
    } catch (error) { errorToast(error.message); }
    finally { setAssigning(false); }
  };

  if (loading) {
    return <p>Kart bilgileri yükleniyor...</p>;
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Kart Düzenle</h1>
          <p>NFC kart bilgilerini güncelleyin.</p>
        </div>
      </div>

      <div className="form-container">
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="name">Kart Adı</label>

            <input
              id="name"
              type="text"
              name="name"
              value={formData.name}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="type">Kart Türü</label>

            <select
              id="type"
              name="type"
              value={formData.type}
              onChange={handleChange}
              required
            >
              <option value="google">Google Yorum</option>

              <option value="instagram">Instagram</option>

              <option value="whatsapp">WhatsApp</option>

              <option value="website">Web Sitesi</option>
            </select>
          </div>

          <DestinationField key={formData.type} type={formData.type} value={formData.destinationUrl}
            onChange={destinationUrl => setFormData(current => ({ ...current, destinationUrl }))} />

          <button type="submit" className="primary-button">
            Değişiklikleri Kaydet
          </button>
        </form>
        {admin && <div className="owner-assignment">
          <h2>Kullanıcıya Ata</h2>
          <OwnerSelect value={ownerId} onChange={setOwnerId} disabled={assigning} />
          <button type="button" className="primary-button" onClick={assignOwner} disabled={assigning}>
            {assigning ? "Kaydediliyor..." : "Kart Sahibini Kaydet"}
          </button>
        </div>}
      </div>
    </>
  );
}

export default EditCard;
