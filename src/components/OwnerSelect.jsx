import { useEffect, useState } from "react";
import { apiJson } from "../services/api";

export default function OwnerSelect({ value, onChange, disabled }) {
  const [users, setUsers] = useState(null);
  const [error, setError] = useState("");
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    apiJson("/api/admin/users", { signal: controller.signal })
      .then((data) => { if (!controller.signal.aborted) { setUsers(data); setError(""); } })
      .catch(() => { if (!controller.signal.aborted) setError("Kullanıcılar yüklenemedi."); });
    return () => controller.abort();
  }, [attempt]);
  return <div className="form-group">
    <label htmlFor="ownerId">Kart sahibi</label>
    <select id="ownerId" value={value ?? ""} onChange={(event) => onChange(event.target.value)} disabled={disabled || !users}>
      <option value="">{users ? "Atanmamış — yalnızca admin görür" : "Kullanıcılar yükleniyor..."}</option>
      {users?.map((user) => <option key={user.id} value={user.id} disabled={!user.active}>
        {user.name} ({user.email}){!user.active ? " — Pasif" : ""}
      </option>)}
    </select>
    {error && <p role="alert">{error} <button type="button" className="edit-button" onClick={() => setAttempt((value) => value + 1)}>Tekrar dene</button></p>}
  </div>;
}
