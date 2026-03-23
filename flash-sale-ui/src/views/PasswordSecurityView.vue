<script setup>
import { inject, reactive, ref } from "vue";
import { useRouter } from "vue-router";

const mallApp = inject("mallApp");
const router = useRouter();
const passwordForm = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: ""
});
const passwordSubmitting = ref(false);

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

function backToProfile() {
  router.push({ name: "app-profile" });
}
</script>

<template>
  <div class="page-stack">
    <section class="profile-hub-hero section-card">
      <div class="profile-hub-copy">
        <p class="eyebrow">Account Security</p>
        <h2>修改密码</h2>
        <p>安全能力单独成页后，表单重心会更明确，也更像正式网站里的账户安全中心。</p>
      </div>
      <div class="profile-page-badge">
        <span>安全中心</span>
        <strong>密码管理</strong>
      </div>
    </section>

    <section class="section-card security-form-shell">
      <div class="section-head">
        <div>
          <p class="eyebrow">Security Form</p>
          <h3>更新登录密码</h3>
        </div>
        <el-button text @click="backToProfile">返回个人中心</el-button>
      </div>

      <el-form label-position="top" class="password-form security-form-grid">
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
      </el-form>

      <div class="security-form-actions">
        <el-button
          type="danger"
          size="large"
          :loading="passwordSubmitting"
          @click="handlePasswordUpdate"
        >
          提交修改
        </el-button>
      </div>
    </section>
  </div>
</template>
