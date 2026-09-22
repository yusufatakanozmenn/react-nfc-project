import { useState } from "react";
import { useNavigate } from "react-router-dom";

function NewCard() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    name: "",
    type: "",
    destinationUrl: "",
  });

  const [generatedCode, setGeneratedCode] = useState("");

  const handleChange = (event) => {
    const { name, value } = event.target;

    setFormData({
      ...formData,
      [name]: value,
    });
  };

  const generateCode = () => {
    const characters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    const randomValues = new Uint32Array(6);

    crypto.getRandomValues(randomValues);

    let code = "";

    randomValues.forEach((value) => {
      code += characters[value % characters.length];
    });

    return code;
  };

  const handleSubmit = (event) => {
    event.preventDefault();

    const code = generateCode();

    const newCard = {
      id: Date.now(),
      ...formData,
      code: code,
      scans: 0,
      active: true,
    };

    const existingCards = JSON.parse(localStorage.getItem("nfcCards")) || [];

    const updatedCards = [...existingCards, newCard];

    localStorage.setItem("nfcCards", JSON.stringify(updatedCards));

    console.log("Yeni Kart:", newCard);

    navigate("/cards");
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

          <div className="form-group">
            <label htmlFor="destinationUrl">Hedef URL</label>

            <input
              id="destinationUrl"
              name="destinationUrl"
              type="url"
              placeholder="https://..."
              value={formData.destinationUrl}
              onChange={handleChange}
              required
            />
          </div>

          <button type="submit" className="primary-button">
            Kartı Oluştur
          </button>
        </form>

        {generatedCode && (
          <div className="card-result">
            <h3>Kart oluşturuldu</h3>

            <p>
              <strong>Kart Kodu:</strong> {generatedCode}
            </p>

            <p>
              <strong>NFC Linki:</strong>
            </p>

            <code>https://go.webonix.com.tr/r/{generatedCode}</code>
          </div>
        )}
      </div>
    </>
  );
}

export default NewCard;
