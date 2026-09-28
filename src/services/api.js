const API_URL =
  import.meta.env?.VITE_API_URL ??
  (import.meta.env?.PROD
    ? "https://nfc-api.webonix.com.tr"
    : "http://localhost:8081");
let onUnauthorized = () => {};
let sessionVersion = 0;
let csrfRequest;
export const advanceSession = () => {
  sessionVersion++;
};
export const setUnauthorizedHandler = (handler) => {
  onUnauthorized = handler;
};

const csrfToken = () => {
  // Share simultaneous first requests so cookie and token cannot diverge.
  if (!csrfRequest) {
    csrfRequest = fetch(`${API_URL}/api/auth/csrf`, {
      credentials: "include",
      cache: "no-store",
      signal: AbortSignal.timeout(15000),
    })
      .then(async (response) => {
        if (!response.ok)
          throw new Error(
            "Güvenlik doğrulaması alınamadı. Lütfen tekrar deneyin.",
          );
        const data = await response.json();
        if (!data.token) throw new Error("Güvenlik doğrulaması eksik.");
        return data.token;
      })
      .finally(() => {
        csrfRequest = undefined;
      });
  }
  return csrfRequest;
};

export const apiFetch = async (endpoint, options = {}) => {
  const { auth = true, ...request } = options;
  const version = sessionVersion;
  const headers = new Headers(request.headers || {});
  if (request.body && !headers.has("Content-Type"))
    headers.set("Content-Type", "application/json");
  if (
    !["GET", "HEAD", "OPTIONS"].includes(
      (request.method || "GET").toUpperCase(),
    )
  ) {
    headers.set("X-XSRF-TOKEN", await csrfToken());
    if (auth && version !== sessionVersion)
      throw new Error("Oturum değişti. Lütfen tekrar deneyin.");
  }
  const response = await fetch(`${API_URL}${endpoint}`, {
    ...request,
    credentials: "include",
    cache: "no-store",
    signal: request.signal ?? AbortSignal.timeout(15000),
    headers,
  });
  if (response.status === 401 && auth && version === sessionVersion)
    onUnauthorized();
  return response;
};

export const apiJson = async (endpoint, options) => {
  const response = await apiFetch(endpoint, options);
  const data =
    response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok)
    throw new Error(
      data?.message || `İşlem gerçekleştirilemedi (${response.status}).`,
    );
  return data;
};
