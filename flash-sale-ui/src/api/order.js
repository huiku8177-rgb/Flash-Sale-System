import { gatewayClient } from "./http";

export async function fetchSeckillOrders() {
  const response = await gatewayClient.get("/order/orders");
  return response.data.data ?? [];
}

export async function fetchSeckillOrderDetail(id) {
  const response = await gatewayClient.get(`/order/orderDetail/${id}`);
  return response.data.data;
}

export async function paySeckillOrder(id) {
  const response = await gatewayClient.post(`/order/seckill-orders/${id}/pay`);
  return response.data.data;
}

export async function fetchSeckillPayStatus(id) {
  const response = await gatewayClient.get(`/order/seckill-pay-status/${id}`);
  return response.data.data;
}

export async function checkoutNormalOrder(payload) {
  const response = await gatewayClient.post("/order/checkout", payload);
  return response.data.data;
}

export async function fetchNormalOrders(status) {
  const response = await gatewayClient.get("/order/normal-orders", {
    params: {
      status: status ?? undefined
    }
  });
  return response.data.data ?? [];
}

export async function fetchNormalOrderDetail(id) {
  const response = await gatewayClient.get(`/order/normal-orders/${id}`);
  return response.data.data;
}

export async function payNormalOrder(id) {
  const response = await gatewayClient.post(`/order/normal-orders/${id}/pay`);
  return response.data.data;
}

export async function fetchNormalPayStatus(id) {
  const response = await gatewayClient.get(`/order/pay-status/${id}`);
  return response.data.data;
}
