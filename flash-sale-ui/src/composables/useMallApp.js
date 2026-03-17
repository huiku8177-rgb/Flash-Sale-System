import { computed, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { fetchOrderDetail, fetchOrders } from "../api/order";
import { fetchProductDetail, fetchProducts } from "../api/product";
import {
  fetchSeckillProductDetail,
  fetchSeckillProducts
} from "../api/seckillProduct";
import { createSeckill, fetchSeckillResult } from "../api/seckill";
import { authState } from "../stores/auth";
import { getProductPhase } from "../utils/format";

const CART_STORAGE_KEY = "flash-sale-cart";

export function useMallApp() {
  const products = ref([]);
  const seckillProducts = ref([]);
  const orders = ref([]);
  const productsLoading = ref(false);
  const seckillProductsLoading = ref(false);
  const ordersLoading = ref(false);
  const productDetailLoading = ref(false);
  const orderDetailLoading = ref(false);
  const productDetailVisible = ref(false);
  const orderDialogVisible = ref(false);
  const productDetail = ref(null);
  const productDetailType = ref("normal");
  const selectedOrder = ref(null);
  const currentTime = ref(Date.now());
  const productFilters = reactive({
    name: "",
    status: "",
    categoryId: null
  });
  const seckillState = reactive({});
  const cartItems = ref(readCart());

  const pollingTimers = new Map();
  let ticker = null;
  let initialized = false;

  const orderStats = computed(() => {
    const stats = {
      total: orders.value.length,
      created: 0,
      paid: 0,
      cancelled: 0
    };

    orders.value.forEach((order) => {
      if (order.status === 0) {
        stats.created += 1;
      } else if (order.status === 1) {
        stats.paid += 1;
      } else if (order.status === 2) {
        stats.cancelled += 1;
      }
    });

    return stats;
  });

  const flashProducts = computed(() => {
    const phaseWeight = {
      running: 0,
      upcoming: 1,
      ended: 2,
      offline: 3
    };

    return [...seckillProducts.value].sort((left, right) => {
      const leftPhase = getProductPhase(left, currentTime.value);
      const rightPhase = getProductPhase(right, currentTime.value);
      return phaseWeight[leftPhase] - phaseWeight[rightPhase];
    });
  });

  const featuredNormalProducts = computed(() => {
    return [...products.value]
      .filter((product) => product.status === 1 && Number(product.stock || 0) > 0)
      .sort((left, right) => {
        const leftDiscount = Number(left.marketPrice ?? left.price ?? 0) - Number(left.price ?? 0);
        const rightDiscount = Number(right.marketPrice ?? right.price ?? 0) - Number(right.price ?? 0);
        if (rightDiscount !== leftDiscount) {
          return rightDiscount - leftDiscount;
        }
        return Number(right.stock ?? 0) - Number(left.stock ?? 0);
      });
  });

  const homeShelves = computed(() => {
    return {
      hero: featuredNormalProducts.value.slice(0, 3),
      picks: featuredNormalProducts.value.slice(0, 4),
      fresh: featuredNormalProducts.value.slice(4, 8)
    };
  });

  const cartSummary = computed(() => {
    return cartItems.value.reduce(
      (summary, item) => {
        summary.count += item.quantity;
        summary.total += Number(getCartItemPrice(item) ?? 0) * item.quantity;
        return summary;
      },
      {
        count: 0,
        total: 0
      }
    );
  });

  async function init() {
    if (!ticker) {
      ticker = window.setInterval(() => {
        currentTime.value = Date.now();
      }, 1000);
    }

    if (!initialized) {
      initialized = true;
      await Promise.all([loadProducts(), loadSeckillProducts(), loadOrders()]);
    }
  }

  function dispose() {
    if (ticker) {
      window.clearInterval(ticker);
      ticker = null;
    }

    pollingTimers.forEach((timer) => window.clearInterval(timer));
    pollingTimers.clear();
  }

  async function loadProducts() {
    productsLoading.value = true;
    try {
      products.value = await fetchProducts({
        name: productFilters.name || undefined,
        status: productFilters.status === "" ? undefined : productFilters.status,
        categoryId: productFilters.categoryId || undefined
      });
      syncCartSnapshots();
    } catch (error) {
      ElMessage.error(error.message);
    } finally {
      productsLoading.value = false;
    }
  }

  async function loadSeckillProducts() {
    seckillProductsLoading.value = true;
    try {
      seckillProducts.value = await fetchSeckillProducts();
      syncCartSnapshots();
    } catch (error) {
      ElMessage.error(error.message);
    } finally {
      seckillProductsLoading.value = false;
    }
  }

  async function loadOrders() {
    ordersLoading.value = true;
    try {
      orders.value = await fetchOrders();
    } catch (error) {
      ElMessage.error(error.message);
    } finally {
      ordersLoading.value = false;
    }
  }

  async function refreshMallData() {
    await Promise.all([loadProducts(), loadSeckillProducts(), loadOrders()]);
  }

  async function openProduct(product, type = inferProductType(product)) {
    productDetailVisible.value = true;
    productDetailLoading.value = true;
    productDetailType.value = type;
    productDetail.value = product;

    try {
      productDetail.value =
        type === "seckill"
          ? await fetchSeckillProductDetail(product.id)
          : await fetchProductDetail(product.id);
    } catch (error) {
      ElMessage.warning(error.message);
    } finally {
      productDetailLoading.value = false;
    }
  }

  async function openOrder(orderId) {
    orderDialogVisible.value = true;
    orderDetailLoading.value = true;
    try {
      selectedOrder.value = await fetchOrderDetail(orderId);
    } catch (error) {
      ElMessage.error(error.message);
    } finally {
      orderDetailLoading.value = false;
    }
  }

  function getProductCardState(productId) {
    return (
      seckillState[productId] || {
        pending: false,
        status: "idle",
        message: ""
      }
    );
  }

  function ensureSeckillState(productId) {
    if (!seckillState[productId]) {
      seckillState[productId] = {
        pending: false,
        status: "idle",
        message: ""
      };
    }
    return seckillState[productId];
  }

  function canSeckill(product) {
    const phase = getProductPhase(product, currentTime.value);
    return phase === "running" && !getProductCardState(product.id).pending;
  }

  async function handleSeckill(product) {
    const state = ensureSeckillState(product.id);
    state.pending = true;
    state.status = "submitting";
    state.message = "请求已提交，正在进入抢购队列";

    try {
      const result = await createSeckill(product.id);
      state.message = result.message;
      if (!result.success) {
        state.pending = false;
        state.status = "failed";
        ElMessage.warning(result.message);
        return;
      }

      state.status = "polling";
      ElMessage.success(result.message);
      startPolling(product.id);
    } catch (error) {
      state.pending = false;
      state.status = "failed";
      state.message = error.message;
      ElMessage.error(error.message);
    }
  }

  function startPolling(productId) {
    stopPolling(productId);
    const startedAt = Date.now();

    const timer = window.setInterval(async () => {
      try {
        const result = await fetchSeckillResult(productId);
        const state = ensureSeckillState(productId);

        if (result.status === 0) {
          state.pending = true;
          state.status = "polling";
          state.message = result.message;

          if (Date.now() - startedAt > 15000) {
            state.pending = false;
            state.status = "timeout";
            state.message = "查询超时，请稍后在订单里确认结果";
            stopPolling(productId);
            ElMessage.warning(state.message);
          }
          return;
        }

        stopPolling(productId);
        state.pending = false;

        if (result.status === 1) {
          state.status = "success";
          state.message = `${result.message}，订单号 ${result.orderId}`;
          ElMessage.success(state.message);
          await loadOrders();
          if (result.orderId) {
            await openOrder(result.orderId);
          }
          return;
        }

        state.status = "failed";
        state.message = result.message;
        ElMessage.error(result.message);
      } catch (error) {
        stopPolling(productId);
        const state = ensureSeckillState(productId);
        state.pending = false;
        state.status = "failed";
        state.message = error.message;
        ElMessage.error(error.message);
      }
    }, 500);

    pollingTimers.set(productId, timer);
  }

  function stopPolling(productId) {
    const timer = pollingTimers.get(productId);
    if (timer) {
      window.clearInterval(timer);
      pollingTimers.delete(productId);
    }
  }

  function addToCart(product, type = inferProductType(product)) {
    const cartKey = buildCartKey(product.id, type);
    const existing = cartItems.value.find((item) => item.cartKey === cartKey);
    if (existing) {
      existing.quantity += 1;
    } else {
      cartItems.value.unshift(createCartItem(product, type));
    }
    persistCart();
    ElMessage.success("已加入购物车草案");
  }

  function updateCartQuantity(cartKey, delta) {
    const target = cartItems.value.find((item) => item.cartKey === cartKey);
    if (!target) {
      return;
    }

    target.quantity = Math.max(1, target.quantity + delta);
    persistCart();
  }

  function removeFromCart(cartKey) {
    cartItems.value = cartItems.value.filter((item) => item.cartKey !== cartKey);
    persistCart();
    ElMessage.success("已移出购物车");
  }

  function isInCart(productId, type = "normal") {
    const cartKey = buildCartKey(productId, type);
    return cartItems.value.some((item) => item.cartKey === cartKey);
  }

  function getCartItemPrice(item) {
    return item.displayPrice ?? item.seckillPrice ?? item.price ?? 0;
  }

  function syncCartSnapshots() {
    const normalProductMap = new Map(products.value.map((product) => [product.id, product]));
    const seckillProductMap = new Map(
      seckillProducts.value.map((product) => [product.id, product])
    );

    cartItems.value = cartItems.value.map((item) => {
      const sourceMap =
        item.productType === "seckill" ? seckillProductMap : normalProductMap;
      const freshProduct = sourceMap.get(item.id);
      if (!freshProduct) {
        return item;
      }
      return {
        ...createCartItem(freshProduct, item.productType),
        quantity: item.quantity
      };
    });
    persistCart();
  }

  function persistCart() {
    localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(cartItems.value));
  }

  return {
    authState,
    products,
    seckillProducts,
    orders,
    productsLoading,
    seckillProductsLoading,
    ordersLoading,
    productDetailLoading,
    orderDetailLoading,
    productDetailVisible,
    orderDialogVisible,
    productDetail,
    productDetailType,
    selectedOrder,
    currentTime,
    productFilters,
    cartItems,
    featuredNormalProducts,
    homeShelves,
    flashProducts,
    cartSummary,
    orderStats,
    init,
    dispose,
    loadProducts,
    loadSeckillProducts,
    loadOrders,
    refreshMallData,
    openProduct,
    openOrder,
    getProductCardState,
    canSeckill,
    handleSeckill,
    addToCart,
    updateCartQuantity,
    removeFromCart,
    isInCart,
    getCartItemPrice
  };
}

function readCart() {
  try {
    return JSON.parse(localStorage.getItem(CART_STORAGE_KEY) || "[]");
  } catch {
    return [];
  }
}

function inferProductType(product) {
  return Object.prototype.hasOwnProperty.call(product, "seckillPrice")
    ? "seckill"
    : "normal";
}

function buildCartKey(productId, type) {
  return `${type}:${productId}`;
}

function createCartItem(product, type) {
  return {
    cartKey: buildCartKey(product.id, type),
    productType: type,
    id: product.id,
    name: product.name,
    subtitle: product.subtitle ?? "",
    price: product.price,
    marketPrice: product.marketPrice ?? null,
    seckillPrice: product.seckillPrice ?? null,
    displayPrice: type === "seckill" ? product.seckillPrice ?? product.price : product.price,
    stock: product.stock,
    startTime: product.startTime ?? null,
    endTime: product.endTime ?? null,
    status: product.status,
    mainImage: product.mainImage ?? "",
    detail: product.detail ?? "",
    quantity: 1
  };
}
