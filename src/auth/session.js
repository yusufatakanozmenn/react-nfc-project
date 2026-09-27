import { apiFetch, apiJson, advanceSession, setUnauthorizedHandler } from "../services/api.js";

let state = { status: "checking", user: null };
let revision = 0;
let authQueue = Promise.resolve();
const listeners = new Set();
const nextRevision = () => { advanceSession(); return ++revision; };
const publish = (status, user = null) => {
  state = { status, user };
  listeners.forEach((listener) => listener());
};
const clearLegacyStorage = () => {
  localStorage.removeItem("token");
  localStorage.removeItem("user");
};
const notifyTabs = () => {
  // This is only an event marker, never identity or credentials.
  localStorage.setItem("auth:event", crypto.randomUUID());
};
const enqueue = (operation) => {
  const result = authQueue.then(operation);
  authQueue = result.catch(() => {});
  return result;
};
export const getSession = () => state;
export const subscribe = (listener) => {
  listeners.add(listener);
  return () => listeners.delete(listener);
};
const unauthorized = () => {
  nextRevision(); clearLegacyStorage(); publish("guest");
};
setUnauthorizedHandler(unauthorized);

const currentUser = async () => {
  const response = await apiFetch("/api/auth/me", { auth: false });
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
  const attempt = nextRevision();
  clearLegacyStorage();
  publish("checking");
  await authQueue;
  if (attempt !== revision) return;
  try {
    const user = await currentUser();
    if (attempt === revision) publish("authenticated", user);
  } catch (error) {
    if (attempt === revision) publish(error.status === 401 ? "guest" : "error");
  }
};

export const login = (credentials) => {
  const attempt = nextRevision();
  return enqueue(async () => {
    const response = await apiFetch("/api/auth/login", {
      method: "POST", auth: false, body: JSON.stringify(credentials),
    });
    if (!response.ok) {
      throw new Error(response.status === 401 ? "E-posta veya şifre hatalı."
        : response.status === 429 ? "Çok fazla giriş denemesi. Lütfen daha sonra tekrar deneyin."
          : "Giriş işlemi başarısız oldu. Lütfen tekrar deneyin.");
    }
    const user = await currentUser();
    if (attempt !== revision) throw new Error("Giriş işlemi iptal edildi. Lütfen tekrar deneyin.");
    clearLegacyStorage(); publish("authenticated", user); notifyTabs();
  });
};

export const logout = () => {
  const attempt = nextRevision();
  return enqueue(async () => {
    // A failed network request must not pretend the server session was revoked.
    await apiJson("/api/auth/logout", { method: "POST", auth: false });
    if (attempt !== revision) return;
    clearLegacyStorage(); publish("guest"); notifyTabs();
  });
};

export const initializeAuth = () => {
  void restoreSession();
  const onStorage = (event) => {
    if (event.key === "auth:event" || event.key === null) void restoreSession();
  };
  window.addEventListener("storage", onStorage);
  return () => { nextRevision(); window.removeEventListener("storage", onStorage); };
};

export const updateProfile = async (profile) => {
  const attempt = revision;
  if (state.status !== "authenticated") throw new Error("Lütfen tekrar giriş yapın.");
  const data = await apiJson("/api/auth/me", { method: "PUT", body: JSON.stringify(profile) });
  if (attempt !== revision) throw new Error("Oturum değişti. Lütfen tekrar giriş yapın.");
  publish("authenticated", data); notifyTabs(); return data;
};
