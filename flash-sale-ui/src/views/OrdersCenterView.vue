<script setup>
import { computed, inject, ref } from "vue";
import { useRouter } from "vue-router";
import {
  formatCurrency,
  formatDateTime,
  getOrderStatusText,
  getOrderStatusType,
  getOrderTypeText
} from "../utils/format";

const mallApp = inject("mallApp");
const router = useRouter();
const orderFilter = ref("all");

const filteredOrders = computed(() => {
  if (orderFilter.value === "all") {
    return mallApp.orders;
  }
  if (orderFilter.value === "pending") {
    return mallApp.orders.filter((order) => order.status === 0);
  }
  return mallApp.orders.filter((order) => order.orderType === orderFilter.value);
});

const selectedOrderAmount = computed(() => {
  return mallApp.selectedOrder ? mallApp.getOrderDisplayAmount(mallApp.selectedOrder) : 0;
});

function backToProfile() {
  router.push({ name: "app-profile" });
}

function handlePay(order) {
  if (order.orderType === "seckill") {
    mallApp.openSeckillPayConfirm(order);
    return;
  }
  mallApp.payOrder(order);
}
</script>

<template>
  <div class="page-stack">
    <section class="profile-hub-hero section-card">
      <div class="profile-hub-copy">
        <p class="eyebrow">Order Center</p>
        <h2>我的订单</h2>
        <p>这里集中处理订单筛选、详情查看、支付、取消和状态查询，整体保留商城主站的导航与浏览节奏。</p>
      </div>
      <div class="profile-page-badge">
        <span>订单总数</span>
        <strong>{{ mallApp.orderStats.total }}</strong>
      </div>
    </section>

    <section class="section-card">
      <div class="section-head section-head-wrap">
        <div>
          <p class="eyebrow">Order List</p>
          <h3>订单中心</h3>
        </div>
        <div class="section-actions">
          <el-radio-group v-model="orderFilter" size="small">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="normal">普通</el-radio-button>
            <el-radio-button label="seckill">秒杀</el-radio-button>
            <el-radio-button label="pending">待支付</el-radio-button>
          </el-radio-group>
          <el-button plain @click="mallApp.loadOrders">刷新订单</el-button>
          <el-button text @click="backToProfile">返回个人中心</el-button>
        </div>
      </div>

      <div class="profile-order-table">
        <div class="profile-order-head profile-order-head-wide">
          <span>订单信息</span>
          <span>类型</span>
          <span>创建时间</span>
          <span>状态</span>
          <span>金额</span>
          <span>操作</span>
        </div>

        <article
          v-for="order in filteredOrders"
          :key="`${order.orderType}-${order.id}`"
          class="profile-order-row profile-order-row-wide"
        >
          <div class="order-primary">
            <strong>{{ order.orderNo || `#${order.id}` }}</strong>
            <small>{{ mallApp.getOrderSummary(order) }}</small>
          </div>

          <div>
            <el-tag :type="order.orderType === 'seckill' ? 'danger' : 'success'" effect="plain">
              {{ getOrderTypeText(order.orderType) }}
            </el-tag>
          </div>

          <span>{{ formatDateTime(order.createTime) }}</span>

          <div class="order-status-block">
            <el-tag :type="getOrderStatusType(order.status)">
              {{ getOrderStatusText(order.status) }}
            </el-tag>
            <small v-if="mallApp.getOrderStatusNote(order)">
              {{ mallApp.getOrderStatusNote(order) }}
            </small>
          </div>

          <strong>{{ formatCurrency(mallApp.getOrderDisplayAmount(order)) }}</strong>

          <div class="order-actions">
            <el-button text @click="mallApp.openOrder(order)">详情</el-button>
            <el-button
              v-if="mallApp.isOrderPayable(order)"
              text
              type="danger"
              @click="handlePay(order)"
            >
              去支付
            </el-button>
            <el-button
              v-if="mallApp.isOrderPayable(order)"
              text
              @click="mallApp.cancelOrder(order)"
            >
              取消订单
            </el-button>
            <el-button text @click="mallApp.fetchAndToastPayStatus(order)">查状态</el-button>
          </div>
        </article>

        <el-empty
          v-if="!filteredOrders.length && !mallApp.ordersLoading"
          description="当前筛选条件下还没有订单"
        />
      </div>
    </section>

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

            <div v-else class="detail-hero">
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
                  @click="handlePay(mallApp.selectedOrder)"
                >
                  去支付
                </el-button>
              </div>
            </div>
          </div>
        </template>
      </el-skeleton>
    </el-dialog>
  </div>
</template>
