import { useState } from "react";

function NewCard() {
  const [name, setName] = useState("");
  const [type, setType] = useState("");
  const [destinationUrl, setDestinationUrl] = useState("");

  const handleSubmit = (event) => {
    event.preventDefault();

    const newCard = {
      name: name,
      type: type,
      destinationUrl: destinationUrl,
    };

    console.log(newCard);
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
            <label>Kart Adı</label>

            <input
              type="text"
              placeholder="Örn: Miesha Google Yorum"
              value={name}
              onChange={(event) => setName(event.target.value)}
            />
          </div>

          <div className="form-group">
            <label>Kart Türü</label>

            <select
              value={type}
              onChange={(event) => setType(event.target.value)}
            >
              <option value="">Kart türü seçin</option>
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
              placeholder="https://..."
              value={destinationUrl}
              onChange={(event) => setDestinationUrl(event.target.value)}
            />
          </div>

          <button type="submit" className="primary-button">
            Kartı Oluştur
          </button>
        </form>
      </div>
    </>
  );
}

export default NewCard;
