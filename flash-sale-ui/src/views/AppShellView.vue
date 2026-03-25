 <script setup>
import { ArrowDown } from "@element-plus/icons-vue";
import { computed, inject } from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import { logout } from "../api/auth";
import { clearSession } from "../stores/auth";
import {
  formatCurrency,
  formatDateTime,
  getCountdownText,
  getOrderStatusText,
  getOrderStatusType,
  getOrderTypeText
} from "../utils/format";

const route = useRoute();
const router = useRouter();
const mallApp = inject("mallApp");

const navItems = [
  { label: "首页", routeName: "app-home" },
  { label: "秒杀", routeName: "app-flash" },
  { label: "购物车", routeName: "app-cart" }
];

const profileMenuItems = [
  { label: "个人中心", routeName: "app-profile" },
  { label: "我的订单", routeName: "app-orders" },
  { label: "账户信息", routeName: "app-account-profile" },
  { label: "修改密码", routeName: "app-account-security" }
];

const hotKeywords = ["耳机", "键盘", "零食", "显示器", "饮料"];

const isAuthenticated = computed(() => Boolean(mallApp.authState.token));

const currentTitle = computed(() => {
  const mapping = {
    "app-home": "首页推荐",
    "app-flash": "秒杀会场",
    "app-cart": "购物车",
    "app-profile": "个人中心",
    "app-orders": "我的订单",
    "app-account-profile": "账户信息",
    "app-account-security": "修改密码"
  };
  return mapping[route.name] || "商城首页";
});

const isProfileRoute = computed(() =>
  ["app-profile", "app-orders", "app-account-profile", "app-account-security"].includes(
    route.name
  )
);

// 只有首页保留右侧栏，其它页面改成单列内容，避免商城主视图被信息卡打断。
const showHomeSidebar = computed(() => route.name === "app-home");

const productApiPath = computed(() =>
  mallApp.productDetailType === "seckill"
    ? "/seckill-product/products/{id}"
    : "/product/products/{id}"
);

const selectedOrderAmount = computed(() =>
  mallApp.selectedOrder ? mallApp.getOrderDisplayAmount(mallApp.selectedOrder) : 0
);

const seckillPayOrderAmount = computed(() =>
  mallApp.pendingSeckillPayOrder
    ? mallApp.getOrderDisplayAmount(mallApp.pendingSeckillPayOrder)
    : 0
);

const searchKeyword = computed({
  get() {
    return mallApp.productFilters.name;
  },
  set(value) {
    mallApp.productFilters.name = value;
  }
});

async function handleLogout() {
  try {
    await logout();
  } catch {
    // 即使退出接口失败，也要清掉本地会话，防止页面停留在伪登录态。
  } finally {
    clearSession();
    router.push({ name: "login" });
  }
}

function goLogin() {
  router.push({ name: "login" });
}

function goRegister() {
  router.push({ name: "register" });
}

async function handleSearch() {
  await mallApp.loadProducts();
  router.push({ name: "app-home" });
  ElMessage.success("已按关键词刷新商品列表");
}

async function handleBuyNowFromDetail() {
  const ready = await mallApp.prepareImmediateCheckout(mallApp.productDetail);
  if (ready) {
    router.push({ name: "checkout" });
  }
}

function jumpNav(routeName) {
  router.push({ name: routeName });
}

function handleProfileCommand(routeName) {
  router.push({ name: routeName });
}

function handleProfileEntry() {
  if (!isAuthenticated.value) {
    goLogin();
    return;
  }
  router.push({ name: "app-profile" });
}

function handleOrderPay(order) {
  if (order.orderType === "seckill") {
    mallApp.openSeckillPayConfirm(order);
    return;
  }
  mallApp.payOrder(order);
}

function handleSeckillPayConfirmBeforeClose(done) {
  mallApp.holdPendingSeckillPayOrder();
  done();
}

function fillKeyword(keyword) {
  searchKeyword.value = keyword;
  handleSearch();
}

function getSidebarOrderNumber(order) {
  return order.orderNo || `#${order.id}`;
}
</script>

<template>
  <div class="web-shell">
    <div class="top-utility-bar">
      <div class="utility-inner">
        <div class="utility-links utility-links--left">
          <template v-if="isAuthenticated">
            <span>欢迎回来，{{ mallApp.profileDisplayName }}</span>
            <el-dropdown trigger="hover" @command="handleProfileCommand">
              <button type="button" class="utility-menu-button">
                我的中心
                <el-icon class="utility-menu-icon"><ArrowDown /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="item in profileMenuItems"
                    :key="item.routeName"
                    :command="item.routeName"
                  >
                    {{ item.label }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <span>欢迎来到 Flash Sale Mall</span>
            <button type="button" @click="goLogin">请登录</button>
            <button type="button" @click="goRegister">免费注册</button>
          </template>
        </div>

        <div class="utility-links utility-links--right">
          <span>当前页面：{{ currentTitle }}</span>
          <template v-if="isAuthenticated">
            <button type="button" @click="jumpNav('app-orders')">我的订单</button>
            <button type="button" @click="handleLogout">退出登录</button>
          </template>
          <template v-else>
            <button type="button" @click="goLogin">登录</button>
            <button type="button" @click="goRegister">注册</button>
          </template>
        </div>
      </div>
    </div>

    <header class="site-header">
      <div class="site-header-inner">
        <!-- 主头部只保留品牌、搜索和购物车，把后台味较重的刷新动作下沉到页面内容区。 -->
        <div class="site-header-main">
          <button class="brand-block" type="button" @click="jumpNav('app-home')">
            <span class="brand-mark">FS</span>
            <div>
              <strong>Flash Sale Mall</strong>
              <small>普通商品与秒杀商品一体化前台</small>
            </div>
          </button>

          <div class="search-stack">
            <div class="search-bar">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索普通商品，例如：耳机、键盘、零食"
                size="large"
                @keyup.enter="handleSearch"
              />
              <el-button type="danger" size="large" @click="handleSearch">搜索</el-button>
            </div>
            <div class="hot-keywords">
              <span>热门搜索</span>
              <button
                v-for="keyword in hotKeywords"
                :key="keyword"
                type="button"
                class="hot-keyword-pill"
                @click="fillKeyword(keyword)"
              >
                {{ keyword }}
              </button>
            </div>
          </div>

          <div class="header-actions">
            <el-badge :value="mallApp.cartSummary.count" :hidden="!mallApp.cartSummary.count">
              <el-button plain @click="jumpNav('app-cart')">购物车</el-button>
            </el-badge>
            <el-button
              v-if="isAuthenticated"
              plain
              @click="jumpNav('app-orders')"
            >
              我的订单
            </el-button>
            <el-button v-else plain @click="goLogin">登录</el-button>
          </div>
        </div>

        <!-- 导航与个人入口并入主头部底部，形成两层信息结构。 -->
        <nav class="site-header-nav" aria-label="主导航">
          <div class="site-nav-group">
            <span class="site-nav-label">全部频道</span>

            <button
              v-for="item in navItems"
              :key="item.routeName"
              type="button"
              class="site-nav-item"
              :class="{ active: route.name === item.routeName }"
              @click="jumpNav(item.routeName)"
            >
              {{ item.label }}
            </button>
          </div>

          <div class="site-nav-actions">
            <template v-if="isAuthenticated">
              <el-dropdown trigger="hover" @command="handleProfileCommand">
                <button
                  type="button"
                  class="site-nav-item site-nav-item--menu"
                  :class="{ active: isProfileRoute }"
                >
                  个人中心
                  <el-icon><ArrowDown /></el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-for="item in profileMenuItems"
                      :key="item.routeName"
                      :command="item.routeName"
                    >
                      {{ item.label }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
            <template v-else>
              <button type="button" class="site-nav-item site-nav-item--menu" @click="handleProfileEntry">
                登录 / 注册
              </button>
            </template>
          </div>
        </nav>
      </div>
    </header>

    <div class="portal-frame" :class="{ 'portal-frame--home': showHomeSidebar }">
      <main class="portal-main">
        <router-view />
      </main>

      <aside v-if="showHomeSidebar" class="portal-sidebar">
        <section class="sidebar-card">
          <div class="sidebar-head">
            <div>
              <p class="eyebrow">Latest Orders</p>
              <h3>最近订单</h3>
            </div>
            <el-button text @click="jumpNav('app-orders')">更多</el-button>
          </div>
          <div class="sidebar-order-list">
            <template v-if="isAuthenticated">
              <article
                v-for="order in mallApp.recentOrders"
                :key="`${order.orderType}-${order.id}`"
                class="sidebar-order-item"
              >
                <div class="sidebar-order-info">
                  <span class="sidebar-order-label">订单号</span>
                  <strong class="sidebar-order-no" :title="getSidebarOrderNumber(order)">
                    {{ getSidebarOrderNumber(order) }}
                  </strong>
                  <div class="sidebar-order-subline">
                    <small>{{ formatDateTime(order.createTime) }}</small>
                    <em>{{ formatCurrency(mallApp.getOrderDisplayAmount(order)) }}</em>
                  </div>
                </div>
                <div class="sidebar-order-meta">
                  <span class="sidebar-order-tag">{{ getOrderTypeText(order.orderType) }}</span>
                  <span
                    class="sidebar-order-tag sidebar-order-tag--status"
                    :class="`is-${getOrderStatusType(order.status)}`"
                  >
                    {{ getOrderStatusText(order.status) }}
                  </span>
                  <button type="button" @click="mallApp.openOrder(order)">详情</button>
                </div>
              </article>
              <el-empty
                v-if="!mallApp.recentOrders.length && !mallApp.ordersLoading"
                description="暂无订单"
                :image-size="80"
              />
            </template>
            <el-empty v-else description="登录后可查看最近订单" :image-size="80" />
          </div>
        </section>

        <section class="sidebar-card">
          <div class="sidebar-head">
            <div>
              <p class="eyebrow">Cart Draft</p>
              <h3>购物车草稿</h3>
            </div>
            <el-button text @click="jumpNav('app-cart')">查看</el-button>
          </div>
          <div class="sidebar-cart-list">
            <template v-if="isAuthenticated">
              <article
                v-for="item in mallApp.cartItems.slice(0, 3)"
                :key="item.cartKey"
                class="sidebar-cart-item"
              >
                <strong>{{ item.name }}</strong>
                <small>
                  数量 x{{ item.quantity }}，金额 {{ formatCurrency(mallApp.getCartItemPrice(item)) }}
                </small>
              </article>
              <el-empty
                v-if="!mallApp.cartItems.length"
                description="购物车为空"
                :image-size="80"
              />
            </template>
            <el-empty v-else description="登录后可保存购物车商品" :image-size="80" />
          </div>
        </section>
      </aside>
    </div>

    <el-drawer
      v-model="mallApp.productDetailVisible"
      size="440px"
      title="商品详情"
      destroy-on-close
      class="detail-panel"
    >
      <el-skeleton :rows="6" animated :loading="mallApp.productDetailLoading">
        <template v-if="mallApp.productDetail">
          <div class="detail-stack">
            <div class="detail-hero">
              <span class="detail-badge">
                {{ mallApp.productDetailType === "seckill" ? "秒杀商品" : "普通商品" }}
                #{{ mallApp.productDetail.id }}
              </span>
              <h3>{{ mallApp.productDetail.name }}</h3>
              <p>
                详情数据来自 <code>{{ productApiPath }}</code>，前端会按商品类型自动切换接口。
              </p>
            </div>

            <div class="detail-grid">
              <div>
                <span>售价</span>
                <strong>{{ formatCurrency(mallApp.productDetail.price) }}</strong>
              </div>
              <div>
                <span>库存</span>
                <strong>{{ mallApp.productDetail.stock }}</strong>
              </div>
              <div>
                <span>状态</span>
                <strong>{{ mallApp.productDetail.status === 1 ? "上架中" : "已下架" }}</strong>
              </div>

              <template v-if="mallApp.productDetailType === 'seckill'">
                <div>
                  <span>秒杀价</span>
                  <strong>{{ formatCurrency(mallApp.productDetail.seckillPrice) }}</strong>
                </div>
                <div>
                  <span>开始时间</span>
                  <strong>{{ formatDateTime(mallApp.productDetail.startTime) }}</strong>
                </div>
                <div>
                  <span>结束时间</span>
                  <strong>{{ formatDateTime(mallApp.productDetail.endTime) }}</strong>
                </div>
              </template>

              <template v-else>
                <div>
                  <span>划线价</span>
                  <strong>{{ formatCurrency(mallApp.productDetail.marketPrice) }}</strong>
                </div>
                <div>
                  <span>副标题</span>
                  <strong>{{ mallApp.productDetail.subtitle || "--" }}</strong>
                </div>
                <div>
                  <span>分类 ID</span>
                  <strong>{{ mallApp.productDetail.categoryId || "--" }}</strong>
                </div>
              </template>
            </div>

            <div class="detail-hero detail-hero--soft">
              <h3>{{ mallApp.productDetailType === "seckill" ? "活动状态" : "补充说明" }}</h3>
              <p v-if="mallApp.productDetailType === 'seckill'">
                {{
                  getCountdownText(
                    mallApp.productDetail.startTime,
                    mallApp.productDetail.endTime,
                    mallApp.currentTime
                  )
                }}
              </p>
              <template v-else>
                <p>主图：{{ mallApp.productDetail.mainImage || "当前未配置" }}</p>
                <p>{{ mallApp.productDetail.detail || "当前商品详情文案尚未补充。" }}</p>
              </template>
            </div>

            <div class="dialog-actions dialog-actions--panel">
              <template v-if="mallApp.productDetailType === 'normal'">
                <el-button plain @click="handleBuyNowFromDetail">立即购买</el-button>
                <el-button type="danger" @click="mallApp.addToCart(mallApp.productDetail, 'normal')">
                  加入购物车
                </el-button>
              </template>
              <template v-else>
                <el-button @click="mallApp.addToCart(mallApp.productDetail, 'seckill')">
                  加入草稿
                </el-button>
                <el-button
                  type="danger"
                  :disabled="!mallApp.canSeckill(mallApp.productDetail)"
                  :loading="mallApp.getProductCardState(mallApp.productDetail.id).pending"
                  @click="mallApp.handleSeckill(mallApp.productDetail)"
                >
                  发起秒杀
                </el-button>
              </template>
            </div>
          </div>
        </template>
      </el-skeleton>
    </el-drawer>

    <el-dialog
      v-model="mallApp.orderDialogVisible"
      width="680px"
      title="订单详情"
      destroy-on-close
      class="detail-panel"
    >
      <el-skeleton :rows="6" animated :loading="mallApp.orderDetailLoading">
        <template v-if="mallApp.selectedOrder">
          <div class="detail-stack">
            <div class="detail-grid detail-grid-wide">
              <div>
                <span>订单号</span>
                <strong>{{ mallApp.selectedOrder.orderNo || mallApp.selectedOrder.id }}</strong>
              </div>
              <div>
                <span>订单类型</span>
                <strong>{{ getOrderTypeText(mallApp.selectedOrder.orderType) }}</strong>
              </div>
              <div>
                <span>用户 ID</span>
                <strong>{{ mallApp.selectedOrder.userId }}</strong>
              </div>
              <div>
                <span>状态</span>
                <strong>{{ getOrderStatusText(mallApp.selectedOrder.status) }}</strong>
              </div>
              <div>
                <span>金额</span>
                <strong>{{ formatCurrency(selectedOrderAmount) }}</strong>
              </div>
              <div>
                <span>创建时间</span>
                <strong>{{ formatDateTime(mallApp.selectedOrder.createTime) }}</strong>
              </div>
            </div>

            <div v-if="mallApp.selectedOrder.orderType === 'normal'" class="detail-stack">
              <div class="detail-hero detail-hero--soft">
                <h3>普通订单信息</h3>
                <p>收货地址：{{ mallApp.getAddressSummary(mallApp.selectedOrder) }}</p>
                <p>备注：{{ mallApp.selectedOrder.remark || "无" }}</p>
                <p v-if="mallApp.getOrderStatusNote(mallApp.selectedOrder)">
                  状态说明：{{ mallApp.getOrderStatusNote(mallApp.selectedOrder) }}
                </p>
              </div>
              <div class="order-item-list">
                <article
                  v-for="item in mallApp.selectedOrder.items || []"
                  :key="item.id || item.productId"
                  class="order-item-card"
                >
                  <div>
                    <strong>{{ item.productName }}</strong>
                    <small>商品 ID {{ item.productId }}</small>
                  </div>
                  <div class="order-item-meta">
                    <span>x{{ item.quantity }}</span>
                    <strong>{{ formatCurrency(item.salePrice) }}</strong>
                  </div>
                </article>
              </div>
            </div>

            <div v-else class="detail-hero detail-hero--soft">
              <h3>秒杀订单信息</h3>
              <p>商品 ID：{{ mallApp.selectedOrder.productId }}</p>
              <p>秒杀价格：{{ formatCurrency(mallApp.selectedOrder.seckillPrice) }}</p>
              <p v-if="mallApp.getOrderStatusNote(mallApp.selectedOrder)">
                状态说明：{{ mallApp.getOrderStatusNote(mallApp.selectedOrder) }}
              </p>
            </div>

            <div class="dialog-actions">
              <el-tag :type="getOrderStatusType(mallApp.selectedOrder.status)">
                {{ getOrderStatusText(mallApp.selectedOrder.status) }}
              </el-tag>
              <div class="dialog-actions-right">
                <el-button @click="mallApp.fetchAndToastPayStatus(mallApp.selectedOrder)">
                  查询支付状态
                </el-button>
                <el-button
                  v-if="mallApp.isOrderPayable(mallApp.selectedOrder)"
                  @click="mallApp.cancelOrder(mallApp.selectedOrder)"
                >
                  取消订单
                </el-button>
                <el-button
                  v-if="mallApp.isOrderPayable(mallApp.selectedOrder)"
                  type="danger"
                  @click="handleOrderPay(mallApp.selectedOrder)"
                >
                  模拟支付
                </el-button>
              </div>
            </div>
          </div>
        </template>
      </el-skeleton>
    </el-dialog>

    <el-dialog
      v-model="mallApp.seckillPayConfirmVisible"
      width="760px"
      destroy-on-close
      :close-on-click-modal="false"
      :before-close="handleSeckillPayConfirmBeforeClose"
      class="pay-confirm-dialog"
      title="确认秒杀支付"
    >
      <template v-if="mallApp.pendingSeckillPayOrder">
        <div class="pay-confirm-body">
          <section class="pay-confirm-section">
            <div class="section-head section-head-wrap">
              <div>
                <p class="eyebrow">Pay Items</p>
                <h3>秒杀订单详情</h3>
              </div>
            </div>

            <article class="checkout-item-card">
              <div class="cart-thumb checkout-item-thumb">秒杀</div>

              <div class="checkout-item-main">
                <strong>秒杀商品 #{{ mallApp.pendingSeckillPayOrder.productId }}</strong>
                <small>
                  订单号
                  {{ mallApp.pendingSeckillPayOrder.orderNo || mallApp.pendingSeckillPayOrder.id }}
                </small>
                <el-tag type="danger" effect="plain">待确认支付</el-tag>
              </div>

              <div class="checkout-item-meta">
                <span>状态：{{ getOrderStatusText(mallApp.pendingSeckillPayOrder.status) }}</span>
                <strong>{{ formatCurrency(seckillPayOrderAmount) }}</strong>
              </div>
            </article>
          </section>

          <section class="pay-confirm-grid">
            <article class="preview-card">
              <div>
                <small>支付说明</small>
                <strong>确认后立即完成模拟支付</strong>
                <span>取消支付不会关闭订单，这笔秒杀订单会保留为待支付状态。</span>
              </div>
            </article>

            <article class="preview-card">
              <div>
                <small>状态说明</small>
                <strong>
                  {{
                    mallApp.getOrderStatusNote(mallApp.pendingSeckillPayOrder) ||
                    "当前订单待支付"
                  }}
                </strong>
                <span>你可以现在完成支付，也可以返回订单中心稍后继续处理。</span>
              </div>
            </article>

            <article class="preview-card">
              <div>
                <small>支付总额</small>
                <strong>{{ formatCurrency(seckillPayOrderAmount) }}</strong>
                <span>本次确认支付 1 件秒杀商品</span>
              </div>
            </article>
          </section>
        </div>
      </template>

      <template #footer>
        <div class="dialog-actions-right pay-confirm-actions">
          <el-button size="large" @click="mallApp.holdPendingSeckillPayOrder()">稍后支付</el-button>
          <el-button type="danger" size="large" @click="mallApp.confirmSeckillPay()">
            确认支付
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
