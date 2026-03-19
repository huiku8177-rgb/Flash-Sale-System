import { gatewayClient } from "./http";

export async function login(payload) {
  const response = await gatewayClient.post("/auth/login", payload);
  return response.data.data;
}

export async function register(payload) {
  const response = await gatewayClient.post("/auth/register", payload);
  return response.data;
}

export async function logout() {
  const response = await gatewayClient.post("/auth/logout");
  return response.data;
}

export async function fetchCurrentUser() {
  const response = await gatewayClient.get("/auth/me");
  return response.data.data;
}

export async function updatePassword(payload) {
  const response = await gatewayClient.post("/auth/updatePassword", payload);
  return response.data;
}
