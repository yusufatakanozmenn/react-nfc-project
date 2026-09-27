import { test, beforeEach, after } from "node:test";
import assert from "node:assert/strict";
import { apiFetch } from "../src/services/api.js";
import { getSession, login, logout, restoreSession, initializeAuth, updateProfile } from "../src/auth/session.js";

const storage = new Map();
Object.defineProperty(globalThis, "localStorage", { value: {
  getItem: (key) => storage.get(key) ?? null,
  setItem: (key, value) => storage.set(key, String(value)),
  removeItem: (key) => storage.delete(key),
}, configurable: true });
const originalFetch = globalThis.fetch;
const user = { id: 1, name: "Database Name", email: "test@example.test", role: "USER" };
const json = (data, status = 200) => new Response(JSON.stringify(data), { status });
const noContent = () => new Response(null, { status: 204 });
const tick = () => new Promise((resolve) => setTimeout(resolve, 0));
const deferred = () => {
  let resolve;
  const promise = new Promise((done) => { resolve = done; });
  return { promise, resolve };
};
const mock = (handler) => {
  globalThis.fetch = async (url, options) => {
    assert.equal(options.credentials, "include");
    if (url.endsWith("/csrf")) return json({ token: "csrf-test-only" });
    return handler(url, options);
  };
};
const successfulLogin = async () => {
  mock(async (url) => url.endsWith("/logout") ? noContent() : json(user));
  await login({ email: user.email, password: "test-only" });
};
beforeEach(async () => {
  mock(async () => noContent());
  await logout();
  storage.clear();
  mock(async () => { throw new Error("Unexpected network request"); });
});
after(() => { globalThis.fetch = originalFetch; });

test("cookie session is checked on refresh without relying on localStorage", async () => {
  storage.set("token", "obsolete"); storage.set("user", '{"role":"ADMIN"}');
  mock(async () => json(user));
  await restoreSession();
  assert.deepEqual(getSession().user, user);
  assert.equal(storage.has("token"), false); assert.equal(storage.has("user"), false);
});
test("login sends CSRF and credentials, verifies /me, never exposes a bearer token", async () => {
  const calls = [];
  mock(async (url, options) => {
    calls.push({ url, options }); return json(url.endsWith("/login") ? { name: "Untrusted" } : user);
  });
  await login({ email: user.email, password: "test-only" });
  assert.deepEqual(getSession().user, user);
  assert.equal(calls[0].options.headers.get("X-XSRF-TOKEN"), "csrf-test-only");
  assert.equal(calls[1].options.headers.get("Authorization"), null);
  assert.equal(localStorage.getItem("token"), null); assert.equal(localStorage.getItem("user"), null);
});
test("wrong credentials do not authenticate", async () => {
  mock(async () => json({}, 401));
  await assert.rejects(login({}), /E-posta veya şifre/);
  assert.equal(getSession().status, "guest");
});
test("rate limiting gives actionable feedback", async () => {
  mock(async () => json({}, 429));
  await assert.rejects(login({}), /Çok fazla giriş denemesi/);
});
test("successful login is insufficient if /me rejects the session", async () => {
  mock(async (url) => json({}, url.endsWith("/login") ? 200 : 401));
  await assert.rejects(login({}), /Oturum geçersiz/);
  assert.equal(getSession().status, "guest");
});
test("expired session returns to login", async () => {
  mock(async () => json({}, 401)); await restoreSession();
  assert.equal(getSession().status, "guest");
});
test("temporary outage hides the panel and permits retry", async () => {
  mock(async () => json({}, 503)); await restoreSession();
  assert.equal(getSession().status, "error");
  mock(async () => json(user)); await restoreSession();
  assert.equal(getSession().status, "authenticated");
});
test("403 preserves session while 401 clears central identity", async () => {
  await successfulLogin(); mock(async () => json({}, 403));
  await apiFetch("/api/cards"); assert.equal(getSession().status, "authenticated");
  mock(async () => json({}, 401)); await apiFetch("/api/cards");
  assert.equal(getSession().status, "guest");
});
test("an old 401 cannot invalidate a newer login", async () => {
  const pending = deferred(); mock(() => pending.promise);
  const old = apiFetch("/api/cards");
  await successfulLogin(); pending.resolve(json({}, 401)); await old;
  assert.equal(getSession().status, "authenticated");
});
test("logout waits for server revocation and failure does not claim success", async () => {
  await successfulLogin(); mock(async () => { throw new Error("offline"); });
  await assert.rejects(logout(), /offline/);
  assert.equal(getSession().status, "authenticated");
  mock(async () => noContent()); await logout(); assert.equal(getSession().status, "guest");
});
test("late /me cannot resurrect a logged out session", async () => {
  const pending = deferred(); mock(() => pending.promise);
  const restoring = restoreSession(); await tick();
  mock(async () => noContent()); await logout();
  pending.resolve(json(user)); await restoring;
  assert.equal(getSession().status, "guest");
});
test("logout is serialized after pending login so its new cookie is revoked", async () => {
  const pending = deferred(); const calls = [];
  mock(async (url) => {
    calls.push(url.split("/").at(-1));
    if (url.endsWith("/login")) return pending.promise;
    if (url.endsWith("/logout")) return noContent();
    return json(user);
  });
  const signingIn = login({}); const cancelled = assert.rejects(signingIn, /iptal edildi/);
  await tick(); const signingOut = logout();
  pending.resolve(json(user)); await cancelled; await signingOut;
  assert.deepEqual(calls, ["login", "me", "logout"]);
  assert.equal(getSession().status, "guest");
});
test("StrictMode ignores the verification from the cleaned-up effect", async () => {
  globalThis.window = new EventTarget();
  const pending = deferred(); let count = 0;
  mock(() => ++count === 1 ? pending.promise : json(user));
  const first = initializeAuth(); await tick(); first();
  const cleanup = initializeAuth(); await tick();
  pending.resolve(json({}, 401)); await tick();
  assert.equal(getSession().status, "authenticated");
  cleanup(); delete globalThis.window;
});
test("profile update refreshes identity without writing credentials to storage", async () => {
  await successfulLogin(); const updated = { ...user, name: "Updated" };
  mock(async () => json(updated)); await updateProfile({});
  assert.deepEqual(getSession().user, updated);
  assert.equal(storage.has("token"), false); assert.equal(storage.has("user"), false);
});
test("incorrect profile password preserves current identity", async () => {
  await successfulLogin(); mock(async () => json({ message: "Mevcut şifre hatalı." }, 400));
  await assert.rejects(updateProfile({}), /Mevcut şifre/);
  assert.deepEqual(getSession().user, user);
});
test("profile response after logout cannot restore identity", async () => {
  await successfulLogin(); const pending = deferred(); mock(() => pending.promise);
  const saving = updateProfile({}); const stale = assert.rejects(saving, /Oturum değişti/);
  await tick(); mock(async () => noContent()); await logout();
  pending.resolve(json(user)); await stale; assert.equal(getSession().status, "guest");
});
test("failed CSRF bootstrap prevents mutation", async () => {
  let calls = 0; globalThis.fetch = async () => { calls++; return json({}, 503); };
  await assert.rejects(apiFetch("/api/cards", { method: "POST", body: "{}" }), /Güvenlik doğrulaması/);
  assert.equal(calls, 1);
});
test("each mutation obtains a fresh CSRF token and safe reads do not require it", async () => {
  const calls = [];
  globalThis.fetch = async (url, options) => {
    calls.push(url); if (url.endsWith("/csrf")) return json({ token: `csrf-${calls.length}` });
    if (options.method === "PUT") assert.ok(options.headers.get("X-XSRF-TOKEN"));
    return json({});
  };
  await apiFetch("/api/cards");
  await apiFetch("/api/cards/1", { method: "PUT" });
  await apiFetch("/api/cards/2", { method: "PUT" });
  assert.equal(calls.filter(url => url.endsWith("/csrf")).length, 2);
});
test("simultaneous writes share CSRF bootstrap", async () => {
  let csrfCalls = 0;
  globalThis.fetch = async (url) => {
    if (url.endsWith("/csrf")) { csrfCalls++; return json({ token: "same" }); }
    return json({});
  };
  await Promise.all([apiFetch("/one", { method: "POST" }), apiFetch("/two", { method: "POST" })]);
  assert.equal(csrfCalls, 1);
});
test("another tab's logout event revalidates the cookie session", async () => {
  globalThis.window = new EventTarget(); mock(async () => json(user));
  const cleanup = initializeAuth(); await tick(); assert.equal(getSession().status, "authenticated");
  mock(async () => json({}, 401)); const event = new Event("storage"); event.key = "auth:event";
  window.dispatchEvent(event); await tick(); assert.equal(getSession().status, "guest");
  cleanup(); delete globalThis.window;
});
test("only ADMIN opens dashboard and USER starts at cards", async () => {
  const { isAdmin, homePath } = await import("../src/auth/permissions.js");
  assert.equal(homePath({ role: "ADMIN" }), "/"); assert.equal(homePath(user), "/cards");
  assert.equal(isAdmin(user), false); assert.equal(isAdmin(null), false);
});
