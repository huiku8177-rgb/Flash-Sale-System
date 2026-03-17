<script setup>
import { computed, inject, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { formatCurrency } from "../utils/format";

const mallApp = inject("mallApp");
const router = useRouter();
const visibleCount = ref(4);
const loadMoreAnchor = ref(null);
let observer = null;

const categoryEntries = [
  "新品推荐",
  "电脑数码",
  "生活电器",
  "休闲零食",
  "办公外设",
  "家居百货",
  "箱包配件",
  "健康个护",
  "潮流配饰",
  "更多频道"
];

const bannerProducts = computed(() => {
  const seckillItems = [...mallApp.seckillProducts].filter(
    (product) => product.status === 1 && Number(product.stock || 0) > 0
  );

  if (!seckillItems.length) {
    return [
      {
        id: "price-hero",
        title: "高价主推秒杀",
        subtitle: "主推位",
        copy: "轮播首位默认展示秒杀价最高的商品，首页点击后直接进入闪购区。",
        accent: "High Price",
        metricLabel: "秒杀价",
        metricValue: "--"
      },
      {
        id: "stock-hero",
        title: "高库存稳定供给",
        subtitle: "稳定供给",
        copy: "第二张轮播默认展示库存最高的秒杀商品，优先承接可立即购买的流量。",
        accent: "High Stock",
        metricLabel: "库存",
        metricValue: "--"
      }
    ];
  }

  const priceHero = [...seckillItems].sort(
    (left, right) => Number(right.seckillPrice ?? 0) - Number(left.seckillPrice ?? 0)
  )[0];

  const stockHero =
    [...seckillItems]
      .sort((left, right) => Number(right.stock ?? 0) - Number(left.stock ?? 0))
      .find((product) => product.id !== priceHero.id) ?? priceHero;

  return [
    {
      id: priceHero.id,
      title: priceHero.name,
      subtitle: "主推位",
      copy: "主推位固定选择秒杀价最高的商品，优先把用户导向更强冲击力的闪购单品。",
      accent: "High Price",
      metricLabel: "秒杀价",
      metricValue: formatCurrency(priceHero.seckillPrice)
    },
    {
      id: stockHero.id,
      title: stockHero.name,
      subtitle: "稳定供给",
      copy: "稳定供给位固定选择库存最高的商品，降低用户点进后秒空的概率。",
      accent: "High Stock",
      metricLabel: "库存",
      metricValue: `${stockHero.stock}`
    }
  ];
});

const visibleProducts = computed(() => {
  const products = mallApp.featuredNormalProducts || [];
  return products.slice(0, visibleCount.value);
});

const hasMoreProducts = computed(() => {
  const total = (mallApp.featuredNormalProducts || []).length;
  return visibleCount.value < total;
});

function goFlash() {
  router.push({ name: "app-flash" });
}

function loadMoreProducts() {
  const total = (mallApp.featuredNormalProducts || []).length;
  if (visibleCount.value >= total) {
    return;
  }
  visibleCount.value = Math.min(visibleCount.value + 4, total);
}

function setupObserver() {
  if (!loadMoreAnchor.value || observer) {
    return;
  }

  observer = new IntersectionObserver(
    (entries) => {
      const [entry] = entries;
      if (entry?.isIntersecting) {
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
    visibleCount.value = 4;
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
          <strong>商品服务分类</strong>
          <span>桌面站导航</span>
        </div>
        <button
          v-for="entry in categoryEntries"
          :key="entry"
          type="button"
          class="category-link"
          @click="mallApp.productFilters.name = entry; mallApp.loadProducts()"
        >
          {{ entry }}
        </button>
      </aside>

      <div class="hero-center">
        <el-carousel height="296px" indicator-position="outside">
          <el-carousel-item v-for="banner in bannerProducts" :key="banner.id">
            <article class="desktop-banner desktop-banner-clickable" @click="goFlash">
              <div>
                <p class="eyebrow">{{ banner.subtitle }}</p>
                <h2>{{ banner.title }}</h2>
                <p>{{ banner.copy }}</p>
                <div class="desktop-banner-actions">
                  <el-button type="danger" @click.stop="goFlash">进入闪购</el-button>
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

        <section class="section-card home-feature-section">
          <div class="section-head">
            <div>
              <p class="eyebrow">Featured Goods</p>
              <h3>普通商品精选</h3>
            </div>
            <el-button text @click="mallApp.loadProducts">刷新普通商品</el-button>
          </div>

          <div class="home-feature-grid">
            <article
              v-for="product in visibleProducts"
              :key="product.id"
              class="desktop-product-card home-feature-card"
            >
              <div class="desktop-product-thumb">{{ product.name.slice(0, 2) }}</div>
              <div class="home-feature-copy">
                <span class="eyebrow">库存 {{ product.stock }}</span>
                <h4>{{ product.name }}</h4>
                <p>{{ product.subtitle || "首页普通商品默认展示卡片" }}</p>
              </div>
              <div class="desktop-price-row">
                <strong>{{ formatCurrency(product.price) }}</strong>
                <span>{{ formatCurrency(product.marketPrice) }}</span>
              </div>
              <div class="desktop-card-actions">
                <el-button text @click="mallApp.openProduct(product, 'normal')">查看详情</el-button>
                <el-button round @click="mallApp.addToCart(product, 'normal')">加入购物车</el-button>
              </div>
            </article>
          </div>

          <div
            v-if="hasMoreProducts"
            ref="loadMoreAnchor"
            class="home-feature-footer"
          >
            <span>下滑自动加载更多商品</span>
          </div>
        </section>
      </div>
    </section>
  </div>
</template>
