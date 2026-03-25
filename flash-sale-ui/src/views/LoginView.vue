<script setup>
import { onBeforeUnmount, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import AuthForm from "../components/auth/AuthForm.vue";
import AuthScene from "../components/auth/AuthScene.vue";
import { login, register } from "../api/auth";
import { useAuthForm } from "../composables/useAuthForm";
import { setSession } from "../stores/auth";

const route = useRoute();
const router = useRouter();

const { sceneInput, formModel } = useAuthForm({
  route,
  router,
  loginAction: login,
  registerAction: register,
  notify: ElMessage,
  setSession
});

onMounted(() => {
  document.body.classList.add("auth-page-mode");
});

onBeforeUnmount(() => {
  document.body.classList.remove("auth-page-mode");
});
</script>

<template>
  <div class="auth-page">
    <main class="auth-stage">
      <AuthScene :input="sceneInput" />
      <AuthForm :model="formModel" />
    </main>
  </div>
</template>

<style scoped>
:global(body.auth-page-mode) {
  min-width: 320px;
  background: #ffffff;
}

.auth-page {
  min-height: 100vh;
  background: #ffffff;
  color: #111827;
}

.auth-stage {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.02fr) minmax(460px, 0.98fr);
}

@media (max-width: 1120px) {
  .auth-stage {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
