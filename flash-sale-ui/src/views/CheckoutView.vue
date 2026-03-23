<script setup>
import { computed, inject, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { formatCurrency } from "../utils/format";

const mallApp = inject("mallApp");
const router = useRouter();
const payConfirmVisible = ref(false);

const selectedItems = computed(() => mallApp.selectedNormalCartItems);
const pendingOrder = computed(() => mallApp.pendingCheckoutOrder);
const displayItems = computed(() => {
  if (pendingOrder.value?.items?.length) {
    return pendingOrder.value.items;
  }
  return selectedItems.value;
});
const summary = computed(() => {
  if (pendingOrder.value) {
    const count = (pendingOrder.value.items || []).reduce(
      (total, item) => total + Number(item.quantity || 0),
      0
    );
    return {
      count,
      total: Number(pendingOrder.value.payAmount ?? pendingOrder.value.totalAmount ?? 0)
    };
  }
  return mallApp.checkoutSummary;
});
const currentAddress = computed(() => {
  if (pendingOrder.value) {
    return {
      receiver: pendingOrder.value.receiver,
      mobile: pendingOrder.value.mobile,
      detail: pendingOrder.value.detail
    };
  }
  return mallApp.selectedAddress;
});
const checkoutLocked = computed(() => Boolean(pendingOrder.value));
const canSubmitPay = computed(() => {
  if (pendingOrder.value) {
    return true;
  }
  return summary.value.count > 0 && mallApp.checkoutFormComplete;
});

onMounted(async () => {
  await Promise.all([mallApp.loadCartItems(), mallApp.loadAddresses(), mallApp.loadOrders()]);

  if (!selectedItems.value.length && !pendingOrder.value) {
    ElMessage.warning("请先在购物车中勾选需要结算的商品");
    router.replace({ name: "app-cart" });
  }
});

async function openPayConfirm() {
  if (pendingOrder.value) {
    payConfirmVisible.value = true;
    return;
  }

  if (!mallApp.checkoutFormComplete) {
    ElMessage.warning("请选择收货地址后再支付");
    return;
  }

  const createdOrder = await mallApp.createPendingCheckoutOrder();
  if (!createdOrder) {
    return;
  }

  payConfirmVisible.value = true;
}

async function leaveForLater() {
  // 待支付订单已在点击“去确认支付”时创建，这里只保留订单并跳去订单中心继续处理。
  const heldOrder = mallApp.holdPendingCheckoutOrder();
  payConfirmVisible.value = false;
  if (heldOrder) {
    await router.replace({ name: "app-orders" });
  }
}

function cancelPayConfirm() {
  if (pendingOrder.value) {
    leaveForLater();
    return;
  }
  payConfirmVisible.value = false;
}

async function confirmPay() {
  // 支付确认层优先消费已经创建好的待支付订单，避免重复创建订单。
  const paidOrder = pendingOrder.value
    ? await mallApp.payOrder(pendingOrder.value)
    : await mallApp.submitCheckoutAndPay();
  if (!paidOrder) {
    return;
  }

  payConfirmVisible.value = false;
  await router.replace({ name: "app-cart" });
}

function handlePayConfirmBeforeClose(done) {
  if (mallApp.checkoutLoading) {
    return;
  }
  if (!pendingOrder.value) {
    done();
    return;
  }

  const heldOrder = mallApp.holdPendingCheckoutOrder();
  done();
  if (heldOrder) {
    router.replace({ name: "app-orders" });
  }
}

function backToCart() {
  router.push({ name: "app-cart" });
}

function goToAddressManage() {
  router.push({ name: "app-account-profile" });
}
</script>

<template>
  <div class="checkout-standalone-shell">
    <div class="checkout-standalone-inner">
      <section class="checkout-standalone-hero section-card">
        <div>
          <p class="eyebrow">Normal Checkout</p>
          <h1>普通商品结算</h1>
          <p>
            先确认本次结算商品、收货地址和订单备注，再进入支付确认层。
            点击“去确认支付”后会先创建一笔待支付普通订单，你可以立即支付，也可以稍后到订单中心继续处理。
          </p>
        </div>
        <div class="checkout-hero-summary">
          <span>已选 {{ displayItems.length }} 种 / {{ summary.count }} 件</span>
          <strong>{{ formatCurrency(summary.total) }}</strong>
        </div>
      </section>

      <div class="checkout-layout">
        <section class="section-card">
          <div class="section-head">
            <div>
              <p class="eyebrow">Selected Items</p>
              <h3>商品明细</h3>
            </div>
            <el-button text @click="backToCart">返回购物车</el-button>
          </div>

          <div v-if="displayItems.length" class="checkout-item-list">
            <article
              v-for="item in displayItems"
              :key="item.id || item.cartKey || item.productId"
              class="checkout-item-card"
            >
              <div class="cart-thumb checkout-item-thumb">{{ (item.name || item.productName || "").slice(0, 2) }}</div>

              <div class="checkout-item-main">
                <strong>{{ item.name || item.productName }}</strong>
                <small>{{ item.subtitle || item.productSubtitle || "普通商品" }}</small>
                <el-tag type="success" effect="plain">
                  {{ pendingOrder ? "待确认支付" : "已选中结算" }}
                </el-tag>
              </div>

              <div class="checkout-item-meta">
                <span>数量 x{{ item.quantity }}</span>
                <strong>
                  {{
                    formatCurrency(
                      pendingOrder
                        ? item.itemAmount
                        : mallApp.getCartItemPrice(item) * item.quantity
                    )
                  }}
                </strong>
              </div>
            </article>
          </div>

          <el-empty
            v-else
            description="当前没有可结算的购物车商品，请先返回购物车进行勾选。"
          />
        </section>

        <aside class="checkout-side-panel">
          <section class="sidebar-card">
            <p class="eyebrow">Saved Address</p>
            <h3>地址选择</h3>
            <div class="preview-stack preview-card-column">
              <el-select
                v-model="mallApp.selectedAddressId"
                class="checkout-address-select"
                :loading="mallApp.addressesLoading"
                :disabled="checkoutLocked"
                clearable
                placeholder="请选择已保存的收货地址"
                @change="mallApp.handleCheckoutAddressChange"
              >
                <el-option
                  v-for="address in mallApp.addresses"
                  :key="address.id"
                  :label="`${address.receiver} / ${address.mobile}`"
                  :value="address.id"
                >
                  <div class="checkout-address-option">
                    <strong>{{ address.receiver }} {{ address.mobile }}</strong>
                    <span>{{ address.detail }}</span>
                  </div>
                </el-option>
              </el-select>

              <article v-if="currentAddress" class="preview-card">
                <div>
                  <small>{{ pendingOrder ? "订单地址" : "当前地址" }}</small>
                  <strong>{{ currentAddress.receiver }} {{ currentAddress.mobile }}</strong>
                  <span>{{ currentAddress.detail }}</span>
                </div>
              </article>

              <article v-else class="preview-card">
                <div>
                  <small>提示</small>
                  <strong>暂无已保存地址</strong>
                  <span>请先去账户信息页新增收货地址，再返回当前页面完成结算。</span>
                </div>
              </article>

              <el-button plain @click="goToAddressManage">去管理地址</el-button>
            </div>
          </section>

          <section class="sidebar-card">
            <p class="eyebrow">Checkout Summary</p>
            <h3>结算信息</h3>
            <div class="preview-stack">
              <article class="preview-card">
                <div>
                  <small>可结算件数</small>
                  <strong>{{ summary.count }}</strong>
                </div>
              </article>
              <article class="preview-card">
                <div>
                  <small>普通商品总额</small>
                  <strong>{{ formatCurrency(summary.total) }}</strong>
                </div>
              </article>
            </div>
          </section>

          <section class="sidebar-card">
            <p class="eyebrow">Order Remark</p>
            <h3>订单备注</h3>
            <el-form label-position="top" class="checkout-form">
              <el-form-item label="备注信息">
                <el-input
                  v-model="mallApp.checkoutForm.remark"
                  :disabled="checkoutLocked"
                  placeholder="例如 工作日白天送达"
                />
              </el-form-item>
            </el-form>
          </section>
        </aside>
      </div>

      <section class="checkout-paybar section-card">
        <div>
          <p class="eyebrow">Pay Action</p>
          <h3>模拟支付</h3>
          <span>
            点击后会先创建待支付普通订单，再展示本次支付确认信息。
            你可以在确认层里取消支付或继续确认支付。
          </span>
        </div>

        <div class="checkout-paybar-action">
          <strong>{{ formatCurrency(summary.total) }}</strong>
          <el-button
            type="danger"
            size="large"
            :disabled="!canSubmitPay"
            @click="openPayConfirm"
          >
            去确认支付
          </el-button>
        </div>
      </section>
    </div>

    <el-dialog
      v-model="payConfirmVisible"
      width="820px"
      destroy-on-close
      :close-on-click-modal="false"
      :before-close="handlePayConfirmBeforeClose"
      class="pay-confirm-dialog"
      title="确认模拟支付"
    >
      <div class="pay-confirm-body">
        <section class="pay-confirm-section">
          <div class="section-head">
            <div>
              <p class="eyebrow">Pay Items</p>
              <h3>商品详情</h3>
            </div>
          </div>

          <div class="checkout-item-list">
            <article
              v-for="item in displayItems"
              :key="`confirm-${item.id || item.cartKey || item.productId}`"
              class="checkout-item-card"
            >
              <div class="cart-thumb checkout-item-thumb">{{ (item.name || item.productName || "").slice(0, 2) }}</div>

              <div class="checkout-item-main">
                <strong>{{ item.name || item.productName }}</strong>
                <small>{{ item.subtitle || item.productSubtitle || "普通商品" }}</small>
                <el-tag type="success" effect="plain">待确认支付</el-tag>
              </div>

              <div class="checkout-item-meta">
                <span>数量 x{{ item.quantity }}</span>
                <strong>
                  {{
                    formatCurrency(
                      pendingOrder
                        ? item.itemAmount
                        : mallApp.getCartItemPrice(item) * item.quantity
                    )
                  }}
                </strong>
              </div>
            </article>
          </div>
        </section>

        <section class="pay-confirm-grid">
          <article class="preview-card">
            <div>
              <small>收货地址</small>
              <strong>
                {{
                  currentAddress
                    ? `${currentAddress.receiver} ${currentAddress.mobile}`
                    : "未选择地址"
                }}
              </strong>
              <span>{{ currentAddress?.detail || "请选择地址后再支付" }}</span>
            </div>
          </article>

          <article class="preview-card">
            <div>
              <small>订单备注</small>
              <strong>{{ pendingOrder?.remark || mallApp.checkoutForm.remark || "无备注" }}</strong>
              <span>取消支付后，这笔订单会保留为待支付状态。</span>
            </div>
          </article>

          <article class="preview-card">
            <div>
              <small>支付总额</small>
              <strong>{{ formatCurrency(summary.total) }}</strong>
              <span>共 {{ summary.count }} 件普通商品</span>
            </div>
          </article>
        </section>
      </div>

      <template #footer>
        <div class="dialog-actions-right pay-confirm-actions">
          <el-button size="large" @click="cancelPayConfirm">取消支付</el-button>
          <el-button
            type="danger"
            size="large"
            :loading="mallApp.checkoutLoading"
            @click="confirmPay"
          >
            确认支付
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
