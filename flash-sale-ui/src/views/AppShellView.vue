<script setup>
import { computed, onBeforeUnmount, onMounted, provide, reactive } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { logout } from "../api/auth";
import { useMallApp } from "../composables/useMallApp";
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
const mallApp = reactive(useMallApp());

provide("mallApp", mallApp);

const navItems = [
  { label: "首页", routeName: "app-home" },
  { label: "秒杀", routeName: "app-flash" },
  { label: "购物车", routeName: "app-cart" },
  { label: "个人中心", routeName: "app-profile" }
];

const hotKeywords = ["耳机", "键盘", "零食", "显示器", "饮料"];

const currentTitle = computed(() => {
  const mapping = {
    "app-home": "首页推荐",
    "app-flash": "秒杀会场",
    "app-cart": "购物车",
    "app-profile": "个人中心"
  };
  return mapping[route.name] || "Flash Sale Mall";
});

const productApiPath = computed(() => {
  return mallApp.productDetailType === "seckill"
    ? "/seckill-product/products/{id}"
    : "/product/products/{id}";
});

const selectedOrderAmount = computed(() => {
  return mallApp.selectedOrder ? mallApp.getOrderDisplayAmount(mallApp.selectedOrder) : 0;
});

const searchKeyword = computed({
  get() {
    return mallApp.productFilters.name;
  },
  set(value) {
    mallApp.productFilters.name = value;
  }
});

onMounted(async () => {
  await mallApp.init();
});

onBeforeUnmount(() => {
  mallApp.dispose();
});

async function handleLogout() {
  try {
    await logout();
  } catch {
    // noop
  } finally {
    clearSession();
    router.push({ name: "login" });
  }
}

async function handleSearch() {
  await mallApp.loadProducts();
  router.push({ name: "app-home" });
  ElMessage.success("普通商品列表已按关键词刷新");
}

function jumpNav(routeName) {
  router.push({ name: routeName });
}

function fillKeyword(keyword) {
  searchKeyword.value = keyword;
  handleSearch();
}
</script>

<template>
  <div class="web-shell">
    <div class="top-utility-bar">
      <div class="utility-inner">
        <div class="utility-links">
          <span>欢迎回来，{{ mallApp.profileDisplayName }}</span>
          <button type="button" @click="jumpNav('app-profile')">我的中心</button>
          <button type="button" @click="jumpNav('app-cart')">购物车</button>
          <button type="button" @click="jumpNav('app-flash')">秒杀频道</button>
        </div>
        <div class="utility-links">
          <span>当前页面：{{ currentTitle }}</span>
          <button type="button" @click="handleLogout">退出登录</button>
        </div>
      </div>
    </div>

    <header class="site-header">
      <div class="site-header-inner">
        <button class="brand-block" type="button" @click="jumpNav('app-home')">
          <span class="brand-mark">FS</span>
          <div>
            <strong>Flash Sale Mall</strong>
            <small>普通商品与秒杀商品一体化前端</small>
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
            <span>热门搜索：</span>
            <button
              v-for="keyword in hotKeywords"
              :key="keyword"
              type="button"
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
          <el-button plain @click="mallApp.refreshMallData">刷新全部</el-button>
          <el-button plain @click="mallApp.loadOrders">刷新订单</el-button>
        </div>
      </div>
    </header>

    <nav class="channel-bar">
      <div class="channel-inner">
        <div class="channel-title">全部频道</div>
        <button
          v-for="item in navItems"
          :key="item.routeName"
          type="button"
          class="channel-link"
          :class="{ active: route.name === item.routeName }"
          @click="jumpNav(item.routeName)"
        >
          {{ item.label }}
        </button>
      </div>
    </nav>

    <div class="portal-frame">
      <main class="portal-main">
        <router-view />
      </main>

      <aside class="portal-sidebar">
        <section class="sidebar-card">
          <p class="eyebrow">Account Snapshot</p>
          <h3>{{ mallApp.profileDisplayName }}</h3>
          <div class="sidebar-metrics">
            <div>
              <span>用户 ID</span>
              <strong>{{ mallApp.authState.userId }}</strong>
            </div>
            <div>
              <span>普通订单</span>
              <strong>{{ mallApp.normalOrders.length }}</strong>
            </div>
            <div>
              <span>秒杀订单</span>
              <strong>{{ mallApp.seckillOrders.length }}</strong>
            </div>
            <div>
              <span>购物车金额</span>
              <strong>{{ formatCurrency(mallApp.cartSummary.total) }}</strong>
            </div>
          </div>
        </section>

        <section class="sidebar-card">
          <div class="sidebar-head">
            <div>
              <p class="eyebrow">Latest Orders</p>
              <h3>最近订单</h3>
            </div>
            <el-button text @click="jumpNav('app-profile')">更多</el-button>
          </div>
          <div class="sidebar-order-list">
            <article
              v-for="order in mallApp.recentOrders"
              :key="`${order.orderType}-${order.id}`"
              class="sidebar-order-item"
            >
              <div>
                <strong>{{ order.orderNo || `#${order.id}` }}</strong>
                <small>{{ formatDateTime(order.createTime) }}</small>
              </div>
              <div class="sidebar-order-meta">
                <span>{{ getOrderTypeText(order.orderType) }}</span>
                <button type="button" @click="mallApp.openOrder(order)">详情</button>
              </div>
            </article>
            <el-empty
              v-if="!mallApp.recentOrders.length && !mallApp.ordersLoading"
              description="暂无订单"
              :image-size="80"
            />
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
            <article
              v-for="item in mallApp.cartItems.slice(0, 3)"
              :key="item.cartKey"
              class="sidebar-cart-item"
            >
              <strong>{{ item.name }}</strong>
              <small>x{{ item.quantity }} · {{ formatCurrency(mallApp.getCartItemPrice(item)) }}</small>
            </article>
            <el-empty
              v-if="!mallApp.cartItems.length"
              description="购物车为空"
              :image-size="80"
            />
          </div>
        </section>
      </aside>
    </div>

    <el-drawer
      v-model="mallApp.productDetailVisible"
      size="440px"
      title="商品详情"
      destroy-on-close
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
              <p>详情数据来自 <code>{{ productApiPath }}</code>，前端会按商品类型自动切换接口。</p>
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

            <div class="detail-hero">
              <h3>{{ mallApp.productDetailType === "seckill" ? "当前活动状态" : "商品补充信息" }}</h3>
              <p v-if="mallApp.productDetailType === 'seckill'">
                {{ getCountdownText(
                  mallApp.productDetail.startTime,
                  mallApp.productDetail.endTime,
                  mallApp.currentTime
                ) }}
              </p>
              <p v-else>主图：{{ mallApp.productDetail.mainImage || "当前未配置" }}</p>
              <p v-if="mallApp.productDetailType === 'normal'">
                {{ mallApp.productDetail.detail || "当前商品详情文案尚未补充。" }}
              </p>
            </div>

            <div class="dialog-actions">
              <el-button
                v-if="mallApp.productDetailType === 'normal'"
                type="danger"
                @click="mallApp.addToCart(mallApp.productDetail, 'normal')"
              >
                加入购物车
              </el-button>
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
              <div class="detail-hero">
                <h3>普通订单信息</h3>
                <p>收货地址：{{ mallApp.getAddressSummary(mallApp.selectedOrder) }}</p>
                <p>备注：{{ mallApp.selectedOrder.remark || "无" }}</p>
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

            <div v-else class="detail-hero">
              <h3>秒杀订单信息</h3>
              <p>商品 ID：{{ mallApp.selectedOrder.productId }}</p>
              <p>秒杀价格：{{ formatCurrency(mallApp.selectedOrder.seckillPrice) }}</p>
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
                  type="danger"
                  @click="mallApp.payOrder(mallApp.selectedOrder)"
                >
                  模拟支付
                </el-button>
              </div>
            </div>
          </div>
        </template>
      </el-skeleton>
    </el-dialog>
  </div>
</template>
