import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

function EditCard() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    name: "",
    type: "",
    destinationUrl: "",
  });

  useEffect(() => {
    const cards = JSON.parse(localStorage.getItem("nfcCards")) || [];

    const card = cards.find((card) => card.id === Number(id));

    if (!card) {
      navigate("/cards");
      return;
    }

    setFormData({
      name: card.name,
      type: card.type,
      destinationUrl: card.destinationUrl,
    });
  }, [id, navigate]);

  const handleChange = (event) => {
    const { name, value } = event.target;

    setFormData({
      ...formData,
      [name]: value,
    });
  };

  const handleSubmit = (event) => {
    event.preventDefault();

    const cards = JSON.parse(localStorage.getItem("nfcCards")) || [];

    const updatedCards = cards.map((card) =>
      card.id === Number(id)
        ? {
            ...card,
            ...formData,
          }
        : card,
    );

    localStorage.setItem("nfcCards", JSON.stringify(updatedCards));

    navigate("/cards");
  };

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
            <label>Kart Adı</label>

            <input
              type="text"
              name="name"
              value={formData.name}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label>Kart Türü</label>

            <select
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
            <label>Hedef URL</label>

            <input
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
