import { productClient } from "./http";

export async function fetchSeckillProducts(params) {
  const response = await productClient.get("/seckill-product/products", { params });
  return response.data.data ?? [];
}

export async function fetchSeckillProductDetail(id) {
  const response = await productClient.get(`/seckill-product/products/${id}`);
  return response.data.data;
}
