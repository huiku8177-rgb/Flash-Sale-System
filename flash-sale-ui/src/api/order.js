import { gatewayClient } from "./http";

export async function fetchOrders() {
  const response = await gatewayClient.get("/order/orders");
  return response.data.data ?? [];
}

export async function fetchOrderDetail(id) {
  const response = await gatewayClient.get(`/order/orderDetail/${id}`);
  return response.data.data;
}
