import { gatewayClient } from "./http";

export async function fetchUserAddresses() {
  const response = await gatewayClient.get("/auth/addresses");
  return response.data.data ?? [];
}

export async function fetchUserAddressDetail(id) {
  const response = await gatewayClient.get(`/auth/addresses/${id}`);
  return response.data.data;
}

export async function createUserAddress(payload) {
  const response = await gatewayClient.post("/auth/addresses", payload);
  return response.data.data;
}

export async function updateUserAddress(id, payload) {
  const response = await gatewayClient.put(`/auth/addresses/${id}`, payload);
  return response.data.data;
}

export async function deleteUserAddress(id) {
  const response = await gatewayClient.delete(`/auth/addresses/${id}`);
  return response.data.data;
}

export async function setDefaultUserAddress(id) {
  const response = await gatewayClient.put(`/auth/addresses/${id}/default`);
  return response.data.data;
}
