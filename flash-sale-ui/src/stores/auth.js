import { reactive } from "vue";

const STORAGE_KEY = "flash-sale-auth";

const saved = safeRead();

export const authState = reactive({
  token: saved.token ?? "",
  userId: saved.userId ?? null,
  username: saved.username ?? ""
});

export function setSession(payload) {
  authState.token = payload.token ?? "";
  authState.userId = payload.userId ?? null;
  authState.username = payload.username ?? "";

  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      token: authState.token,
      userId: authState.userId,
      username: authState.username
    })
  );
}

export function clearSession() {
  authState.token = "";
  authState.userId = null;
  authState.username = "";
  localStorage.removeItem(STORAGE_KEY);
}

function safeRead() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || "{}");
  } catch {
    return {};
  }
}
