<script setup>
import { computed, inject } from "vue";
import { formatCurrency, getCountdownText } from "../utils/format";

const mallApp = inject("mallApp");

const recommendations = computed(() => {
  return mallApp.products
    .filter((product) => !mallApp.isInCart(product.id, "normal"))
    .slice(0, 4);
});

function getItemMeta(item) {
  if (item.productType === "seckill") {
    return getCountdownText(item.startTime, item.endTime, mallApp.currentTime);
  }
  return item.subtitle || "普通商品";
}
</script>

<template>
  <div class="page-stack">
    <section class="cart-page-header section-card">
      <div>
        <p class="eyebrow">Cart Workspace</p>
        <h2>购物车</h2>
        <p>当前仍是前端本地购物车草案，但现在已经能同时容纳普通商品和秒杀商品。</p>
      </div>
      <div class="cart-page-summary">
        <span>件数 {{ mallApp.cartSummary.count }}</span>
        <strong>{{ formatCurrency(mallApp.cartSummary.total) }}</strong>
      </div>
    </section>

    <div class="cart-desktop-layout">
      <section class="section-card">
        <div v-if="mallApp.cartItems.length" class="cart-table">
          <div class="cart-table-head">
            <span>商品</span>
            <span>商品类型</span>
            <span>数量</span>
            <span>价格</span>
            <span>操作</span>
          </div>
          <article
            v-for="item in mallApp.cartItems"
            :key="item.cartKey"
            class="cart-table-row"
          >
            <div class="cart-col cart-col-product">
              <div class="cart-thumb">{{ item.name.slice(0, 2) }}</div>
              <div>
                <strong>{{ item.name }}</strong>
                <small>{{ getItemMeta(item) }}</small>
              </div>
            </div>
            <div class="cart-col">
              <span>{{ item.productType === "seckill" ? "秒杀商品" : "普通商品" }}</span>
            </div>
            <div class="cart-col">
              <div class="qty-row">
                <el-button circle @click="mallApp.updateCartQuantity(item.cartKey, -1)">-</el-button>
                <span>{{ item.quantity }}</span>
                <el-button circle @click="mallApp.updateCartQuantity(item.cartKey, 1)">+</el-button>
              </div>
            </div>
            <div class="cart-col">
              <strong>{{ formatCurrency(mallApp.getCartItemPrice(item)) }}</strong>
            </div>
            <div class="cart-col cart-col-actions">
              <el-button text @click="mallApp.removeFromCart(item.cartKey)">删除</el-button>
            </div>
          </article>
        </div>
        <el-empty
          v-else
          description="购物车还是空的，先去首页或闪购页加点商品"
        />
      </section>

      <aside class="cart-side-panel">
        <section class="sidebar-card">
          <p class="eyebrow">Summary</p>
          <h3>结算摘要</h3>
          <div class="sidebar-metrics">
            <div>
              <span>商品件数</span>
              <strong>{{ mallApp.cartSummary.count }}</strong>
            </div>
            <div>
              <span>合计金额</span>
              <strong>{{ formatCurrency(mallApp.cartSummary.total) }}</strong>
            </div>
          </div>
          <el-button type="danger" disabled>等待后端 checkout 接口</el-button>
        </section>

        <section class="sidebar-card">
          <div class="sidebar-head">
            <div>
              <p class="eyebrow">Recommend</p>
              <h3>猜你喜欢</h3>
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
