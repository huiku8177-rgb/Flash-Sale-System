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
  return item.subtitle || "普通商品支持直接走 checkout 下单接口。";
}
</script>

<template>
  <div class="page-stack">
    <section class="cart-page-header section-card">
      <div>
        <p class="eyebrow">Cart Workspace</p>
        <h2>购物车与结算</h2>
        <p>普通商品可直接创建订单，秒杀商品继续作为抢购草稿保留，正式秒杀仍需在秒杀会场发起。</p>
      </div>
      <div class="cart-page-summary">
        <span>总件数 {{ mallApp.cartSummary.count }}</span>
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
          <el-button text @click="mallApp.loadProducts">刷新普通商品</el-button>
        </div>

        <div v-if="mallApp.cartItems.length" class="cart-table">
          <div class="cart-table-head">
            <span>商品</span>
            <span>类型</span>
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
              <el-tag :type="item.productType === 'seckill' ? 'danger' : 'success'" effect="plain">
                {{ item.productType === "seckill" ? "秒杀草稿" : "普通商品" }}
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
              <strong>{{ formatCurrency(mallApp.getCartItemPrice(item)) }}</strong>
            </div>

            <div class="cart-col cart-col-actions">
              <el-button text @click="mallApp.openProduct(item, item.productType)">详情</el-button>
              <el-button text @click="mallApp.removeFromCart(item.cartKey)">移除</el-button>
            </div>
          </article>
        </div>

        <el-empty
          v-else
          description="购物车还是空的，先去首页或秒杀会场挑些商品吧"
        />
      </section>

      <aside class="cart-side-panel">
        <section class="sidebar-card">
          <p class="eyebrow">Normal Checkout</p>
          <h3>普通商品结算</h3>
          <div class="sidebar-metrics">
            <div>
              <span>可结算件数</span>
              <strong>{{ mallApp.checkoutSummary.count }}</strong>
            </div>
            <div>
              <span>普通商品总额</span>
              <strong>{{ formatCurrency(mallApp.checkoutSummary.total) }}</strong>
            </div>
          </div>

          <el-form class="checkout-form" label-position="top">
            <el-form-item label="收货人">
              <el-input v-model="mallApp.checkoutForm.receiver" placeholder="例如 Neo" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="mallApp.checkoutForm.mobile" placeholder="例如 13800000000" />
            </el-form-item>
            <el-form-item label="收货地址">
              <el-input
                v-model="mallApp.checkoutForm.detail"
                type="textarea"
                :rows="3"
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

          <el-button
            type="danger"
            :disabled="!mallApp.normalCartItems.length"
            :loading="mallApp.checkoutLoading"
            @click="mallApp.checkoutNormalCart"
          >
            创建普通订单
          </el-button>
        </section>

        <section class="sidebar-card">
          <p class="eyebrow">Seckill Draft</p>
          <h3>秒杀草稿提醒</h3>
          <div class="side-bullet-list compact-list">
            <div>秒杀商品不会在购物车直接下单。</div>
            <div>请前往“秒杀会场”发起抢购，系统会异步创建秒杀订单。</div>
            <div>抢购成功后可在个人中心继续模拟支付和查看状态。</div>
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
