<script setup>
import { computed, inject, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { formatCurrency } from "../utils/format";

const mallApp = inject("mallApp");
const router = useRouter();

const selectedItems = computed(() => mallApp.selectedNormalCartItems);
const summary = computed(() => mallApp.checkoutSummary);
const canSubmitPay = computed(() => {
  return summary.value.count > 0 && mallApp.checkoutFormComplete;
});

onMounted(async () => {
  if (!selectedItems.value.length) {
    await mallApp.loadCartItems();
  }

  if (!selectedItems.value.length) {
    ElMessage.warning("请先在购物车中勾选需要结算的商品");
    router.replace({ name: "app-cart" });
  }
});

async function handlePay() {
  if (!mallApp.checkoutFormComplete) {
    ElMessage.warning("请完整填写收货人、手机号和收货地址后再支付");
    return;
  }

  const paidOrder = await mallApp.submitCheckoutAndPay();
  if (!paidOrder) {
    return;
  }

  await router.replace({ name: "app-cart" });
}

function backToCart() {
  router.push({ name: "app-cart" });
}
</script>

<template>
  <div class="checkout-standalone-shell">
    <div class="checkout-standalone-inner">
      <section class="checkout-standalone-hero section-card">
        <div>
          <p class="eyebrow">Normal Checkout</p>
          <h1>普通商品结算</h1>
          <p>这里是独立结算界面，确认购物车已选商品与收货信息后，可直接完成模拟支付。</p>
        </div>
        <div class="checkout-hero-summary">
          <span>已选 {{ selectedItems.length }} 种 / {{ summary.count }} 件</span>
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

          <div v-if="selectedItems.length" class="checkout-item-list">
            <article
              v-for="item in selectedItems"
              :key="item.cartKey"
              class="checkout-item-card"
            >
              <div class="cart-thumb checkout-item-thumb">{{ item.name.slice(0, 2) }}</div>

              <div class="checkout-item-main">
                <strong>{{ item.name }}</strong>
                <small>{{ item.subtitle || "普通商品" }}</small>
                <el-tag type="success" effect="plain">已选中结算</el-tag>
              </div>

              <div class="checkout-item-meta">
                <span>数量 x{{ item.quantity }}</span>
                <strong>{{ formatCurrency(mallApp.getCartItemPrice(item) * item.quantity) }}</strong>
              </div>
            </article>
          </div>

          <el-empty
            v-else
            description="当前没有已选中的购物车商品，请先返回购物车进行勾选"
          />
        </section>

        <aside class="checkout-side-panel">
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
            <p class="eyebrow">Receiver Form</p>
            <h3>收货信息</h3>
            <el-form label-position="top" class="checkout-form">
              <el-form-item label="收货人">
                <el-input
                  v-model="mallApp.checkoutForm.receiver"
                  placeholder="例如 Neo"
                />
              </el-form-item>
              <el-form-item label="手机号">
                <el-input
                  v-model="mallApp.checkoutForm.mobile"
                  placeholder="例如 13800000000"
                />
              </el-form-item>
              <el-form-item label="收货地址">
                <el-input
                  v-model="mallApp.checkoutForm.detail"
                  type="textarea"
                  :rows="4"
                  placeholder="填写地址后会作为地址快照传给后端"
                />
              </el-form-item>
              <el-form-item label="订单备注">
                <el-input
                  v-model="mallApp.checkoutForm.remark"
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
          <span>点击后会先创建普通订单，再立即完成模拟支付，然后自动返回购物车页面。</span>
        </div>

        <div class="checkout-paybar-action">
          <strong>{{ formatCurrency(summary.total) }}</strong>
          <el-button
            type="danger"
            size="large"
            :loading="mallApp.checkoutLoading"
            :disabled="!canSubmitPay"
            @click="handlePay"
          >
            模拟支付成功后返回购物车
          </el-button>
        </div>
      </section>
    </div>
  </div>
</template>
