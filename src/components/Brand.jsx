export default function Brand({ light = false }) {
  return <span className={`brand${light ? " brand-light" : ""}`}>
    <img src="/webonix-mark.svg" width="44" height="44" alt="" />
    <span className="brand-wordmark">Webonix <strong>Tap</strong><small>Bir dokunuşla bağlantı kur.</small></span>
  </span>;
}
