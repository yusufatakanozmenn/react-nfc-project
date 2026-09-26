const API_URL = import.meta.env?.VITE_API_URL ?? "http://localhost:8080";
let onUnauthorized = () => {};

export const setUnauthorizedHandler = (handler) => {
  onUnauthorized = handler;
};

export const apiFetch = async (endpoint, options = {}) => {
  const { auth = true, token = localStorage.getItem("token"), ...request } = options;
  const headers = new Headers(request.headers || {});
  if (auth && token) headers.set("Authorization", `Bearer ${token}`);
  if (request.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const response = await fetch(`${API_URL}${endpoint}`, {
    ...request,
    signal: request.signal ?? AbortSignal.timeout(15000),
    headers,
  });
  // An older request must never end a newer session. A 403 is not a logout.
  if (response.status === 401 && auth && token === localStorage.getItem("token")) {
    onUnauthorized();
  }
  return response;
};

export const apiJson = async (endpoint, options) => {
  const response = await apiFetch(endpoint, options);
  const data = response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok) throw new Error(data?.message || `İşlem gerçekleştirilemedi (${response.status}).`);
  return data;
};
