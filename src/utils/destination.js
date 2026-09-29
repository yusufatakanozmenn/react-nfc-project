export const destinations = {
  instagram: { prefix: 'https://www.instagram.com/', label: 'Instagram kullanıcı adı', placeholder: 'webonix', hint: '@ işareti olmadan kullanıcı adınızı yazın.', pattern: '[A-Za-z0-9._]{1,30}' },
  whatsapp: { prefix: 'https://wa.me/', label: 'WhatsApp telefon numarası', placeholder: '905551234567', hint: 'Ülke koduyla yazın. Örnek: 905551234567', pattern: '[1-9][0-9]{6,14}' },
  google: { prefix: 'https://search.google.com/local/writereview?placeid=', label: 'Google Place ID', placeholder: 'ChIJ…', hint: 'İşletmenizin Place ID bilgisini girin. Hazır Google yorum bağlantınız varsa “Tam bağlantı kullan” seçeneğini seçin.', pattern: '[A-Za-z0-9_-]+' },
};

export function normalizeDestinationPart(type, value) {
  if (type === 'instagram') return value.trim().replace(/^@/, '');
  if (type === 'whatsapp') return value.replace(/[\s()+-]/g, '');
  return value.trim();
}
