<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { login, register } from "../api/auth";
import { setSession } from "../stores/auth";

const router = useRouter();
const activeTab = ref("login");
const submitting = ref(false);

const loginForm = reactive({
  username: "",
  password: ""
});

const registerForm = reactive({
  username: "",
  password: "",
  confirmPassword: ""
});

async function submitLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning("请输入用户名和密码");
    return;
  }

  submitting.value = true;
  try {
    const user = await login(loginForm);
    setSession(user);
    ElMessage.success(`欢迎回来，${user.username}`);
    router.push({ name: "app-home" });
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    submitting.value = false;
  }
}

async function submitRegister() {
  if (!registerForm.username || !registerForm.password) {
    ElMessage.warning("请补全注册信息");
    return;
  }

  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning("两次输入的密码不一致");
    return;
  }

  submitting.value = true;
  try {
    await register({
      username: registerForm.username,
      password: registerForm.password
    });
    ElMessage.success("注册成功，请直接登录");
    activeTab.value = "login";
    loginForm.username = registerForm.username;
    loginForm.password = registerForm.password;
    registerForm.confirmPassword = "";
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <div class="auth-shell">
    <div class="auth-hero">
      <p class="eyebrow">Flash Sale System</p>
      <h1>高并发秒杀控制台</h1>
      <p class="hero-copy">
        统一走你当前仓库的鉴权、秒杀与订单接口，页面已经适配
        <code>Result&lt;T&gt;</code> 包装返回和秒杀轮询流程。
      </p>
      <div class="hero-metrics">
        <div>
          <span>接口入口</span>
          <strong>/auth /product /seckill-product /seckill /order</strong>
        </div>
        <div>
          <span>核心交互</span>
          <strong>登录、抢购、轮询、查单</strong>
        </div>
      </div>
    </div>

    <el-card class="auth-card" shadow="never">
      <template #header>
        <div class="auth-card-header">
          <span>账号接入</span>
          <small>JWT + Gateway</small>
        </div>
      </template>

      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form label-position="top">
            <el-form-item label="用户名">
              <el-input
                v-model="loginForm.username"
                placeholder="例如 alice"
                size="large"
              />
            </el-form-item>
            <el-form-item label="密码">
              <el-input
                v-model="loginForm.password"
                type="password"
                show-password
                placeholder="请输入密码"
                size="large"
                @keyup.enter="submitLogin"
              />
            </el-form-item>
            <el-button
              type="primary"
              class="submit-button"
              size="large"
              :loading="submitting"
              @click="submitLogin"
            >
              登录并进入控制台
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form label-position="top">
            <el-form-item label="用户名">
              <el-input
                v-model="registerForm.username"
                placeholder="创建一个账号名"
                size="large"
              />
            </el-form-item>
            <el-form-item label="密码">
              <el-input
                v-model="registerForm.password"
                type="password"
                show-password
                placeholder="至少 6 位更合适"
                size="large"
              />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                show-password
                placeholder="再次输入密码"
                size="large"
                @keyup.enter="submitRegister"
              />
            </el-form-item>
            <el-button
              class="submit-button submit-button-secondary"
              size="large"
              :loading="submitting"
              @click="submitRegister"
            >
              创建账号
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>
