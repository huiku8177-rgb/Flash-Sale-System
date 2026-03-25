<script setup>
import { computed } from "vue";

const labels = {
  username: "\u7528\u6237\u540d",
  password: "\u5bc6\u7801",
  confirmPassword: "\u786e\u8ba4\u5bc6\u7801",
  loginUsernamePlaceholder: "\u8bf7\u8f93\u5165\u7528\u6237\u540d",
  loginPasswordPlaceholder: "\u8bf7\u8f93\u5165\u5bc6\u7801",
  registerUsernamePlaceholder: "\u521b\u5efa 3 \u5230 32 \u4f4d\u8d26\u53f7\u540d",
  registerPasswordPlaceholder: "\u81f3\u5c11 6 \u4f4d\u5bc6\u7801",
  registerConfirmPlaceholder: "\u518d\u6b21\u8f93\u5165\u5bc6\u7801",
  rememberSession: "\u8bb0\u4f4f\u767b\u5f55\u72b6\u6001",
  goRegister: "\u6ca1\u6709\u8d26\u53f7\uff1f\u6ce8\u518c",
  backToLogin: "\u8fd4\u56de\u767b\u5f55",
  login: "\u767b\u5f55",
  createAccount: "\u521b\u5efa\u8d26\u53f7",
  showShort: "\u663e\u793a",
  hideShort: "\u9690\u85cf",
  showPassword: "\u663e\u793a\u5bc6\u7801",
  hidePassword: "\u9690\u85cf\u5bc6\u7801"
};

const props = defineProps({
  model: {
    type: Object,
    required: true
  }
});

const model = computed(() => props.model);

const strengthSteps = [1, 2, 3];

function toneClass(tone) {
  return tone ? `is-${tone}` : "";
}

function passwordToggleLabel(visible) {
  return visible ? labels.hidePassword : labels.showPassword;
}
</script>

<template>
  <section class="auth-panel">
    <div class="auth-card" :class="`is-${model.surfaceState}`">
      <div class="auth-card__head">
        <h1>{{ model.title }}</h1>
        <p>{{ model.subtitle }}</p>
      </div>

      <form v-if="!model.isRegisterPage" class="auth-form" @submit.prevent="model.submitLogin">
        <label class="auth-field">
          <span>{{ labels.username }}</span>
          <el-input
            v-model="model.loginForm.username"
            clearable
            size="large"
            autocomplete="username"
            :placeholder="labels.loginUsernamePlaceholder"
            @focus="model.handleFieldFocus('username')"
            @blur="model.handleFieldBlur"
            @input="model.handleFieldInput('username')"
          />
        </label>

        <label class="auth-field">
          <span>{{ labels.password }}</span>
          <el-input
            v-model="model.loginForm.password"
            :type="model.passwordVisibility.login ? 'text' : 'password'"
            size="large"
            autocomplete="current-password"
            :placeholder="labels.loginPasswordPlaceholder"
            @focus="model.handleFieldFocus('password')"
            @blur="model.handleFieldBlur"
            @input="model.handleFieldInput('password')"
          >
            <template #suffix>
              <button
                class="password-toggle"
                type="button"
                :aria-label="passwordToggleLabel(model.passwordVisibility.login)"
                @mousedown.prevent
                @click="model.togglePasswordVisibility('login')"
              >
                {{ model.passwordVisibility.login ? labels.hideShort : labels.showShort }}
              </button>
            </template>
          </el-input>
        </label>

        <div class="auth-form__row">
          <el-checkbox v-model="model.preferences.rememberSession" size="large">
            {{ labels.rememberSession }}
          </el-checkbox>
          <button class="text-link" type="button" @click="model.switchMode('register')">
            {{ labels.goRegister }}
          </button>
        </div>

        <p v-if="model.redirectHint" class="auth-redirect-hint">
          {{ model.redirectHint }}
        </p>

        <el-button
          class="auth-submit auth-submit--login"
          native-type="submit"
          size="large"
          :loading="model.isLoginSubmitting"
          :disabled="model.loginDisabled"
        >
          {{ labels.login }}
        </el-button>
      </form>

      <form v-else class="auth-form" @submit.prevent="model.submitRegister">
        <label class="auth-field">
          <span>{{ labels.username }}</span>
          <el-input
            v-model="model.registerForm.username"
            clearable
            size="large"
            autocomplete="username"
            :placeholder="labels.registerUsernamePlaceholder"
            @focus="model.handleFieldFocus('username')"
            @blur="model.handleFieldBlur"
            @input="model.handleFieldInput('username')"
          />
          <div class="field-meta">
            <span class="field-hint" :class="toneClass(model.registerUsernameHint.tone)">
              {{ model.registerUsernameHint.text }}
            </span>
          </div>
        </label>

        <label class="auth-field">
          <span>{{ labels.password }}</span>
          <el-input
            v-model="model.registerForm.password"
            :type="model.passwordVisibility.register ? 'text' : 'password'"
            size="large"
            autocomplete="new-password"
            :placeholder="labels.registerPasswordPlaceholder"
            @focus="model.handleFieldFocus('password')"
            @blur="model.handleFieldBlur"
            @input="model.handleFieldInput('password')"
          >
            <template #suffix>
              <button
                class="password-toggle"
                type="button"
                :aria-label="passwordToggleLabel(model.passwordVisibility.register)"
                @mousedown.prevent
                @click="model.togglePasswordVisibility('register')"
              >
                {{ model.passwordVisibility.register ? labels.hideShort : labels.showShort }}
              </button>
            </template>
          </el-input>
          <div class="field-meta field-meta--between">
            <span class="field-hint" :class="toneClass(model.registerPasswordHint.tone)">
              {{ model.registerPasswordHint.text }}
            </span>
            <div class="strength-meter" aria-hidden="true">
              <span
                v-for="step in strengthSteps"
                :key="step"
                class="strength-meter__bar"
                :class="[
                  `is-${model.passwordStrength.tone}`,
                  { 'is-active': model.passwordStrength.level >= step }
                ]"
              ></span>
            </div>
          </div>
        </label>

        <label class="auth-field">
          <span>{{ labels.confirmPassword }}</span>
          <el-input
            v-model="model.registerForm.confirmPassword"
            :type="model.passwordVisibility.confirm ? 'text' : 'password'"
            size="large"
            autocomplete="new-password"
            :placeholder="labels.registerConfirmPlaceholder"
            @focus="model.handleFieldFocus('confirm')"
            @blur="model.handleFieldBlur"
            @input="model.handleFieldInput('confirm')"
          >
            <template #suffix>
              <button
                class="password-toggle"
                type="button"
                :aria-label="passwordToggleLabel(model.passwordVisibility.confirm)"
                @mousedown.prevent
                @click="model.togglePasswordVisibility('confirm')"
              >
                {{ model.passwordVisibility.confirm ? labels.hideShort : labels.showShort }}
              </button>
            </template>
          </el-input>
          <div class="field-meta">
            <span class="field-hint" :class="toneClass(model.confirmPasswordHint.tone)">
              {{ model.confirmPasswordHint.text }}
            </span>
          </div>
        </label>

        <div class="auth-form__row auth-form__row--end">
          <span class="auth-form__note">{{ model.helperNote }}</span>
          <button class="text-link" type="button" @click="model.switchMode('login')">
            {{ labels.backToLogin }}
          </button>
        </div>

        <el-button
          class="auth-submit auth-submit--register"
          native-type="submit"
          size="large"
          :loading="model.isRegisterSubmitting"
          :disabled="model.registerDisabled"
        >
          {{ labels.createAccount }}
        </el-button>
      </form>
    </div>
  </section>
</template>

<style scoped>
.auth-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 64px;
  background: #ffffff;
}

.auth-card {
  width: min(560px, 100%);
  display: grid;
  gap: 28px;
  padding: 44px 40px 40px;
  border: 1px solid #eef2f7;
  border-radius: 34px;
  background: #ffffff;
  box-shadow: 0 28px 70px rgba(15, 23, 42, 0.08);
  transition:
    border-color 0.24s ease,
    box-shadow 0.24s ease,
    transform 0.24s ease;
}

.auth-card.is-submitting {
  border-color: rgba(75, 99, 211, 0.26);
  box-shadow: 0 30px 70px rgba(75, 99, 211, 0.12);
}

.auth-card.is-success {
  border-color: rgba(34, 197, 94, 0.3);
  box-shadow: 0 30px 70px rgba(34, 197, 94, 0.1);
}

.auth-card.is-error {
  border-color: rgba(239, 68, 68, 0.28);
  animation: auth-card-shake 320ms ease;
}

.auth-card__head {
  text-align: center;
}

.auth-card__head h1 {
  margin: 0;
  color: #0f172a;
  font-size: clamp(40px, 5vw, 66px);
  font-weight: 800;
  line-height: 0.98;
  letter-spacing: -0.05em;
}

.auth-card__head p {
  margin: 14px 0 0;
  color: #64748b;
  font-size: 17px;
  line-height: 1.6;
}

.auth-form {
  display: grid;
  gap: 18px;
}

.auth-field {
  display: grid;
  gap: 10px;
}

.auth-field > span {
  color: #111827;
  font-size: 14px;
  font-weight: 700;
}

.auth-field :deep(.el-input__wrapper) {
  min-height: 60px;
  padding: 0 18px;
  border-radius: 999px !important;
  box-shadow: inset 0 0 0 1px #dfe5ef !important;
  transition:
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.auth-field :deep(.el-input__wrapper:hover) {
  box-shadow: inset 0 0 0 1px #cfd7e5 !important;
}

.auth-field :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    inset 0 0 0 2px rgba(75, 99, 211, 0.95) !important,
    0 14px 30px rgba(75, 99, 211, 0.08) !important;
}

.auth-field :deep(.el-input__inner) {
  font-size: 15px;
}

.auth-form__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.auth-form__row--end {
  justify-content: flex-end;
}

.auth-form__row :deep(.el-checkbox__label) {
  color: #374151;
  font-weight: 500;
}

.auth-form__note,
.auth-redirect-hint {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.field-meta {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  min-height: 18px;
}

.field-meta--between {
  justify-content: space-between;
}

.field-hint {
  color: #94a3b8;
  font-size: 13px;
  line-height: 1.4;
}

.field-hint.is-muted {
  color: #94a3b8;
}

.field-hint.is-warning {
  color: #d97706;
}

.field-hint.is-success {
  color: #15803d;
}

.field-hint.is-danger {
  color: #dc2626;
}

.strength-meter {
  display: inline-flex;
  gap: 6px;
}

.strength-meter__bar {
  width: 22px;
  height: 6px;
  border-radius: 999px;
  background: #e5e7eb;
  transition:
    background-color 0.18s ease,
    transform 0.18s ease;
}

.strength-meter__bar.is-active.is-warning {
  background: #f59e0b;
}

.strength-meter__bar.is-active.is-success {
  background: #22c55e;
}

.strength-meter__bar.is-active.is-danger {
  background: #ef4444;
}

.text-link,
.password-toggle {
  padding: 0;
  border: 0;
  background: transparent;
  color: #4b63d3;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.text-link:hover,
.password-toggle:hover {
  color: #354fc4;
}

.password-toggle {
  display: inline-flex;
  align-items: center;
  height: 100%;
}

.password-toggle:focus-visible,
.text-link:focus-visible {
  outline: 2px solid rgba(75, 99, 211, 0.45);
  outline-offset: 4px;
  border-radius: 999px;
}

.auth-submit {
  width: 100%;
  min-height: 58px;
  border-radius: 999px !important;
  font-size: 16px;
  font-weight: 700;
}

.auth-submit--login {
  border-color: #dfe5ef;
  background: #ffffff;
  color: #111827;
}

.auth-submit--login:hover,
.auth-submit--login:focus {
  border-color: #cfd7e5;
  background: #f8fafc;
  color: #111827;
}

.auth-submit--register {
  border-color: transparent;
  background: linear-gradient(135deg, #4b63d3, #6f82ec);
  color: #ffffff;
}

.auth-submit--register:hover,
.auth-submit--register:focus {
  border-color: transparent;
  background: linear-gradient(135deg, #4258c2, #6475dd);
  color: #ffffff;
}

.auth-submit.is-disabled,
.auth-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@keyframes auth-card-shake {
  0%,
  100% {
    transform: translateX(0);
  }

  25% {
    transform: translateX(-5px);
  }

  75% {
    transform: translateX(5px);
  }
}

@media (max-width: 1120px) {
  .auth-panel {
    padding: 36px 40px 44px;
  }

  .auth-card {
    width: min(680px, 100%);
  }
}

@media (max-width: 720px) {
  .auth-panel {
    padding: 30px 22px 40px;
  }

  .auth-card {
    padding: 32px 22px 28px;
    border-radius: 28px;
  }

  .auth-card__head h1 {
    font-size: 42px;
  }

  .auth-card__head p {
    font-size: 15px;
  }

  .auth-form__row {
    flex-direction: column;
    align-items: flex-start;
  }

  .auth-form__row--end {
    align-items: flex-start;
  }

  .field-meta,
  .field-meta--between {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (prefers-reduced-motion: reduce) {
  .auth-card,
  .auth-field :deep(.el-input__wrapper),
  .strength-meter__bar {
    transition: none;
  }

  .auth-card.is-error {
    animation: none;
  }
}
</style>
