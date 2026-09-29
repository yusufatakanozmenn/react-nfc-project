import DestinationField from "../components/DestinationField";
import OwnerSelect from "../components/OwnerSelect";
import { apiJson } from "../services/api";
import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { successToast, errorToast } from "../utils/toast";
function NewCard() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [formData, setFormData] = useState({
    name: "",
    type: "",
    destinationUrl: "",
    ownerId: /^\d+$/.test(searchParams.get("ownerId") ?? "") ? searchParams.get("ownerId") : "",
  });


  const handleChange = (event) => {
    const { name, value } = event.target;

    setFormData({
      ...formData,
      [name]: value,
      ...(name === "type" ? { destinationUrl: "" } : {}),
    });
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    try {
      await apiJson("/api/cards", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ ...formData, ownerId: formData.ownerId === "" ? null : Number(formData.ownerId) }),
      });

      successToast("Kart başarıyla oluşturuldu.");

      navigate("/cards");
    } catch (error) {
      console.error("Kart oluşturma hatası:", error);

      errorToast(error.message);
    }
  };

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Yeni Kart</h1>
          <p>Yeni NFC kartınızı oluşturun.</p>
        </div>
      </div>

      <div className="form-container">
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="name">Kart Adı</label>

            <input
              id="name"
              name="name"
              type="text"
              placeholder="Örn: Miesha Google Yorum"
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
              <option value="">Kart türü seçin</option>
              <option value="google">Google Yorum</option>
              <option value="instagram">Instagram</option>
              <option value="whatsapp">WhatsApp</option>
              <option value="website">Web Sitesi</option>
            </select>
          </div>

          <DestinationField key={formData.type} type={formData.type} value={formData.destinationUrl}
            onChange={destinationUrl => setFormData(current => ({ ...current, destinationUrl }))} />

          <OwnerSelect value={formData.ownerId} onChange={(ownerId) => setFormData((current) => ({ ...current, ownerId }))} />
          <button type="submit" className="primary-button">
            Kartı Oluştur
          </button>
        </form>


      </div>
    </>
  );
}

export default NewCard;
