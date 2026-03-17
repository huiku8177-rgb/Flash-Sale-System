import { productClient } from "./http";

export async function fetchProducts(params) {
  const response = await productClient.get("/product/products", { params });
  return response.data.data ?? [];
}

export async function fetchProductDetail(id) {
  const response = await productClient.get(`/product/products/${id}`);
  return response.data.data;
}
