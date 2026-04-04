<script setup>
import { computed, inject, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { formatCurrency } from "../utils/format";

const mallApp = inject("mallApp");
const router = useRouter();
const visibleCount = ref(6);
const loadMoreAnchor = ref(null);
let observer = null;

const categoryEntries = computed(() =>
  mallApp.productCategories.length
    ? mallApp.productCategories
    : [{ id: null, label: "全部商品", count: mallApp.productTotalCount }]
);

const bannerProducts = computed(() => {
  const seckillItems = [...mallApp.seckillProducts].filter(
    (product) => product.status === 1 && Number(product.stock || 0) > 0
  );

  if (!seckillItems.length) {
    return [
      {
        id: "flash-banner",
        title: "秒杀会场已就绪",
        subtitle: "Flash Sale",
        copy: "秒杀商品、异步建单、支付状态查询都已经接入，点击后可直接进入秒杀频道。",
        accent: "会场入口",
        metricLabel: "当前商品",
        metricValue: "0"
      }
    ];
  }

  const hottest = [...seckillItems].sort(
    (left, right) => Number(right.seckillPrice ?? 0) - Number(left.seckillPrice ?? 0)
  )[0];
  const richest =
    [...seckillItems]
      .sort((left, right) => Number(right.stock ?? 0) - Number(left.stock ?? 0))
      .find((product) => product.id !== hottest.id) ?? hottest;

  return [
    {
      id: hottest.id,
      title: hottest.name,
      subtitle: "高客单秒杀",
      copy: "优先展示秒杀价更高的商品，让首页可以直接把用户引导到冲击力更强的活动位。",
      accent: "高热商品",
      metricLabel: "秒杀价",
      metricValue: formatCurrency(hottest.seckillPrice)
    },
    {
      id: richest.id,
      title: richest.name,
      subtitle: "稳态承接",
      copy: "库存更充足的秒杀商品适合作为活动入口，减少用户刚进入会场就售罄的落差感。",
      accent: "库存充足",
      metricLabel: "剩余库存",
      metricValue: `${richest.stock}`
    }
  ];
});

const visibleProducts = computed(() =>
  (mallApp.featuredNormalProducts || []).slice(0, visibleCount.value)
);

const hasMoreProducts = computed(
  () => visibleCount.value < (mallApp.featuredNormalProducts || []).length
);

function goFlash() {
  router.push({ name: "app-flash" });
}

function goAssistant() {
  router.push({ name: "app-assistant" });
}

async function buyNow(product) {
  const ready = await mallApp.prepareImmediateCheckout(product);
  if (ready) {
    router.push({ name: "checkout" });
  }
}

function loadMoreProducts() {
  const total = (mallApp.featuredNormalProducts || []).length;
  if (visibleCount.value >= total) {
    return;
  }
  visibleCount.value = Math.min(visibleCount.value + 6, total);
}

function applyCategory(categoryId) {
  if (categoryId === null) {
    mallApp.clearProductFilters();
    return;
  }
  mallApp.applyCategoryFilter(categoryId);
}

function setupObserver() {
  if (!loadMoreAnchor.value || observer) {
    return;
  }

  observer = new IntersectionObserver(
    (entries) => {
      if (entries[0]?.isIntersecting) {
        loadMoreProducts();
      }
    },
    {
      root: null,
      rootMargin: "0px 0px 180px 0px",
      threshold: 0
    }
  );

  observer.observe(loadMoreAnchor.value);
}

watch(
  () => (mallApp.featuredNormalProducts || []).length,
  () => {
    visibleCount.value = 6;
    observer?.disconnect();
    observer = null;
    setTimeout(() => setupObserver(), 0);
  }
);

onMounted(() => {
  setupObserver();
});

onBeforeUnmount(() => {
  observer?.disconnect();
  observer = null;
});
</script>

<template>
  <div class="page-stack">
    <section class="home-hero-layout">
      <aside class="category-board">
        <div class="category-board-head">
          <strong>商品分类</strong>
          <span>按普通商品分类快速筛选</span>
        </div>
        <button
          type="button"
          class="category-link"
          :class="{ active: mallApp.productFilters.categoryId === null }"
          @click="mallApp.clearProductFilters"
        >
          <span>全部商品</span>
          <small>{{ mallApp.productTotalCount }}</small>
        </button>
        <button
          v-for="entry in categoryEntries"
          :key="entry.id ?? 'all'"
          type="button"
          class="category-link"
          :class="{ active: mallApp.productFilters.categoryId === entry.id }"
          @click="applyCategory(entry.id)"
        >
          <span>{{ entry.label }}</span>
          <small>{{ entry.count }}</small>
        </button>
      </aside>

      <div class="hero-center">
        <el-carousel height="296px" indicator-position="outside" class="home-carousel">
          <el-carousel-item v-for="banner in bannerProducts" :key="banner.id">
            <article class="desktop-banner desktop-banner-clickable" @click="goFlash">
              <div class="desktop-banner-copy">
                <p class="eyebrow">{{ banner.subtitle }}</p>
                <h2>{{ banner.title }}</h2>
                <p>{{ banner.copy }}</p>
                <div class="desktop-banner-actions">
                  <el-button type="danger" @click.stop="goFlash">进入秒杀会场</el-button>
                  <el-button @click.stop="mallApp.loadSeckillProducts">刷新秒杀商品</el-button>
                </div>
              </div>
              <div class="desktop-banner-badge">
                <span>{{ banner.accent }}</span>
                <strong>{{ banner.metricValue }}</strong>
                <small>{{ banner.metricLabel }}</small>
              </div>
            </article>
          </el-carousel-item>
        </el-carousel>

        <section class="desktop-shortcuts">
          <article class="shortcut-card shortcut-orange">
            <span>普通商品</span>
            <strong>{{ mallApp.products.length }}</strong>
            <small>
              支持列表、详情、加入购物车、下单和模拟支付，基本覆盖常规商城主链路。
            </small>
          </article>
          <article class="shortcut-card shortcut-blue">
            <span>秒杀商品</span>
            <strong>{{ mallApp.seckillProducts.length }}</strong>
            <small>
              支持发起秒杀、轮询结果、待支付确认和订单继续支付，适合展示抢购场景。
            </small>
          </article>
          <article class="shortcut-card shortcut-green">
            <span>待支付订单</span>
            <strong>{{ mallApp.orderStats.created }}</strong>
            <small>
              普通订单和秒杀订单都支持先创建待支付订单，再在订单中心继续完成支付。
            </small>
          </article>
          <article class="shortcut-card shortcut-ink">
            <span>AI 商品助手</span>
            <strong>可靠问答</strong>
            <small>
              在独立工作区内咨询商品、秒杀、配送和售后问题，回答会优先基于检索证据和实时信息。
            </small>
            <el-button plain @click="goAssistant">进入 AI 助手</el-button>
          </article>
        </section>
      </div>
    </section>

    <section class="section-card home-shelf-section">
      <div class="section-head">
        <div>
          <p class="eyebrow">Featured Goods</p>
          <h3>普通商品精选</h3>
        </div>
        <div class="section-actions">
          <el-button text @click="mallApp.clearProductFilters">清空筛选</el-button>
          <el-button text @click="mallApp.loadProducts">刷新商品</el-button>
        </div>
      </div>

      <div class="desktop-product-grid">
        <article
          v-for="product in visibleProducts"
          :key="product.id"
          class="desktop-product-card"
        >
          <div class="desktop-product-thumb">{{ product.name.slice(0, 2) }}</div>
          <div class="product-card-topline">
            <el-tag size="small" effect="plain">{{ product.categoryName || "普通商品" }}</el-tag>
            <span>库存 {{ product.stock }}</span>
          </div>
          <h4>{{ product.name }}</h4>
          <p>
            {{
              product.subtitle ||
              "适合作为首页推荐位展示的普通商品，支持立即购买和加入购物车。"
            }}
          </p>
          <div class="desktop-price-row">
            <strong>{{ formatCurrency(product.price) }}</strong>
            <span>{{ formatCurrency(product.marketPrice) }}</span>
          </div>
          <div class="desktop-card-actions">
            <el-button type="danger" plain @click="buyNow(product)">立即购买</el-button>
            <el-button text @click="mallApp.openProduct(product, 'normal')">查看详情</el-button>
            <el-button round @click="mallApp.addToCart(product, 'normal')">加入购物车</el-button>
          </div>
        </article>
      </div>

      <div v-if="hasMoreProducts" ref="loadMoreAnchor" class="home-feature-footer">
        <span>继续下滑可自动加载更多普通商品</span>
      </div>
    </section>

    <section class="home-dual-grid">
      <section class="section-card">
        <div class="section-head">
          <div>
            <p class="eyebrow">Quick Checkout</p>
            <h3>购物车概览</h3>
          </div>
          <el-button text @click="router.push({ name: 'app-cart' })">去购物车</el-button>
        </div>
        <div class="home-metric-grid">
          <div class="home-metric-card">
            <span>购物车商品数</span>
            <strong>{{ mallApp.cartSummary.count }}</strong>
          </div>
          <div class="home-metric-card">
            <span>普通商品待结算</span>
            <strong>{{ formatCurrency(mallApp.checkoutSummary.total) }}</strong>
          </div>
          <div class="home-metric-card">
            <span>秒杀草稿数</span>
            <strong>{{ mallApp.seckillCartItems.length }}</strong>
          </div>
        </div>
      </section>

      <section class="section-card">
        <div class="section-head">
          <div>
            <p class="eyebrow">Flash Preview</p>
            <h3>秒杀抢购预览</h3>
          </div>
          <el-button text @click="goFlash">进入会场</el-button>
        </div>
        <div class="preview-stack">
          <article
            v-for="product in mallApp.flashProducts.slice(0, 3)"
            :key="product.id"
            class="preview-card"
          >
            <div>
              <strong>{{ product.name }}</strong>
              <small>秒杀价 {{ formatCurrency(product.seckillPrice) }}</small>
            </div>
            <el-button type="danger" plain @click="mallApp.openProduct(product, 'seckill')">
              详情
            </el-button>
          </article>
          <el-empty
            v-if="!mallApp.flashProducts.length && !mallApp.seckillProductsLoading"
            description="暂无秒杀商品"
            :image-size="80"
          />
        </div>
      </section>
    </section>
  </div>
</template>
