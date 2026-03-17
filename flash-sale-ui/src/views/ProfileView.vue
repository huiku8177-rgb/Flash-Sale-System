<script setup>
import { inject } from "vue";
import { formatCurrency, formatDateTime, getOrderStatusText } from "../utils/format";

const mallApp = inject("mallApp");
</script>

<template>
  <div class="page-stack">
    <section class="profile-page-header section-card">
      <div>
        <p class="eyebrow">User Center</p>
        <h2>我的订单与账户概览</h2>
        <p>当前个人中心以登录态和订单列表为主，后续可以继续接用户资料、地址簿和支付状态。</p>
      </div>
      <div class="profile-page-badge">
        <span>{{ mallApp.authState.username }}</span>
        <strong>ID {{ mallApp.authState.userId }}</strong>
      </div>
    </section>

    <section class="profile-dashboard-grid">
      <article class="profile-stat-card">
        <span>订单总数</span>
        <strong>{{ mallApp.orderStats.total }}</strong>
      </article>
      <article class="profile-stat-card">
        <span>待支付</span>
        <strong>{{ mallApp.orderStats.created }}</strong>
      </article>
      <article class="profile-stat-card">
        <span>已支付</span>
        <strong>{{ mallApp.orderStats.paid }}</strong>
      </article>
      <article class="profile-stat-card">
        <span>购物车件数</span>
        <strong>{{ mallApp.cartSummary.count }}</strong>
      </article>
    </section>

    <div class="profile-desktop-layout">
      <section class="section-card">
        <div class="section-head">
          <div>
            <p class="eyebrow">Order List</p>
            <h3>我的订单</h3>
          </div>
          <el-button plain @click="mallApp.loadOrders">刷新订单</el-button>
        </div>
        <div class="profile-order-table">
          <div class="profile-order-head">
            <span>订单号</span>
            <span>商品</span>
            <span>时间</span>
            <span>状态</span>
            <span>金额</span>
            <span>操作</span>
          </div>
          <article
            v-for="order in mallApp.orders"
            :key="order.id"
            class="profile-order-row"
          >
            <span>#{{ order.id }}</span>
            <span>商品 {{ order.productId }}</span>
            <span>{{ formatDateTime(order.createTime) }}</span>
            <span>{{ getOrderStatusText(order.status) }}</span>
            <strong>{{ formatCurrency(order.seckillPrice) }}</strong>
            <el-button text @click="mallApp.openOrder(order.id)">查看</el-button>
          </article>
          <el-empty
            v-if="!mallApp.orders.length && !mallApp.ordersLoading"
            description="当前还没有订单"
          />
        </div>
      </section>

      <aside class="profile-side-panel">
        <section class="sidebar-card">
          <p class="eyebrow">Current Capabilities</p>
          <h3>已接通数据</h3>
          <ul class="side-bullet-list">
            <li>登录态用户名与用户 ID</li>
            <li>我的订单列表与订单详情</li>
            <li>购物车草案数量汇总</li>
          </ul>
        </section>

        <section class="sidebar-card">
          <p class="eyebrow">Need More APIs</p>
          <h3>建议继续补后端</h3>
          <ul class="side-bullet-list">
            <li>`GET /auth/me` 获取完整个人资料</li>
            <li>`GET /user/addresses` 地址簿</li>
            <li>`GET /order/orders?pageNum=...` 订单分页</li>
            <li>`GET /order/pay-status/{orderId}` 支付状态</li>
          </ul>
        </section>
      </aside>
    </div>
  </div>
</template>
