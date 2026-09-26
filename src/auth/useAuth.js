import { useSyncExternalStore } from "react";
import { getSession, subscribe } from "./session.js";

export const useAuth = () => useSyncExternalStore(subscribe, getSession);
