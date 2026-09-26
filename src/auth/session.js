import { apiFetch, setUnauthorizedHandler } from "../services/api.js";

let state = { status: "checking", user: null };
let revision = 0;
const listeners = new Set();
const publish = (status, user = null) => {
  state = { status, user };
  listeners.forEach((listener) => listener());
};
export const getSession = () => state;
export const subscribe = (listener) => {
  listeners.add(listener);
  return () => listeners.delete(listener);
};

export const logout = () => {
  revision++;
  localStorage.removeItem("token");
  localStorage.removeItem("user");
  publish("guest");
};
setUnauthorizedHandler(logout);

const currentUser = async (token) => {
  // Validation requests are handled here, independently of requests in old pages.
  const response = await apiFetch("/api/auth/me", { auth: false, headers: { Authorization: `Bearer ${token}` } });
  if (!response.ok) {
    const error = new Error(response.status === 401
      ? "Oturum geçersiz. Lütfen tekrar giriş yapın."
      : "Oturum doğrulanamadı. Lütfen tekrar deneyin.");
    error.status = response.status;
    throw error;
  }
  return response.json();
};

export const restoreSession = async () => {
  const attempt = ++revision;
  const token = localStorage.getItem("token");
  if (!token) {
    localStorage.removeItem("user");
    publish("guest");
    return;
  }
  publish("checking");
  try {
    const user = await currentUser(token);
    if (attempt !== revision) return;
    localStorage.setItem("user", JSON.stringify(user));
    publish("authenticated", user);
  } catch (error) {
    if (attempt !== revision) return;
    if (error.status === 401) logout();
    else publish("error");
  }
};

export const login = async (credentials) => {
  const attempt = ++revision;
  const response = await apiFetch("/api/auth/login", {
    method: "POST", auth: false, body: JSON.stringify(credentials),
  });
  if (!response.ok) {
    throw new Error(response.status === 401 ? "E-posta veya şifre hatalı." : "Giriş işlemi başarısız oldu. Lütfen tekrar deneyin.");
  }
  const data = await response.json();
  if (!data.token) throw new Error("Sunucu oturum bilgisi döndürmedi.");
  const user = await currentUser(data.token);
  if (attempt !== revision) throw new Error("Giriş işlemi iptal edildi. Lütfen tekrar deneyin.");
  localStorage.setItem("token", data.token);
  localStorage.setItem("user", JSON.stringify(user));
  publish("authenticated", user);
};

export const initializeAuth = () => {
  void restoreSession();
  const onStorage = (event) => {
    if (event.key === "token" || event.key === null) void restoreSession();
  };
  window.addEventListener("storage", onStorage);
  return () => {
    revision++;
    window.removeEventListener("storage", onStorage);
  };
};

export const updateProfile = async (profile) => {
  const attempt = revision;
  const token = localStorage.getItem("token");
  if (state.status !== "authenticated" || !token) throw new Error("Lütfen tekrar giriş yapın.");
  const response = await apiFetch("/api/auth/me", { method: "PUT", body: JSON.stringify(profile) });
  const data = await response.json().catch(() => null);
  if (!response.ok) throw new Error(data?.message || "Bilgiler güncellenemedi.");
  if (attempt !== revision || token !== localStorage.getItem("token")) throw new Error("Oturum değişti. Lütfen tekrar giriş yapın.");
  localStorage.setItem("user", JSON.stringify(data));
  publish("authenticated", data);
  return data;
};
