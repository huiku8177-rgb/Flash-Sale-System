import { computed, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { fetchCurrentUser, updatePassword } from "../api/auth";
import {
  addCartItem,
  deleteCartItem,
  fetchCartItems,
  updateCartItem
} from "../api/cart";
import { fetchUserAddresses } from "../api/address";
import {
  cancelNormalOrder,
  cancelSeckillOrder,
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
import {
  formatDateTime,
  getOrderStatusText,
  getProductPhase
} from "../utils/format";

const CART_STORAGE_KEY = "flash-sale-cart-draft";

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
  const productCategoryStats = ref(buildCategoryStats());
  const addresses = ref([]);
  const profile = ref(null);

  const productsLoading = ref(false);
  const seckillProductsLoading = ref(false);
  const ordersLoading = ref(false);
  const profileLoading = ref(false);
  const addressesLoading = ref(false);
  const checkoutLoading = ref(false);
  const productDetailLoading = ref(false);
  const orderDetailLoading = ref(false);

  const productDetailVisible = ref(false);
  const orderDialogVisible = ref(false);
  const seckillPayConfirmVisible = ref(false);
  const productDetail = ref(null);
  const productDetailType = ref("normal");
  const selectedOrder = ref(null);
  const selectedOrderType = ref("seckill");
  const pendingCheckoutOrder = ref(null);
  const pendingSeckillPayOrder = ref(null);
  const selectedAddressId = ref(null);
  const currentTime = ref(Date.now());

  const productFilters = reactive({
    name: "",
    status: "",
    categoryId: null
  });
  const checkoutForm = reactive({
    remark: ""
  });
  const seckillState = reactive({});
  const cartItems = ref(readCart());

  const pollingTimers = new Map();
  let ticker = null;
  let initialized = false;

  const productCategories = computed(() => productCategoryStats.value);

  const productTotalCount = computed(() =>
    productCategoryStats.value.reduce((total, entry) => total + entry.count, 0)
  );

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

  const homeShelves = computed(() => ({
    hero: featuredNormalProducts.value.slice(0, 3),
    picks: featuredNormalProducts.value.slice(0, 4),
    fresh: featuredNormalProducts.value.slice(4, 8)
  }));

  const normalCartItems = computed(() => cartItems.value.filter((item) => item.productType === "normal"));
  const seckillCartItems = computed(() => cartItems.value.filter((item) => item.productType === "seckill"));

  const selectedNormalCartItems = computed(() =>
    normalCartItems.value.filter((item) => item.selected && item.canCheckout)
  );

  const normalCartAllSelected = computed(() => {
    const selectableItems = normalCartItems.value.filter((item) => item.canCheckout);
    return Boolean(selectableItems.length) && selectableItems.every((item) => item.selected);
  });

  const cartSummary = computed(() => {
    return cartItems.value.reduce(
      (summary, item) => {
        summary.count += Number(item.quantity || 0);
        summary.total += Number(getCartItemPrice(item) || 0) * Number(item.quantity || 0);
        return summary;
      },
      { count: 0, total: 0 }
    );
  });

  const checkoutSummary = computed(() => {
    return selectedNormalCartItems.value.reduce(
      (summary, item) => {
        summary.count += Number(item.quantity || 0);
        summary.total += Number(getCartItemPrice(item) || 0) * Number(item.quantity || 0);
        return summary;
      },
      { count: 0, total: 0 }
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
      if (order.status === 0) stats.created += 1;
      else if (order.status === 1) stats.paid += 1;
      else if (order.status === 2) stats.cancelled += 1;
    });

    return stats;
  });

  const selectedAddress = computed(() =>
    addresses.value.find((address) => address.id === selectedAddressId.value) || null
  );

  const checkoutFormComplete = computed(() => Boolean(selectedAddress.value));

  const profileDisplayName = computed(() => {
    return profile.value?.username || authState.username || "未登录用户";
  });

  async function init() {
    if (!ticker) {
      ticker = window.setInterval(() => {
        currentTime.value = Date.now();
      }, 1000);
    }

    if (initialized) {
      return;
    }

    initialized = true;
    await Promise.all([
      loadProducts(),
      loadSeckillProducts(),
      loadProfile(),
      loadOrders(),
      loadCartItems(),
      loadAddresses()
    ]);
  }

  function dispose() {
    if (ticker) {
      window.clearInterval(ticker);
      ticker = null;
    }

    pollingTimers.forEach((timer) => window.clearInterval(timer));
    pollingTimers.clear();
  }

  function resetUserState() {
    profile.value = null;
    normalOrders.value = [];
    seckillOrders.value = [];
    addresses.value = [];
    selectedAddressId.value = null;
    pendingCheckoutOrder.value = null;
    pendingSeckillPayOrder.value = null;
    selectedOrder.value = null;
    orderDialogVisible.value = false;
    seckillPayConfirmVisible.value = false;
    checkoutForm.remark = "";
    cartItems.value = readCart();
  }

  async function loadProfile() {
    if (!authState.token) {
      profile.value = null;
      return;
    }

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
      const list = await fetchProducts({
        name: productFilters.name || undefined,
        status: productFilters.status === "" ? undefined : Number(productFilters.status),
        categoryId: productFilters.categoryId || undefined
      });
      products.value = list.map((product) => ({
        ...product,
        categoryName: getCategoryName(product.categoryId)
      }));
      if (!productFilters.name && productFilters.status === "" && productFilters.categoryId === null) {
        productCategoryStats.value = buildCategoryStats(products.value);
      }
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
    if (!authState.token) {
      normalOrders.value = [];
      seckillOrders.value = [];
      return;
    }

    ordersLoading.value = true;
    try {
      const [normal, seckill] = await Promise.all([fetchNormalOrders(), fetchSeckillOrders()]);
      normalOrders.value = normal;
      seckillOrders.value = seckill;
    } catch (error) {
      ElMessage.error(error.message);
    } finally {
      ordersLoading.value = false;
    }
  }

  async function loadCartItems() {
    const draftItems = readCart();

    if (!authState.token) {
      cartItems.value = draftItems;
      return;
    }

    try {
      const items = await fetchCartItems();
      cartItems.value = [...items.map((item) => normalizeCartItem(item)), ...draftItems];
    } catch (error) {
      cartItems.value = draftItems;
      ElMessage.error(error.message);
    }
  }

  async function loadAddresses() {
    if (!authState.token) {
      addresses.value = [];
      selectedAddressId.value = null;
      return;
    }

    addressesLoading.value = true;
    try {
      const list = await fetchUserAddresses();
      addresses.value = list;
      const activeAddress =
        list.find((address) => address.id === selectedAddressId.value) ||
        list.find((address) => address.isDefault) ||
        list[0] ||
        null;
      selectedAddressId.value = activeAddress?.id ?? null;
    } catch (error) {
      addresses.value = [];
      selectedAddressId.value = null;
      ElMessage.error(error.message);
    } finally {
      addressesLoading.value = false;
    }
  }

  async function refreshMallData() {
    await Promise.all([
      loadProducts(),
      loadSeckillProducts(),
      loadProfile(),
      loadOrders(),
      loadCartItems(),
      loadAddresses()
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
    if (!authState.token) {
      ElMessage.warning("请先登录后再查看商品详情");
      if (window.location.hash !== "#/login") {
        window.location.hash = "#/login";
      }
      return;
    }

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
    if (!authState.token) {
      ElMessage.warning("请先登录后再参与秒杀");
      window.location.hash = "#/login";
      return;
    }

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
            state.message = "查询超时，请稍后在订单中心确认结果";
            stopPolling(productId);
            ElMessage.warning(state.message);
          }
          return;
        }

        stopPolling(productId);
        state.pending = false;

        if (result.status === 1) {
          state.status = "success";
          state.message = result.message;
          ElMessage.success(result.message);
          await Promise.all([loadOrders(), loadSeckillProducts()]);
          const existingOrder = seckillOrders.value.find((order) => order.id === result.orderId);
          if (existingOrder?.status === 0) {
            openSeckillPayConfirm(existingOrder);
          }
          return;
        }

        state.status = "failed";
        state.message = result.message;
        ElMessage.warning(result.message);
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

  async function addToCart(product, type = inferProductType(product)) {
    if (type === "seckill") {
      const draft = createLocalCartItem(product, type);
      const existing = seckillCartItems.value.find((item) => item.cartKey === draft.cartKey);
      if (existing) {
        existing.quantity += 1;
      } else {
        cartItems.value.unshift(draft);
      }
      persistCart();
      ElMessage.success("已加入秒杀草稿");
      return;
    }

    if (!authState.token) {
      ElMessage.warning("请先登录后再加入购物车");
      window.location.hash = "#/login";
      return;
    }

    try {
      await addCartItem({
        productId: product.id,
        quantity: 1,
        selected: true
      });
      await loadCartItems();
      ElMessage.success("已加入购物车");
    } catch (error) {
      ElMessage.error(error.message);
    }
  }

  async function updateCartQuantity(cartKey, delta) {
    const target = cartItems.value.find((item) => item.cartKey === cartKey);
    if (!target) {
      return;
    }

    if (target.productType === "seckill") {
      target.quantity = Math.max(1, Number(target.quantity || 1) + delta);
      persistCart();
      return;
    }

    try {
      await updateCartItem(target.cartItemId, {
        quantity: Math.max(1, Number(target.quantity || 1) + delta),
        selected: target.selected
      });
      await loadCartItems();
    } catch (error) {
      ElMessage.error(error.message);
    }
  }

  async function updateCartSelected(cartKey, selected) {
    const target = normalCartItems.value.find((item) => item.cartKey === cartKey);
    if (!target) {
      return;
    }

    try {
      await updateCartItem(target.cartItemId, {
        quantity: target.quantity,
        selected: Boolean(selected)
      });
      await loadCartItems();
    } catch (error) {
      ElMessage.error(error.message);
    }
  }

  async function toggleAllNormalCart(selected) {
    const nextValue = Boolean(selected);
    const changedItems = normalCartItems.value.filter(
      (item) => item.canCheckout && item.selected !== nextValue
    );

    if (!changedItems.length) {
      return;
    }

    try {
      await Promise.all(
        changedItems.map((item) =>
          updateCartItem(item.cartItemId, {
            quantity: item.quantity,
            selected: nextValue
          })
        )
      );
      await loadCartItems();
    } catch (error) {
      ElMessage.error(error.message);
    }
  }

  async function removeFromCart(cartKey) {
    const target = cartItems.value.find((item) => item.cartKey === cartKey);
    if (!target) {
      return;
    }

    if (target.productType === "seckill") {
      cartItems.value = cartItems.value.filter((item) => item.cartKey !== cartKey);
      persistCart();
      ElMessage.success("已移出购物车草稿");
      return;
    }

    try {
      await deleteCartItem(target.cartItemId);
      await loadCartItems();
      ElMessage.success("已移出购物车");
    } catch (error) {
      ElMessage.error(error.message);
    }
  }

  function isInCart(productId, type = "normal") {
    return cartItems.value.some(
      (item) => item.productType === type && Number(item.productId || item.id) === Number(productId)
    );
  }

  function getCartItemPrice(item) {
    return Number(item.displayPrice ?? item.seckillPrice ?? item.price ?? 0);
  }

  async function prepareImmediateCheckout(product) {
    if (!authState.token) {
      ElMessage.warning("请先登录后再立即购买");
      window.location.hash = "#/login";
      return false;
    }

    if (inferProductType(product) !== "normal") {
      ElMessage.warning("秒杀商品请直接在秒杀会场参与抢购");
      return false;
    }

    await Promise.all([loadAddresses(), loadCartItems()]);

    let target = normalCartItems.value.find((item) => Number(item.productId) === Number(product.id));
    if (!target) {
      await addCartItem({
        productId: product.id,
        quantity: 1,
        selected: true
      });
      await loadCartItems();
      target = normalCartItems.value.find((item) => Number(item.productId) === Number(product.id));
    }

    if (!target) {
      ElMessage.error("立即购买准备失败，请稍后重试");
      return false;
    }

    const changedItems = normalCartItems.value.filter((item) => {
      if (!item.canCheckout) {
        return false;
      }
      const shouldSelect = Number(item.productId) === Number(product.id);
      return item.selected !== shouldSelect;
    });

    if (changedItems.length) {
      await Promise.all(
        changedItems.map((item) =>
          updateCartItem(item.cartItemId, {
            quantity: item.quantity,
            selected: Number(item.productId) === Number(product.id)
          })
        )
      );
      await loadCartItems();
    }

    pendingCheckoutOrder.value = null;
    checkoutForm.remark = "";
    return true;
  }

  async function createPendingCheckoutOrder() {
    if (pendingCheckoutOrder.value) {
      return pendingCheckoutOrder.value;
    }

    if (!selectedNormalCartItems.value.length) {
      ElMessage.warning("请先勾选需要结算的商品");
      return null;
    }

    if (!selectedAddressId.value) {
      ElMessage.warning("请选择收货地址后再支付");
      return null;
    }

    checkoutLoading.value = true;
    try {
      const order = await checkoutNormalOrder({
        addressId: selectedAddressId.value,
        remark: checkoutForm.remark || undefined
      });
      const normalized = normalizeOrder(order, "normal");
      pendingCheckoutOrder.value = normalized;
      checkoutForm.remark = "";
      await Promise.all([loadProducts(), loadOrders(), loadCartItems()]);
      ElMessage.success(`普通订单已创建，订单号 ${normalized.orderNo}`);
      return normalized;
    } catch (error) {
      ElMessage.error(error.message);
      return null;
    } finally {
      checkoutLoading.value = false;
    }
  }

  function holdPendingCheckoutOrder() {
    const order = pendingCheckoutOrder.value;
    pendingCheckoutOrder.value = null;
    return order;
  }

  async function submitCheckoutAndPay() {
    const order = pendingCheckoutOrder.value || (await createPendingCheckoutOrder());
    if (!order) {
      return null;
    }
    return payOrder(order);
  }

  function openSeckillPayConfirm(order) {
    const normalized = normalizeOrder(order, "seckill");
    if (normalized.status !== 0) {
      ElMessage.warning("当前秒杀订单状态不允许继续支付");
      return;
    }
    pendingSeckillPayOrder.value = normalized;
    seckillPayConfirmVisible.value = true;
  }

  function holdPendingSeckillPayOrder() {
    const order = pendingSeckillPayOrder.value;
    pendingSeckillPayOrder.value = null;
    seckillPayConfirmVisible.value = false;
    return order;
  }

  async function confirmSeckillPay() {
    if (!pendingSeckillPayOrder.value) {
      return null;
    }
    return payOrder(pendingSeckillPayOrder.value);
  }

  async function payOrder(order) {
    const normalized = normalizeOrder(order, order.orderType);
    try {
      const paidOrder =
        normalized.orderType === "normal"
          ? await payNormalOrder(normalized.id)
          : await paySeckillOrder(normalized.id);

      const normalizedPaidOrder = normalizeOrder(paidOrder, normalized.orderType);
      if (normalized.orderType === "normal") {
        pendingCheckoutOrder.value = null;
        await Promise.all([loadOrders(), loadCartItems()]);
      } else {
        pendingSeckillPayOrder.value = null;
        seckillPayConfirmVisible.value = false;
        await loadOrders();
      }

      if (
        selectedOrder.value?.id === normalized.id &&
        selectedOrder.value?.orderType === normalized.orderType
      ) {
        selectedOrder.value = normalizedPaidOrder;
      }

      ElMessage.success(normalized.orderType === "normal" ? "普通订单已模拟支付" : "秒杀订单已模拟支付");
      return normalizedPaidOrder;
    } catch (error) {
      ElMessage.error(error.message);
      return null;
    }
  }

  async function cancelOrder(order) {
    const normalized = normalizeOrder(order, order.orderType);
    if (normalized.status !== 0) {
      ElMessage.warning("当前订单状态不允许取消");
      return null;
    }

    try {
      const cancelledOrder =
        normalized.orderType === "normal"
          ? await cancelNormalOrder(normalized.id)
          : await cancelSeckillOrder(normalized.id);

      const normalizedCancelledOrder = normalizeOrder(cancelledOrder, normalized.orderType);
      if (normalized.orderType === "normal") {
        if (pendingCheckoutOrder.value?.id === normalized.id) {
          pendingCheckoutOrder.value = null;
        }
        await Promise.all([loadOrders(), loadProducts(), loadCartItems()]);
      } else {
        if (pendingSeckillPayOrder.value?.id === normalized.id) {
          pendingSeckillPayOrder.value = null;
          seckillPayConfirmVisible.value = false;
        }
        await Promise.all([loadOrders(), loadSeckillProducts()]);
      }

      if (
        selectedOrder.value?.id === normalized.id &&
        selectedOrder.value?.orderType === normalized.orderType
      ) {
        selectedOrder.value = normalizedCancelledOrder;
      }

      ElMessage.success(normalized.orderType === "normal" ? "普通订单已取消" : "秒杀订单已取消");
      return normalizedCancelledOrder;
    } catch (error) {
      ElMessage.error(error.message);
      return null;
    }
  }

  async function fetchAndToastPayStatus(order) {
    const normalized = normalizeOrder(order, order.orderType);
    try {
      const payStatus =
        normalized.orderType === "normal"
          ? await fetchNormalPayStatus(normalized.id)
          : await fetchSeckillPayStatus(normalized.id);
      ElMessage.info(payStatus.message || `当前订单状态：${getOrderStatusText(normalized.status)}`);
      return payStatus;
    } catch (error) {
      ElMessage.error(error.message);
      return null;
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
    return normalized.productName || `秒杀商品 ${normalized.productId}`;
  }

  function getAddressSummary(order) {
    if (order.receiver || order.mobile || order.detail) {
      return [order.receiver, order.mobile, order.detail].filter(Boolean).join(" ");
    }

    const snapshot = order.addressSnapshot;
    if (!snapshot) {
      return "未填写收货地址";
    }

    try {
      const address = typeof snapshot === "string" ? JSON.parse(snapshot) : snapshot;
      return [address.receiver, address.mobile, address.detail].filter(Boolean).join(" ");
    } catch {
      return snapshot;
    }
  }

  function getOrderStatusNote(order) {
    const normalized = normalizeOrder(order, order.orderType);
    if (normalized.status === 0) {
      return "当前订单待支付，可继续支付";
    }
    if (normalized.status === 1) {
      return normalized.payTime ? `支付时间 ${formatDateTime(normalized.payTime)}` : "当前订单已完成支付";
    }
    if (normalized.status === 2) {
      const reason = normalized.cancelReason || "订单已取消";
      return normalized.cancelTime ? `${reason} · ${formatDateTime(normalized.cancelTime)}` : reason;
    }
    return "";
  }

  function handleCheckoutAddressChange(value) {
    selectedAddressId.value = value ?? null;
  }

  function persistCart() {
    persistCartItems(cartItems.value);
  }

  function syncCartSnapshots() {
    const seckillMap = new Map(seckillProducts.value.map((product) => [product.id, product]));
    cartItems.value = cartItems.value.map((item) => {
      if (item.productType !== "seckill") {
        return item;
      }
      const latestProduct = seckillMap.get(item.productId);
      if (!latestProduct) {
        return item;
      }
      return {
        ...createLocalCartItem(latestProduct, "seckill"),
        quantity: item.quantity
      };
    });
    persistCart();
  }

  watch(
    () => authState.token,
    async (token) => {
      if (!initialized) {
        return;
      }

      if (!token) {
        resetUserState();
        return;
      }

      await Promise.all([loadProfile(), loadOrders(), loadCartItems(), loadAddresses()]);
    }
  );

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
    addresses,
    addressesLoading,
    selectedAddress,
    selectedAddressId,
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
    productTotalCount,
    cartItems,
    normalCartItems,
    seckillCartItems,
    selectedNormalCartItems,
    normalCartAllSelected,
    featuredNormalProducts,
    homeShelves,
    flashProducts,
    cartSummary,
    checkoutSummary,
    orderStats,
    checkoutForm,
    checkoutFormComplete,
    profileDisplayName,
    pendingCheckoutOrder,
    pendingSeckillPayOrder,
    seckillPayConfirmVisible,
    init,
    dispose,
    loadProfile,
    loadProducts,
    loadSeckillProducts,
    loadOrders,
    loadCartItems,
    loadAddresses,
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
    updateCartSelected,
    toggleAllNormalCart,
    removeFromCart,
    isInCart,
    getCartItemPrice,
    prepareImmediateCheckout,
    createPendingCheckoutOrder,
    holdPendingCheckoutOrder,
    submitCheckoutAndPay,
    openSeckillPayConfirm,
    holdPendingSeckillPayOrder,
    confirmSeckillPay,
    payOrder,
    cancelOrder,
    fetchAndToastPayStatus,
    submitPasswordUpdate,
    getOrderDisplayAmount,
    isOrderPayable,
    getOrderSummary,
    getAddressSummary,
    getOrderStatusNote,
    handleCheckoutAddressChange,
    getOrderStatusText
  };
}

function readCart() {
  try {
    const stored = JSON.parse(localStorage.getItem(CART_STORAGE_KEY) || "[]");
    return Array.isArray(stored) ? stored.map((item) => normalizeDraftCartItem(item)) : [];
  } catch {
    return [];
  }
}

function persistCartItems(items) {
  localStorage.setItem(
    CART_STORAGE_KEY,
    JSON.stringify(items.filter((item) => item.productType === "seckill"))
  );
}

function inferProductType(product) {
  return Object.prototype.hasOwnProperty.call(product, "seckillPrice") ? "seckill" : "normal";
}

function buildCartKey(productId, type) {
  return `${type}:${productId}`;
}

function createLocalCartItem(product, type) {
  return normalizeDraftCartItem({
    cartKey: buildCartKey(product.id, type),
    productType: type,
    id: product.id,
    productId: product.id,
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
    quantity: 1,
    selected: false,
    canCheckout: false
  });
}

function normalizeDraftCartItem(item) {
  return {
    cartKey: item.cartKey || buildCartKey(item.productId || item.id, item.productType || "seckill"),
    cartItemId: item.cartItemId ?? null,
    productType: item.productType || "seckill",
    id: item.id,
    productId: item.productId ?? item.id,
    name: item.name,
    subtitle: item.subtitle ?? "",
    price: Number(item.price ?? 0),
    marketPrice: item.marketPrice == null ? null : Number(item.marketPrice),
    seckillPrice: item.seckillPrice == null ? null : Number(item.seckillPrice),
    displayPrice: Number(item.displayPrice ?? item.seckillPrice ?? item.price ?? 0),
    stock: Number(item.stock ?? 0),
    startTime: item.startTime ?? null,
    endTime: item.endTime ?? null,
    status: item.status ?? 1,
    mainImage: item.mainImage ?? "",
    detail: item.detail ?? "",
    quantity: Number(item.quantity ?? 1),
    selected: false,
    canCheckout: false
  };
}

function normalizeCartItem(item) {
  const productId = Number(item.productId ?? item.id);
  return {
    cartKey: buildCartKey(productId, "normal"),
    cartItemId: item.id,
    productType: "normal",
    id: productId,
    productId,
    name: item.productName ?? item.name ?? "普通商品",
    subtitle: item.productSubtitle ?? item.subtitle ?? "",
    price: Number(item.price ?? item.salePrice ?? 0),
    marketPrice: item.marketPrice == null ? null : Number(item.marketPrice),
    seckillPrice: null,
    displayPrice: Number(item.price ?? item.salePrice ?? 0),
    stock: Number(item.stock ?? 0),
    status: item.status ?? 1,
    mainImage: item.productImage ?? item.mainImage ?? "",
    detail: item.detail ?? "",
    quantity: Number(item.quantity ?? 1),
    selected: Boolean(item.selected),
    canCheckout: item.canCheckout !== false && Number(item.stock ?? 0) > 0 && Number(item.status ?? 1) === 1
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
      : Number(order.seckillPrice ?? order.payAmount ?? 0);

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
  if (Array.isArray(order.items) || Object.prototype.hasOwnProperty.call(order, "payAmount")) {
    return "normal";
  }
  return "seckill";
}

function getCategoryName(categoryId) {
  return CATEGORY_NAME_MAP[categoryId] || `分类 ${categoryId}`;
}

function buildCategoryStats(sourceProducts = []) {
  const groups = new Map(
    Object.entries(CATEGORY_NAME_MAP).map(([id, label]) => [
      Number(id),
      {
        id: Number(id),
        label,
        count: 0
      }
    ])
  );

  for (const product of sourceProducts) {
    const categoryId = Number(product.categoryId ?? 0);
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
}
