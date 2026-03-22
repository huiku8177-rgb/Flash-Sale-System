<script setup>
import { computed, inject } from "vue";
import { useRouter } from "vue-router";
import { formatCurrency } from "../utils/format";

const mallApp = inject("mallApp");
const router = useRouter();

const recommendations = computed(() => {
  return mallApp.products
    .filter((product) => !mallApp.isInCart(product.id, "normal"))
    .slice(0, 4);
});

function goCheckout() {
  if (!mallApp.checkoutSummary.count) {
    return;
  }
  router.push({ name: "checkout" });
}
</script>

<template>
  <div class="page-stack">
    <section class="cart-page-header section-card">
      <div>
        <p class="eyebrow">Cart Workspace</p>
        <h2>购物车明细</h2>
        <p>先在这里勾选需要结算的普通商品，再进入独立结算页完成模拟支付。</p>
      </div>
      <div class="cart-page-summary">
        <span>购物车商品数 {{ mallApp.cartSummary.count }}</span>
        <strong>{{ formatCurrency(mallApp.cartSummary.total) }}</strong>
      </div>
    </section>

    <div class="cart-desktop-layout">
      <section class="section-card">
        <div class="section-head">
          <div>
            <p class="eyebrow">Cart Items</p>
            <h3>购物车明细</h3>
          </div>
          <el-button text @click="mallApp.loadCartItems">刷新购物车</el-button>
        </div>

        <div v-if="mallApp.normalCartItems.length" class="cart-table">
          <div class="cart-table-head cart-table-head-rich">
            <span>选择</span>
            <span>商品</span>
            <span>状态</span>
            <span>数量</span>
            <span>价格</span>
            <span>操作</span>
          </div>

          <article
            v-for="item in mallApp.normalCartItems"
            :key="item.cartKey"
            class="cart-table-row cart-table-row-rich"
          >
            <div class="cart-col cart-col-check">
              <el-checkbox
                :model-value="item.selected"
                :disabled="!item.canCheckout"
                @change="mallApp.updateCartSelected(item.cartKey, $event)"
              />
            </div>

            <div class="cart-col cart-col-product">
              <div class="cart-thumb">{{ item.name.slice(0, 2) }}</div>
              <div>
                <strong>{{ item.name }}</strong>
                <small>{{ item.subtitle || "普通商品" }}</small>
              </div>
            </div>

            <div class="cart-col">
              <el-tag :type="item.canCheckout ? 'success' : 'warning'" effect="plain">
                {{ item.canCheckout ? "可结算" : "暂不可结算" }}
              </el-tag>
            </div>

            <div class="cart-col">
              <div class="qty-row">
                <el-button circle @click="mallApp.updateCartQuantity(item.cartKey, -1)">-</el-button>
                <span>{{ item.quantity }}</span>
                <el-button circle @click="mallApp.updateCartQuantity(item.cartKey, 1)">+</el-button>
              </div>
            </div>

            <div class="cart-col">
              <strong>{{ formatCurrency(mallApp.getCartItemPrice(item) * item.quantity) }}</strong>
              <small>单价 {{ formatCurrency(mallApp.getCartItemPrice(item)) }}</small>
            </div>

            <div class="cart-col cart-col-actions">
              <el-button text @click="mallApp.openProduct(item, item.productType)">详情</el-button>
              <el-button text @click="mallApp.removeFromCart(item.cartKey)">移除</el-button>
            </div>
          </article>
        </div>

        <el-empty
          v-else
          description="购物车还是空的，先去首页挑选一些普通商品吧"
        />

        <div v-if="mallApp.normalCartItems.length" class="cart-bottom-bar">
          <label class="cart-select-all">
            <el-checkbox
              :model-value="mallApp.normalCartAllSelected"
              @change="mallApp.toggleAllNormalCart($event)"
            />
            <span>全选</span>
          </label>

          <div class="cart-bottom-summary">
            <span>已选 {{ mallApp.selectedNormalCartItems.length }} 种 / {{ mallApp.checkoutSummary.count }} 件</span>
            <strong>合计 {{ formatCurrency(mallApp.checkoutSummary.total) }}</strong>
          </div>

          <el-button
            type="danger"
            size="large"
            :disabled="!mallApp.checkoutSummary.count"
            @click="goCheckout"
          >
            去结算
          </el-button>
        </div>
      </section>

      <aside class="cart-side-panel">
        <section class="sidebar-card">
          <p class="eyebrow">Checkout Hint</p>
          <h3>结算说明</h3>
          <div class="side-bullet-list compact-list">
            <div>勾选普通商品后，可进入独立结算页确认商品明细。</div>
            <div>结算页会填写收货信息，并在底部完成模拟支付。</div>
            <div>支付成功后会自动返回购物车页，并刷新当前购物车数据。</div>
          </div>
        </section>

        <section class="sidebar-card">
          <div class="sidebar-head">
            <div>
              <p class="eyebrow">Recommend</p>
              <h3>继续加购</h3>
            </div>
          </div>
          <div class="sidebar-cart-list">
            <article
              v-for="product in recommendations"
              :key="product.id"
              class="sidebar-cart-item"
            >
              <strong>{{ product.name }}</strong>
              <small>{{ formatCurrency(product.price) }}</small>
              <button type="button" @click="mallApp.addToCart(product, 'normal')">加入购物车</button>
            </article>
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>
