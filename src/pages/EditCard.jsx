import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import { successToast, errorToast } from "../utils/toast";

function EditCard() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    name: "",
    type: "",
    destinationUrl: "",
  });

  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const getCard = async () => {
      try {
        const response = await fetch(`http://localhost:8080/api/cards/${id}`);

        if (!response.ok) {
          throw new Error("Kart bulunamadı.");
        }

        const card = await response.json();

        setFormData({
          name: card.name,
          type: card.type,
          destinationUrl: card.destinationUrl,
        });
      } catch (error) {
        console.error("Kart getirme hatası:", error);

        errorToast("Kart bilgileri alınamadı.");

        navigate("/cards");
      } finally {
        setLoading(false);
      }
    };

    getCard();
  }, [id, navigate]);

  const handleChange = (event) => {
    const { name, value } = event.target;

    setFormData((currentData) => ({
      ...currentData,
      [name]: value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    try {
      const response = await fetch(`http://localhost:8080/api/cards/${id}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(formData),
      });

      if (!response.ok) {
        throw new Error("Kart güncellenemedi.");
      }

      const updatedCard = await response.json();

      console.log("Güncellenen kart:", updatedCard);

      successToast("Kart başarıyla güncellendi.");

      navigate("/cards");
    } catch (error) {
      console.error("Kart güncelleme hatası:", error);

      errorToast("Kart güncellenirken hata oluştu.");
    }
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

          <div className="form-group">
            <label htmlFor="destinationUrl">Hedef URL</label>

            <input
              id="destinationUrl"
              type="url"
              name="destinationUrl"
              value={formData.destinationUrl}
              onChange={handleChange}
              required
            />
          </div>

          <button type="submit" className="primary-button">
            Değişiklikleri Kaydet
          </button>
        </form>
      </div>
    </>
  );
}

export default EditCard;
