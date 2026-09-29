import { apiFetch } from "./api.js";

export async function deleteCustomer(id) {
  const response = await apiFetch(`/api/admin/customers/${id}`, { method: "DELETE" });
  if ([404, 405, 501].includes(response.status)) {
    throw new Error("Müşteri silme hizmeti sunucuda henüz etkin değil. Müşteri kaydı korunuyor.");
  }
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.message || "Müşteri silinemedi. Lütfen tekrar deneyin.");
  }
}
