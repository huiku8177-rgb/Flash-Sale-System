<script setup>
import { computed, inject, ref } from "vue";
import {
  formatCurrency,
  formatDateTime,
  getCountdownText,
  getProductPhase,
  getProductPhaseLabel,
  getProductPhaseType
} from "../utils/format";

const mallApp = inject("mallApp");
const activeFilter = ref("all");

const flashTabs = [
  { label: "全部商品", value: "all" },
  { label: "抢购中", value: "running" },
  { label: "即将开始", value: "upcoming" },
  { label: "已结束", value: "ended" }
];

const visibleProducts = computed(() => {
  if (activeFilter.value === "all") {
    return mallApp.flashProducts;
  }
  return mallApp.flashProducts.filter((product) => {
    return getProductPhase(product, mallApp.currentTime) === activeFilter.value;
  });
});
</script>

<template>
  <div class="page-stack">
    <section class="flash-desktop-banner">
      <div>
        <p class="eyebrow">Flash Event</p>
        <h2>秒杀会场</h2>
        <p>这里直接对接秒杀商品列表、发起秒杀、轮询结果、订单详情和模拟支付整套链路。</p>
      </div>
      <div class="flash-tab-row">
        <button
          v-for="tab in flashTabs"
          :key="tab.value"
          type="button"
          class="flash-tab"
          :class="{ active: activeFilter === tab.value }"
          @click="activeFilter = tab.value"
        >
          {{ tab.label }}
        </button>
      </div>
    </section>

    <section class="section-card">
      <div class="section-head">
        <div>
          <p class="eyebrow">Asynchronous Flow</p>
          <h3>秒杀商品列表</h3>
        </div>
        <el-button text @click="mallApp.loadSeckillProducts">刷新秒杀商品</el-button>
      </div>

      <div class="flash-table-head">
        <span>商品信息</span>
        <span>活动时间</span>
        <span>价格与库存</span>
        <span>操作</span>
      </div>

      <article
        v-for="product in visibleProducts"
        :key="product.id"
        class="flash-table-row"
      >
        <div class="flash-col flash-col-product">
          <div class="flash-thumb">{{ product.name.slice(0, 2) }}</div>
          <div>
            <div class="flash-card-topline">
              <h4>{{ product.name }}</h4>
              <el-tag :type="getProductPhaseType(product, mallApp.currentTime)" effect="dark">
                {{ getProductPhaseLabel(product, mallApp.currentTime) }}
              </el-tag>
            </div>
            <p>{{ mallApp.getProductCardState(product.id).message || "当前商品可直接进入秒杀流程。" }}</p>
          </div>
        </div>

        <div class="flash-col">
          <strong>{{ getCountdownText(product.startTime, product.endTime, mallApp.currentTime) }}</strong>
          <small>开始：{{ formatDateTime(product.startTime) }}</small>
          <small>结束：{{ formatDateTime(product.endTime) }}</small>
        </div>

        <div class="flash-col">
          <strong>{{ formatCurrency(product.seckillPrice) }}</strong>
          <small class="line-text">原价 {{ formatCurrency(product.price) }}</small>
          <small>库存 {{ product.stock }}</small>
        </div>

        <div class="flash-col flash-col-actions">
          <el-button plain @click="mallApp.openProduct(product, 'seckill')">详情</el-button>
          <el-button @click="mallApp.addToCart(product, 'seckill')">加入草稿</el-button>
          <el-button
            type="danger"
            :disabled="!mallApp.canSeckill(product)"
            :loading="mallApp.getProductCardState(product.id).pending"
            @click="mallApp.handleSeckill(product)"
          >
            {{
              mallApp.getProductCardState(product.id).pending
                ? "轮询中"
                : mallApp.canSeckill(product)
                  ? "立即秒杀"
                  : "暂不可抢"
            }}
          </el-button>
        </div>
      </article>

      <el-empty
        v-if="!visibleProducts.length && !mallApp.seckillProductsLoading"
        description="当前筛选条件下暂无秒杀商品"
      />
    </section>
  </div>
</template>
