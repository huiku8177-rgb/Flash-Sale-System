<script setup>
import { ArrowDown } from "@element-plus/icons-vue";
import { inject } from "vue";
import { useRouter } from "vue-router";

const mallApp = inject("mallApp");
const router = useRouter();

function goToOrders() {
  router.push({ name: "app-orders" });
}

function goToAccountInfo() {
  router.push({ name: "app-account-profile" });
}

function goToPasswordCenter() {
  router.push({ name: "app-account-security" });
}
</script>

<template>
  <div class="page-stack">
    <section class="profile-hub-hero section-card">
      <div class="profile-hub-copy">
        <p class="eyebrow">User Center</p>
        <h2>个人中心导航</h2>
        <p>
          个人中心现在只保留导航和入口，把订单、账户信息与安全设置拆成独立路由，让页面层次更轻、更清楚。
        </p>
      </div>

      <div class="profile-page-badge">
        <span>{{ mallApp.profileDisplayName }}</span>
        <strong>ID {{ mallApp.authState.userId }}</strong>
      </div>
    </section>

    <section class="profile-hub-grid">
      <article class="profile-entry-card profile-entry-card-wide">
        <div class="profile-entry-head">
          <div>
            <p class="eyebrow">Order Center</p>
            <h3>我的订单</h3>
          </div>
          <el-tag type="danger" effect="plain">{{ mallApp.orderStats.total }} 条</el-tag>
        </div>

        <p>
          订单中心会单独承接普通订单、秒杀订单、待支付筛选、订单详情与支付动作，不再和账户信息混排。
        </p>

        <div class="profile-entry-metrics">
          <div>
            <span>普通订单</span>
            <strong>{{ mallApp.orderStats.normal }}</strong>
          </div>
          <div>
            <span>秒杀订单</span>
            <strong>{{ mallApp.orderStats.seckill }}</strong>
          </div>
          <div>
            <span>待支付</span>
            <strong>{{ mallApp.orderStats.created }}</strong>
          </div>
        </div>

        <el-button type="danger" size="large" @click="goToOrders">进入订单中心</el-button>
      </article>

      <article class="profile-entry-card">
        <div class="profile-entry-head">
          <div>
            <p class="eyebrow">Account Center</p>
            <h3>账户与安全</h3>
          </div>
          <el-tag type="success" effect="plain">2 个入口</el-tag>
        </div>

        <p>
          账户信息和修改密码改成下拉导航，再分别进入独立页面处理，让交互更接近真实商城的个人中心。
        </p>

        <div class="profile-entry-dropdown">
          <el-dropdown trigger="click">
            <el-button size="large">
              选择账户功能
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goToAccountInfo">账户信息</el-dropdown-item>
                <el-dropdown-item @click="goToPasswordCenter">修改密码</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <div class="profile-entry-mini">
          <div>
            <span>用户名</span>
            <strong>{{ mallApp.profile?.username || mallApp.authState.username }}</strong>
          </div>
          <div>
            <span>购物车概览</span>
            <strong>{{ mallApp.cartSummary.count }} 件商品</strong>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>
