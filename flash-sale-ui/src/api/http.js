import axios from "axios";
import { authState, clearSession } from "../stores/auth";

const gatewayBaseUrl =
  import.meta.env.VITE_GATEWAY_BASE_URL || "http://localhost:8080";

const productBaseUrl =
  import.meta.env.VITE_PRODUCT_BASE_URL || gatewayBaseUrl;

function createClient(baseURL) {
  const client = axios.create({
    baseURL,
    timeout: 10000
  });

  client.interceptors.request.use((config) => {
    if (authState.token) {
      config.headers.Authorization = `Bearer ${authState.token}`;
    }
    return config;
  });

  client.interceptors.response.use(
    (response) => {
      const payload = response.data;
      if (payload && typeof payload.code === "number" && payload.code !== 200) {
        const error = new Error(payload.message || "业务请求失败");
        error.code = payload.code;
        error.payload = payload;
        return Promise.reject(error);
      }
      return response;
    },
    (error) => {
      if (error.response?.status === 401) {
        clearSession();
        if (!window.location.hash.startsWith("#/login")) {
          const currentPath = normalizeHashPath(window.location.hash);
          const redirect = encodeURIComponent(currentPath || "/app/home");
          window.location.hash = `#/login?redirect=${redirect}`;
        }
      }

      const message =
        error.response?.data?.message ||
        error.message ||
        "网络请求失败，请稍后重试";
      const wrappedError = new Error(message);
      wrappedError.status = error.response?.status;
      wrappedError.payload = error.response?.data;
      return Promise.reject(wrappedError);
    }
  );

  return client;
}

function normalizeHashPath(hash) {
  const normalized = (hash || "").replace(/^#/, "");
  if (!normalized || normalized.startsWith("/login")) {
    return "";
  }

  return normalized;
}

export const gatewayClient = createClient(gatewayBaseUrl);
export const productClient = createClient(productBaseUrl);
