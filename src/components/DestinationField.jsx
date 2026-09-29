import { useState } from 'react';
import { destinations, normalizeDestinationPart } from '../utils/destination';

export default function DestinationField({ type, value, onChange }) {
  const config = destinations[type];
  const [fullUrl, setFullUrl] = useState(Boolean(value && config && !value.startsWith(config.prefix)));
  const guided = config && !fullUrl;
  return <div className="form-group">
    <label htmlFor="destinationUrl">{guided ? config.label : 'Hedef bağlantı'}</label>
    {guided ? <>
      <div className="destination-input">
        <span className="destination-prefix">{config.prefix}</span>
        <input id="destinationUrl" name="destinationUrl" type="text" required
          value={value.startsWith(config.prefix) ? value.slice(config.prefix.length) : ''}
          onChange={event => onChange(config.prefix + normalizeDestinationPart(type, event.target.value))}
          placeholder={config.placeholder} pattern={config.pattern} title={config.hint}
          maxLength={255 - config.prefix.length} autoCapitalize="none" autoCorrect="off" spellCheck={false}
          inputMode={type === 'whatsapp' ? 'tel' : 'text'} aria-describedby="destinationHint" />
      </div>
      <p id="destinationHint" className="destination-hint">{config.hint}</p>
    </> : <input id="destinationUrl" name="destinationUrl" type="url" required
      value={value} onChange={event => onChange(event.target.value)} placeholder="https://…"
      maxLength={255} pattern="https?://.*" title="http:// veya https:// ile başlayan bir bağlantı girin." />}
    {config && <button type="button" className="destination-mode" onClick={() => {
      if (fullUrl && !value.startsWith(config.prefix)) onChange('');
      setFullUrl(!fullUrl);
    }}>{fullUrl ? 'Sabit adresle doldur' : 'Tam bağlantı kullan'}</button>}
  </div>;
}
