import { productClient } from "./http";

export async function fetchCartItems() {
  const response = await productClient.get("/product/cart/items");
  return response.data.data ?? [];
}

export async function addCartItem(payload) {
  const response = await productClient.post("/product/cart/items", payload);
  return response.data.data;
}

export async function updateCartItem(id, payload) {
  const response = await productClient.put(`/product/cart/items/${id}`, payload);
  return response.data.data;
}

export async function deleteCartItem(id) {
  const response = await productClient.delete(`/product/cart/items/${id}`);
  return response.data;
}

export async function clearCartItems(selectedOnly = false) {
  const response = await productClient.delete("/product/cart/items", {
    params: {
      selectedOnly: selectedOnly || undefined
    }
  });
  return response.data;
}
