export const cardTypes = { google: "Google Yorum", instagram: "Instagram", whatsapp: "WhatsApp", website: "Web Sitesi" };
export function nfcLink(code, base, origin) {
  if (!code || !/^[a-zA-Z0-9_-]{1,64}$/.test(String(code))) return "";
  const url = new URL(`${base.replace(/\/$/, "")}/r/${encodeURIComponent(code)}`, origin);
  if (!["http:", "https:"].includes(url.protocol)) return "";
  return url.href;
}
export const safeDestination = value => {
  try { const url = new URL(value); return ["https:", "http:"].includes(url.protocol) && !url.username && !url.password ? url.href : ""; }
  catch { return ""; }
};
