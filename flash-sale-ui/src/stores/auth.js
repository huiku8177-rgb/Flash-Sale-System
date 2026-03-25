import { reactive } from "vue";

const STORAGE_KEY = "flash-sale-auth";

const saved = safeRead();

export const authState = reactive({
  token: saved.token ?? "",
  userId: saved.userId ?? null,
  username: saved.username ?? "",
  persist: saved.persist ?? true
});

export function setSession(payload, options = {}) {
  const persist = options.persist ?? true;

  authState.token = payload.token ?? "";
  authState.userId = payload.userId ?? null;
  authState.username = payload.username ?? "";
  authState.persist = persist;

  const snapshot = JSON.stringify({
    token: authState.token,
    userId: authState.userId,
    username: authState.username,
    persist
  });

  localStorage.removeItem(STORAGE_KEY);
  sessionStorage.removeItem(STORAGE_KEY);
  selectStorage(persist).setItem(STORAGE_KEY, snapshot);
}

export function clearSession() {
  authState.token = "";
  authState.userId = null;
  authState.username = "";
  authState.persist = true;
  localStorage.removeItem(STORAGE_KEY);
  sessionStorage.removeItem(STORAGE_KEY);
}

function safeRead() {
  try {
    const local = readStorage(localStorage, true);
    if (local.token) {
      return local;
    }

    return readStorage(sessionStorage, false);
  } catch {
    return {};
  }
}

function readStorage(storage, persist) {
  const parsed = JSON.parse(storage.getItem(STORAGE_KEY) || "{}");
  if (!parsed || typeof parsed !== "object") {
    return {};
  }

  return {
    ...parsed,
    persist
  };
}

function selectStorage(persist) {
  return persist ? localStorage : sessionStorage;
}
