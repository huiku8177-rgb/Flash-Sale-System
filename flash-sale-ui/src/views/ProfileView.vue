<script setup>
import { computed, inject, reactive, ref } from "vue";
import {
  formatCurrency,
  formatDateTime,
  getOrderStatusText,
  getOrderStatusType,
  getOrderTypeText
} from "../utils/format";

const mallApp = inject("mallApp");
const orderFilter = ref("all");
const passwordForm = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: ""
});
const passwordSubmitting = ref(false);

const filteredOrders = computed(() => {
  if (orderFilter.value === "all") {
    return mallApp.orders;
  }
  if (orderFilter.value === "pending") {
    return mallApp.orders.filter((order) => order.status === 0);
  }
  return mallApp.orders.filter((order) => order.orderType === orderFilter.value);
});

async function handlePasswordUpdate() {
  passwordSubmitting.value = true;
  const success = await mallApp.submitPasswordUpdate(passwordForm);
  passwordSubmitting.value = false;

  if (success) {
    passwordForm.oldPassword = "";
    passwordForm.newPassword = "";
    passwordForm.confirmPassword = "";
  }
}
</script>

<template>
  <div class="page-stack">
    <section class="profile-page-header section-card">
      <div>
        <p class="eyebrow">User Center</p>
        <h2>我的订单与账号信息</h2>
        <p>这里汇总普通订单、秒杀订单、当前登录用户信息和密码修改能力。</p>
      </div>
      <div class="profile-page-badge">
        <span>{{ mallApp.profileDisplayName }}</span>
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
        <span>普通订单</span>
        <strong>{{ mallApp.orderStats.normal }}</strong>
      </article>
      <article class="profile-stat-card">
        <span>秒杀订单</span>
        <strong>{{ mallApp.orderStats.seckill }}</strong>
      </article>
    </section>

    <div class="profile-desktop-layout">
      <section class="section-card">
        <div class="section-head section-head-wrap">
          <div>
            <p class="eyebrow">Order List</p>
            <h3>全部订单</h3>
          </div>
          <div class="section-actions">
            <el-radio-group v-model="orderFilter" size="small">
              <el-radio-button label="all">全部</el-radio-button>
              <el-radio-button label="normal">普通</el-radio-button>
              <el-radio-button label="seckill">秒杀</el-radio-button>
              <el-radio-button label="pending">待支付</el-radio-button>
            </el-radio-group>
            <el-button plain @click="mallApp.loadOrders">刷新订单</el-button>
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

            <div>
              <el-tag :type="getOrderStatusType(order.status)">
                {{ getOrderStatusText(order.status) }}
              </el-tag>
            </div>

            <strong>{{ formatCurrency(mallApp.getOrderDisplayAmount(order)) }}</strong>

            <div class="order-actions">
              <el-button text @click="mallApp.openOrder(order)">详情</el-button>
              <el-button
                v-if="mallApp.isOrderPayable(order)"
                text
                type="danger"
                @click="mallApp.payOrder(order)"
              >
                去支付
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

      <aside class="profile-side-panel">
        <section class="sidebar-card">
          <p class="eyebrow">Profile</p>
          <h3>登录信息</h3>
          <div class="sidebar-metrics">
            <div>
              <span>用户名</span>
              <strong>{{ mallApp.profile?.username || mallApp.authState.username }}</strong>
            </div>
            <div>
              <span>用户 ID</span>
              <strong>{{ mallApp.profile?.id || mallApp.authState.userId }}</strong>
            </div>
          </div>
        </section>

        <section class="sidebar-card">
          <p class="eyebrow">Account Security</p>
          <h3>修改密码</h3>
          <el-form label-position="top" class="password-form">
            <el-form-item label="旧密码">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                show-password
                placeholder="请输入当前密码"
              />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                show-password
                placeholder="请输入新密码"
              />
            </el-form-item>
            <el-form-item label="确认新密码">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                show-password
                placeholder="再次输入新密码"
                @keyup.enter="handlePasswordUpdate"
              />
            </el-form-item>
            <el-button
              type="danger"
              :loading="passwordSubmitting"
              @click="handlePasswordUpdate"
            >
              提交修改
            </el-button>
          </el-form>
        </section>

        <section class="sidebar-card">
          <p class="eyebrow">Recent Orders</p>
          <h3>最近动态</h3>
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
                <button type="button" @click="mallApp.openOrder(order)">查看</button>
              </div>
            </article>
            <el-empty
              v-if="!mallApp.recentOrders.length && !mallApp.ordersLoading"
              description="暂无最近订单"
              :image-size="80"
            />
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>
