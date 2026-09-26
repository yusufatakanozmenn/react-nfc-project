import { test, beforeEach, after } from "node:test";
import assert from "node:assert/strict";
import { apiFetch } from "../src/services/api.js";
import { getSession, login, logout, restoreSession, initializeAuth } from "../src/auth/session.js";

const storage = new Map();
Object.defineProperty(globalThis, "localStorage", { value: {
  getItem: (key) => storage.get(key) ?? null,
  setItem: (key, value) => storage.set(key, String(value)),
  removeItem: (key) => storage.delete(key),
}, configurable: true });
const originalFetch = globalThis.fetch;
const user = { id: 1, name: "Veritabanındaki İsim", email: "test@example.test", role: "USER" };
const json = (data, status = 200) => new Response(JSON.stringify(data), { status });
const deferred = () => {
  let resolve;
  const promise = new Promise((done) => { resolve = done; });
  return { promise, resolve };
};
beforeEach(() => {
  logout();
  globalThis.fetch = async () => { throw new Error("Unexpected network request"); };
});
after(() => { globalThis.fetch = originalFetch; });

const successfulLogin = async (token = "new-token") => {
  globalThis.fetch = async (url) => url.endsWith("/login") ? json({ token }) : json(user);
  await login({ email: user.email, password: "test-only" });
};

test("without a token protected pages stay guest without contacting the server", async () => {
  await restoreSession();
  assert.equal(getSession().status, "guest");
});
test("login verifies /me and uses database identity before authenticating", async () => {
  const calls = [];
  globalThis.fetch = async (url, options) => {
    calls.push({ url, authorization: options.headers.get("Authorization") });
    return url.endsWith("/login") ? json({ token: "issued-token", name: "Untrusted name" }) : json(user);
  };
  await login({ email: user.email, password: "test-only" });
  assert.equal(getSession().status, "authenticated");
  assert.deepEqual(getSession().user, user);
  assert.equal(calls[0].authorization, null);
  assert.equal(calls[1].authorization, "Bearer issued-token");
  assert.equal(localStorage.getItem("token"), "issued-token");
});
test("wrong credentials do not establish a session", async () => {
  globalThis.fetch = async () => json({}, 401);
  await assert.rejects(login({}), /E-posta veya şifre/);
  assert.equal(getSession().status, "guest");
  assert.equal(localStorage.getItem("token"), null);
});
test("a successful login response is insufficient when /me rejects its token", async () => {
  globalThis.fetch = async (url) => url.endsWith("/login") ? json({ token: "rejected" }) : json({}, 401);
  await assert.rejects(login({}), /Oturum geçersiz/);
  assert.equal(localStorage.getItem("token"), null);
  assert.equal(getSession().status, "guest");
});
test("refresh revalidates token and replaces cached user data", async () => {
  localStorage.setItem("token", "saved-token");
  localStorage.setItem("user", '{"name":"Forged"}');
  globalThis.fetch = async () => json(user);
  await restoreSession();
  assert.deepEqual(getSession().user, user);
});
test("invalid or expired token is cleared on refresh", async () => {
  localStorage.setItem("token", "invalid");
  localStorage.setItem("user", JSON.stringify(user));
  globalThis.fetch = async () => json({}, 401);
  await restoreSession();
  assert.equal(getSession().status, "guest");
  assert.equal(storage.size, 0);
});
test("temporary server failure hides the panel but preserves the token for retry", async () => {
  localStorage.setItem("token", "saved-token");
  globalThis.fetch = async () => json({}, 503);
  await restoreSession();
  assert.equal(getSession().status, "error");
  assert.equal(localStorage.getItem("token"), "saved-token");
  globalThis.fetch = async () => json(user);
  await restoreSession();
  assert.equal(getSession().status, "authenticated");
});
test("403 does not log out an authenticated user", async () => {
  await successfulLogin();
  globalThis.fetch = async () => json({}, 403);
  assert.equal((await apiFetch("/api/cards")).status, 403);
  assert.equal(getSession().status, "authenticated");
});
test("401 from the current session clears both storage and central state", async () => {
  await successfulLogin();
  globalThis.fetch = async () => json({}, 401);
  await apiFetch("/api/cards");
  assert.equal(getSession().status, "guest");
  assert.equal(storage.size, 0);
});
test("late 401 from an old token cannot invalidate a new login", async () => {
  localStorage.setItem("token", "old-token");
  const pending = deferred();
  globalThis.fetch = () => pending.promise;
  const oldRequest = apiFetch("/api/cards");
  await successfulLogin();
  pending.resolve(json({}, 401));
  await oldRequest;
  assert.equal(getSession().status, "authenticated");
  assert.equal(localStorage.getItem("token"), "new-token");
});
test("late /me after logout cannot resurrect the session", async () => {
  localStorage.setItem("token", "saved-token");
  const pending = deferred();
  globalThis.fetch = () => pending.promise;
  const checking = restoreSession();
  logout();
  pending.resolve(json(user));
  await checking;
  assert.equal(getSession().status, "guest");
  assert.equal(storage.size, 0);
});
test("late login completion after logout cannot restore credentials", async () => {
  const pending = deferred();
  globalThis.fetch = async (url) => url.endsWith("/login") ? json({ token: "cancelled" }) : pending.promise;
  const signingIn = login({});
  logout();
  pending.resolve(json(user));
  await assert.rejects(signingIn, /iptal edildi/);
  assert.equal(storage.size, 0);
});
test("StrictMode setup-cleanup-setup ignores the first verification response", async () => {
  const events = new EventTarget();
  globalThis.window = events;
  localStorage.setItem("token", "saved-token");
  const old = deferred();
  let count = 0;
  globalThis.fetch = () => ++count === 1 ? old.promise : Promise.resolve(json(user));
  const firstCleanup = initializeAuth();
  firstCleanup();
  const cleanup = initializeAuth();
  await new Promise((resolve) => setTimeout(resolve, 0));
  old.resolve(json({}, 401));
  await new Promise((resolve) => setTimeout(resolve, 0));
  assert.equal(getSession().status, "authenticated");
  assert.equal(localStorage.getItem("token"), "saved-token");
  cleanup();
  delete globalThis.window;
});
test("logout followed by session restore remains guest", async () => {
  await successfulLogin();
  logout();
  await restoreSession();
  assert.equal(getSession().status, "guest");
  assert.equal(storage.size, 0);
});

test("profile update refreshes the shared user without changing the token", async () => {
  const { updateProfile } = await import("../src/auth/session.js");
  await successfulLogin();
  const updated = { ...user, name: "Updated Name", email: "updated@example.test" };
  globalThis.fetch = async () => json(updated);
  await updateProfile({ name: updated.name, email: updated.email, currentPassword: "test-only" });
  assert.deepEqual(getSession().user, updated);
  assert.deepEqual(JSON.parse(localStorage.getItem("user")), updated);
  assert.equal(localStorage.getItem("token"), "new-token");
});
test("wrong profile password leaves the session and current identity intact", async () => {
  const { updateProfile } = await import("../src/auth/session.js");
  await successfulLogin();
  globalThis.fetch = async () => json({ message: "Mevcut şifre hatalı." }, 400);
  await assert.rejects(updateProfile({}), /Mevcut şifre/);
  assert.deepEqual(getSession().user, user);
  assert.equal(getSession().status, "authenticated");
});
test("a profile response arriving after logout cannot restore the user", async () => {
  const { updateProfile } = await import("../src/auth/session.js");
  await successfulLogin();
  const pending = deferred();
  globalThis.fetch = () => pending.promise;
  const saving = updateProfile({});
  logout(); pending.resolve(json(user));
  await assert.rejects(saving, /Oturum değişti/);
  assert.equal(getSession().status, "guest");
  assert.equal(storage.size, 0);
});
test("only ADMIN opens the admin home; USER starts at own cards", async () => {
  const { isAdmin, homePath } = await import("../src/auth/permissions.js");
  assert.equal(homePath({ role: "ADMIN" }), "/");
  assert.equal(homePath(user), "/cards");
  assert.equal(isAdmin(user), false);
  assert.equal(isAdmin(null), false);
});
