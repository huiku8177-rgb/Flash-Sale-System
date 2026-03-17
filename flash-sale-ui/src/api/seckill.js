import { gatewayClient } from "./http";

export async function createSeckill(productId) {
  const response = await gatewayClient.post(`/seckill/${productId}`);
  return response.data.data;
}

export async function fetchSeckillResult(productId) {
  const response = await gatewayClient.get(`/seckill/result/${productId}`);
  return response.data.data;
}
