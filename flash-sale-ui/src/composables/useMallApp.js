import { computed, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { fetchCurrentUser, updatePassword } from "../api/auth";
import {
  checkoutNormalOrder,
  fetchNormalOrderDetail,
  fetchNormalOrders,
  fetchNormalPayStatus,
  fetchSeckillOrderDetail,
  fetchSeckillOrders,
  fetchSeckillPayStatus,
  payNormalOrder,
  paySeckillOrder
} from "../api/order";
import { fetchProductDetail, fetchProducts } from "../api/product";
import { createSeckill, fetchSeckillResult } from "../api/seckill";
import {
  fetchSeckillProductDetail,
  fetchSeckillProducts
} from "../api/seckillProduct";
import { authState } from "../stores/auth";
import { getOrderStatusText, getProductPhase } from "../utils/format";

const CART_STORAGE_KEY = "flash-sale-cart";
const CATEGORY_NAME_MAP = {
  1: "休闲零食",
  2: "数码办公",
  3: "酒水饮料",
  4: "家居百货",
  5: "配件耗材",
  6: "个护礼盒"
};

export function useMallApp() {
  const products = ref([]);
  const seckillProducts = ref([]);
  const normalOrders = ref([]);
  const seckillOrders = ref([]);
  const profile = ref(null);

  const productsLoading = ref(false);
  const seckillProductsLoading = ref(false);
  const ordersLoading = ref(false);
  const profileLoading = ref(false);
  const checkoutLoading = ref(false);
  const productDetailLoading = ref(false);
  const orderDetailLoading = ref(false);

  const productDetailVisible = ref(false);
  const orderDialogVisible = ref(false);
  const productDetail = ref(null);
  const productDetailType = ref("normal");
  const selectedOrder = ref(null);
  const selectedOrderType = ref("seckill");

  const currentTime = ref(Date.now());
  const productFilters = reactive({
    name: "",
    status: "",
    categoryId: null
  });
  const checkoutForm = reactive({
    receiver: "",
    mobile: "",
    detail: "",
    remark: ""
  });
  const seckillState = reactive({});
  const cartItems = ref(readCart());

  const pollingTimers = new Map();
  let ticker = null;
  let initialized = false;

  const productCategories = computed(() => {
    const groups = new Map();
    for (const product of products.value) {
      const categoryId = product.categoryId ?? 0;
      if (!groups.has(categoryId)) {
        groups.set(categoryId, {
          id: categoryId,
          label: getCategoryName(categoryId),
          count: 0
        });
      }
      groups.get(categoryId).count += 1;
    }

    return [...groups.values()].sort((left, right) => left.id - right.id);
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

  const normalCartItems = computed(() =>
    cartItems.value.filter((item) => item.productType === "normal")
  );

  const seckillCartItems = computed(() =>
    cartItems.value.filter((item) => item.productType === "seckill")
  );

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

  const checkoutSummary = computed(() => {
    return normalCartItems.value.reduce(
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

  const orders = computed(() => {
    const merged = [
      ...normalOrders.value.map((order) => normalizeOrder(order, "normal")),
      ...seckillOrders.value.map((order) => normalizeOrder(order, "seckill"))
    ];

    return merged.sort((left, right) => {
      return new Date(right.createTime || 0).getTime() - new Date(left.createTime || 0).getTime();
    });
  });

  const recentOrders = computed(() => orders.value.slice(0, 3));

  const orderStats = computed(() => {
    const stats = {
      total: orders.value.length,
      created: 0,
      paid: 0,
      cancelled: 0,
      normal: normalOrders.value.length,
      seckill: seckillOrders.value.length
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

  const profileDisplayName = computed(() => {
    return profile.value?.username || authState.username || "未登录用户";
  });

  async function init() {
    if (!ticker) {
      ticker = window.setInterval(() => {
        currentTime.value = Date.now();
      }, 1000);
    }

    if (!initialized) {
      initialized = true;
      await Promise.all([
        loadProfile(),
        loadProducts(),
        loadSeckillProducts(),
        loadOrders()
      ]);
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

  async function loadProfile() {
    profileLoading.value = true;
    try {
      profile.value = await fetchCurrentUser();
    } catch (error) {
      profile.value = null;
      ElMessage.error(error.message);
    } finally {
      profileLoading.value = false;
    }
  }

  async function loadProducts() {
    productsLoading.value = true;
    try {
      products.value = await fetchProducts({
        name: productFilters.name || undefined,
        status: productFilters.status === "" ? undefined : Number(productFilters.status),
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
      const [normal, seckill] = await Promise.all([
        fetchNormalOrders(),
        fetchSeckillOrders()
      ]);
      normalOrders.value = normal;
      seckillOrders.value = seckill;
    } catch (error) {
      ElMessage.error(error.message);
    } finally {
      ordersLoading.value = false;
    }
  }

  async function refreshMallData() {
    await Promise.all([
      loadProfile(),
      loadProducts(),
      loadSeckillProducts(),
      loadOrders()
    ]);
  }

  function applyCategoryFilter(categoryId) {
    productFilters.categoryId = categoryId;
    loadProducts();
  }

  function clearProductFilters() {
    productFilters.name = "";
    productFilters.status = "";
    productFilters.categoryId = null;
    loadProducts();
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

  async function openOrder(orderOrId, explicitType = null) {
    const orderRef = normalizeOrderRef(orderOrId, explicitType);
    orderDialogVisible.value = true;
    orderDetailLoading.value = true;
    selectedOrderType.value = orderRef.orderType;

    try {
      const order =
        orderRef.orderType === "normal"
          ? await fetchNormalOrderDetail(orderRef.id)
          : await fetchSeckillOrderDetail(orderRef.id);
      selectedOrder.value = normalizeOrder(order, orderRef.orderType);
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
          await Promise.all([loadOrders(), loadSeckillProducts()]);
          if (result.orderId) {
            await openOrder(result.orderId, "seckill");
          }
          return;
        }

        state.status = "failed";
        state.message = result.message;
        ElMessage.error(result.message);
        await loadSeckillProducts();
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
    ElMessage.success(type === "normal" ? "已加入普通商品购物车" : "已加入秒杀商品草案");
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

  async function checkoutNormalCart() {
    if (!normalCartItems.value.length) {
      ElMessage.warning("购物车里还没有普通商品");
      return;
    }

    const addressSnapshot = buildAddressSnapshot();
    if (addressSnapshot === false) {
      return;
    }

    checkoutLoading.value = true;
    try {
      const order = await checkoutNormalOrder({
        items: normalCartItems.value.map((item) => ({
          productId: item.id,
          quantity: item.quantity
        })),
        remark: checkoutForm.remark || undefined,
        addressSnapshot: addressSnapshot || undefined
      });

      cartItems.value = cartItems.value.filter((item) => item.productType !== "normal");
      persistCart();
      clearCheckoutForm();
      ElMessage.success(`普通订单创建成功，订单号 ${order.orderNo}`);
      await Promise.all([loadProducts(), loadOrders()]);
      await openOrder(order, "normal");
    } catch (error) {
      ElMessage.error(error.message);
    } finally {
      checkoutLoading.value = false;
    }
  }

  async function payOrder(order) {
    const normalized = normalizeOrder(order, order.orderType);
    try {
      const paidOrder =
        normalized.orderType === "normal"
          ? await payNormalOrder(normalized.id)
          : await paySeckillOrder(normalized.id);

      ElMessage.success(
        normalized.orderType === "normal" ? "普通订单已模拟支付" : "秒杀订单已模拟支付"
      );
      await loadOrders();

      if (selectedOrder.value?.id === normalized.id && selectedOrder.value?.orderType === normalized.orderType) {
        selectedOrder.value = normalizeOrder(paidOrder, normalized.orderType);
      }
      return paidOrder;
    } catch (error) {
      ElMessage.error(error.message);
      throw error;
    }
  }

  async function fetchAndToastPayStatus(order) {
    const normalized = normalizeOrder(order, order.orderType);
    try {
      const payStatus =
        normalized.orderType === "normal"
          ? await fetchNormalPayStatus(normalized.id)
          : await fetchSeckillPayStatus(normalized.id);
      ElMessage.info(payStatus.message || "已获取支付状态");
      return payStatus;
    } catch (error) {
      ElMessage.error(error.message);
      throw error;
    }
  }

  async function submitPasswordUpdate(passwordForm) {
    if (!passwordForm.oldPassword || !passwordForm.newPassword) {
      ElMessage.warning("请补全旧密码和新密码");
      return false;
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      ElMessage.warning("两次输入的新密码不一致");
      return false;
    }

    try {
      await updatePassword({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      });
      ElMessage.success("密码修改成功");
      return true;
    } catch (error) {
      ElMessage.error(error.message);
      return false;
    }
  }

  function getOrderDisplayAmount(order) {
    return Number(order.displayAmount ?? order.payAmount ?? order.totalAmount ?? order.seckillPrice ?? 0);
  }

  function isOrderPayable(order) {
    return normalizeOrder(order, order.orderType).status === 0;
  }

  function getOrderSummary(order) {
    const normalized = normalizeOrder(order, order.orderType);
    if (normalized.orderType === "normal") {
      return normalized.items?.map((item) => item.productName).join("、") || "普通商品订单";
    }
    return `秒杀商品 ${normalized.productId}`;
  }

  function getAddressSummary(order) {
    const snapshot = order.addressSnapshot;
    if (!snapshot) {
      return "未填写收货地址";
    }

    try {
      const address = typeof snapshot === "string" ? JSON.parse(snapshot) : snapshot;
      return [address.receiver, address.mobile, address.detail].filter(Boolean).join(" · ");
    } catch {
      return snapshot;
    }
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

  function clearCheckoutForm() {
    checkoutForm.receiver = "";
    checkoutForm.mobile = "";
    checkoutForm.detail = "";
    checkoutForm.remark = "";
  }

  function buildAddressSnapshot() {
    const receiver = checkoutForm.receiver.trim();
    const mobile = checkoutForm.mobile.trim();
    const detail = checkoutForm.detail.trim();
    const hasAny = Boolean(receiver || mobile || detail);
    const hasAll = Boolean(receiver && mobile && detail);

    if (!hasAny) {
      return null;
    }

    if (!hasAll) {
      ElMessage.warning("收货人、手机号和地址请一起填写");
      return false;
    }

    return JSON.stringify({
      receiver,
      mobile,
      detail
    });
  }

  return {
    authState,
    profile,
    profileLoading,
    products,
    seckillProducts,
    normalOrders,
    seckillOrders,
    orders,
    recentOrders,
    productsLoading,
    seckillProductsLoading,
    ordersLoading,
    checkoutLoading,
    productDetailLoading,
    orderDetailLoading,
    productDetailVisible,
    orderDialogVisible,
    productDetail,
    productDetailType,
    selectedOrder,
    selectedOrderType,
    currentTime,
    productFilters,
    productCategories,
    cartItems,
    normalCartItems,
    seckillCartItems,
    featuredNormalProducts,
    homeShelves,
    flashProducts,
    cartSummary,
    checkoutSummary,
    orderStats,
    checkoutForm,
    profileDisplayName,
    init,
    dispose,
    loadProfile,
    loadProducts,
    loadSeckillProducts,
    loadOrders,
    refreshMallData,
    applyCategoryFilter,
    clearProductFilters,
    openProduct,
    openOrder,
    getProductCardState,
    canSeckill,
    handleSeckill,
    addToCart,
    updateCartQuantity,
    removeFromCart,
    isInCart,
    getCartItemPrice,
    checkoutNormalCart,
    payOrder,
    fetchAndToastPayStatus,
    submitPasswordUpdate,
    getOrderDisplayAmount,
    isOrderPayable,
    getOrderSummary,
    getAddressSummary,
    getOrderStatusText
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

function normalizeOrder(order, explicitType = null) {
  if (!order) {
    return null;
  }

  const orderType = explicitType || inferOrderType(order);
  const status = order.orderStatus ?? order.status ?? 0;
  const displayAmount =
    orderType === "normal"
      ? Number(order.payAmount ?? order.totalAmount ?? 0)
      : Number(order.seckillPrice ?? 0);

  return {
    ...order,
    orderType,
    status,
    displayAmount
  };
}

function normalizeOrderRef(orderOrId, explicitType) {
  if (typeof orderOrId === "object" && orderOrId !== null) {
    return {
      id: orderOrId.id,
      orderType: explicitType || inferOrderType(orderOrId)
    };
  }

  return {
    id: orderOrId,
    orderType: explicitType || "seckill"
  };
}

function inferOrderType(order) {
  return Object.prototype.hasOwnProperty.call(order, "orderNo") ||
    Object.prototype.hasOwnProperty.call(order, "orderStatus") ||
    Array.isArray(order.items)
    ? "normal"
    : "seckill";
}

function getCategoryName(categoryId) {
  return CATEGORY_NAME_MAP[categoryId] || `分类 ${categoryId}`;
}
