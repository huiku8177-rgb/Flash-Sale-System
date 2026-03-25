import { computed, onBeforeUnmount, reactive, ref, watch } from "vue";

const TYPING_IDLE_DELAY = 700;
const FIELD_BLUR_DELAY = 80;
const FEEDBACK_RESET_DELAY = 1400;

const COPY = {
  registerUsernameIdle: "\u7528\u6237\u540d\u9700\u8981 3 \u5230 32 \u4f4d\u3002",
  registerUsernameValid: "\u7528\u6237\u540d\u957f\u5ea6\u53ef\u7528\u3002",
  registerUsernameInvalid:
    "\u7528\u6237\u540d\u957f\u5ea6\u9700\u8981\u63a7\u5236\u5728 3 \u5230 32 \u4f4d\u4e4b\u95f4\u3002",
  registerPasswordIdle:
    "\u81f3\u5c11 6 \u4f4d\uff0c\u5efa\u8bae\u540c\u65f6\u5305\u542b\u5b57\u6bcd\u3001\u6570\u5b57\u548c\u7b26\u53f7\u3002",
  passwordStrengthPrefix: "\u5bc6\u7801\u5f3a\u5ea6\uff1a",
  confirmPasswordIdle: "\u8bf7\u518d\u6b21\u8f93\u5165\u5bc6\u7801\u4ee5\u5b8c\u6210\u786e\u8ba4\u3002",
  confirmPasswordMatch: "\u4e24\u6b21\u8f93\u5165\u7684\u5bc6\u7801\u4e00\u81f4\u3002",
  confirmPasswordMismatch: "\u4e24\u6b21\u8f93\u5165\u7684\u5bc6\u7801\u4e0d\u4e00\u81f4\u3002",
  loginTitle: "\u6b22\u8fce\u56de\u6765",
  registerTitle: "\u521b\u5efa\u4f60\u7684\u8d26\u53f7",
  loginSubtitle:
    "\u8bf7\u8f93\u5165\u4f60\u7684\u8d26\u53f7\u4fe1\u606f\u4ee5\u7ee7\u7eed\u4f7f\u7528 FlashSale\u3002",
  registerSubtitle:
    "\u521b\u5efa\u8d26\u53f7\u540e\u5373\u53ef\u7ee7\u7eed\u53c2\u4e0e\u79d2\u6740\u548c\u8d2d\u7269\u3002",
  redirectHintPrefix: "\u767b\u5f55\u6210\u529f\u540e\u5c06\u8fd4\u56de",
  loginUsernameWarning: "\u8bf7\u8f93\u5165\u81f3\u5c11 3 \u4f4d\u7684\u7528\u6237\u540d\u3002",
  loginPasswordWarning: "\u8bf7\u8f93\u5165\u81f3\u5c11 6 \u4f4d\u7684\u5bc6\u7801\u3002",
  loginSuccessPrefix: "\u6b22\u8fce\u56de\u6765\uff0c",
  loginError: "\u767b\u5f55\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002",
  registerUsernameWarning:
    "\u7528\u6237\u540d\u957f\u5ea6\u9700\u8981\u63a7\u5236\u5728 3 \u5230 32 \u4f4d\u4e4b\u95f4\u3002",
  registerPasswordWarning: "\u5bc6\u7801\u957f\u5ea6\u81f3\u5c11\u9700\u8981 6 \u4f4d\u3002",
  registerConfirmWarning:
    "\u8bf7\u786e\u8ba4\u4e24\u6b21\u8f93\u5165\u7684\u5bc6\u7801\u4fdd\u6301\u4e00\u81f4\u3002",
  registerSuccess: "\u6ce8\u518c\u6210\u529f\uff0c\u8bf7\u4f7f\u7528\u65b0\u8d26\u53f7\u767b\u5f55\u3002",
  registerError: "\u6ce8\u518c\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002",
  helperNote: "\u6ce8\u518c\u6210\u529f\u540e\u4f1a\u81ea\u52a8\u8fd4\u56de\u767b\u5f55\u9875\u3002"
};

export function useAuthForm({
  route,
  router,
  loginAction,
  registerAction,
  notify,
  setSession
}) {
  const preferences = reactive({
    rememberSession: true
  });

  const loginForm = reactive({
    username: "",
    password: ""
  });

  const registerForm = reactive({
    username: "",
    password: "",
    confirmPassword: ""
  });

  const passwordVisibility = reactive({
    login: false,
    register: false,
    confirm: false
  });

  const activeField = ref(null);
  const isTyping = ref(false);
  const submittingAction = ref("");
  const feedbackState = ref("idle");

  const timeoutIds = new Set();
  let typingTimerId = 0;
  let blurTimerId = 0;
  let feedbackTimerId = 0;

  const mode = computed(() => resolveAuthMode(route));
  const isRegisterPage = computed(() => mode.value === "register");
  const isSubmitting = computed(() => Boolean(submittingAction.value));
  const isLoginSubmitting = computed(() => submittingAction.value === "login");
  const isRegisterSubmitting = computed(() => submittingAction.value === "register");

  const visiblePasswordField = computed(() => {
    if (mode.value === "login") {
      return passwordVisibility.login ? "password" : null;
    }

    if (passwordVisibility.confirm && activeField.value === "confirm") {
      return "confirm";
    }

    if (passwordVisibility.register && activeField.value === "password") {
      return "password";
    }

    if (passwordVisibility.confirm) {
      return "confirm";
    }

    if (passwordVisibility.register) {
      return "password";
    }

    return null;
  });

  const passwordField = computed(() => {
    if (visiblePasswordField.value) {
      return visiblePasswordField.value;
    }

    if (activeField.value === "confirm") {
      return "confirm";
    }

    return "password";
  });

  const activePasswordValue = computed(() => {
    if (mode.value === "login") {
      return loginForm.password;
    }

    return passwordField.value === "confirm"
      ? registerForm.confirmPassword
      : registerForm.password;
  });

  const hasPasswordValue = computed(() => activePasswordValue.value.length > 0);
  const isPasswordVisible = computed(() => visiblePasswordField.value !== null);

  const loginUsernameValid = computed(() => loginForm.username.trim().length >= 3);
  const loginPasswordValid = computed(() => loginForm.password.length >= 6);
  const registerUsernameValid = computed(() => {
    const length = registerForm.username.trim().length;
    return length >= 3 && length <= 32;
  });
  const registerPasswordValid = computed(() => registerForm.password.length >= 6);

  const passwordStrength = computed(() => evaluatePasswordStrength(registerForm.password));
  const confirmPasswordState = computed(() => {
    if (!registerForm.confirmPassword) {
      return "idle";
    }

    return registerForm.confirmPassword === registerForm.password ? "match" : "mismatch";
  });

  const loginDisabled = computed(
    () => isSubmitting.value || !loginUsernameValid.value || !loginPasswordValid.value
  );

  const registerDisabled = computed(
    () =>
      isSubmitting.value ||
      !registerUsernameValid.value ||
      !registerPasswordValid.value ||
      confirmPasswordState.value !== "match"
  );

  const registerUsernameHint = computed(() => {
    if (!registerForm.username) {
      return {
        text: COPY.registerUsernameIdle,
        tone: "muted"
      };
    }

    if (registerUsernameValid.value) {
      return {
        text: COPY.registerUsernameValid,
        tone: "success"
      };
    }

    return {
      text: COPY.registerUsernameInvalid,
      tone: "danger"
    };
  });

  const registerPasswordHint = computed(() => {
    if (!registerForm.password) {
      return {
        text: COPY.registerPasswordIdle,
        tone: "muted"
      };
    }

    return {
      text: `${COPY.passwordStrengthPrefix}${passwordStrength.value.label}`,
      tone: passwordStrength.value.tone
    };
  });

  const confirmPasswordHint = computed(() => {
    if (!registerForm.confirmPassword) {
      return {
        text: COPY.confirmPasswordIdle,
        tone: "muted"
      };
    }

    if (confirmPasswordState.value === "match") {
      return {
        text: COPY.confirmPasswordMatch,
        tone: "success"
      };
    }

    return {
      text: COPY.confirmPasswordMismatch,
      tone: "danger"
    };
  });

  const surfaceState = computed(() => {
    if (isSubmitting.value) {
      return "submitting";
    }

    return feedbackState.value;
  });

  const title = computed(() => (isRegisterPage.value ? COPY.registerTitle : COPY.loginTitle));
  const subtitle = computed(() =>
    isRegisterPage.value ? COPY.registerSubtitle : COPY.loginSubtitle
  );

  const redirectHint = computed(() => {
    if (isRegisterPage.value) {
      return "";
    }

    const redirect = normalizeRedirect(route.query.redirect);
    if (!redirect) {
      return "";
    }

    return `${COPY.redirectHintPrefix}${resolveRouteLabel(redirect)}\u3002`;
  });

  const sceneInput = computed(() => ({
    mode: mode.value,
    activeField: activeField.value,
    passwordField: passwordField.value,
    visiblePasswordField: visiblePasswordField.value,
    isTyping: isTyping.value,
    isSubmitting: isSubmitting.value,
    feedbackState: feedbackState.value,
    isPasswordVisible: isPasswordVisible.value,
    hasPasswordValue: hasPasswordValue.value,
    confirmState: confirmPasswordState.value
  }));

  watch(
    () => [route.name, route.query.mode],
    ([name, queryMode]) => {
      if (name === "login" && queryMode === "register") {
        router.replace(buildAuthRoute("register", route));
      }
    },
    { immediate: true }
  );

  watch(
    mode,
    (nextMode, previousMode) => {
      clearTimer("typing");
      clearTimer("blur");
      clearTimer("feedback");

      activeField.value = null;
      isTyping.value = false;
      feedbackState.value = "idle";
      passwordVisibility.login = false;
      passwordVisibility.register = false;
      passwordVisibility.confirm = false;

      if (!previousMode) {
        return;
      }

      if (nextMode === "register") {
        registerForm.username = registerForm.username || loginForm.username.trim();
      } else {
        loginForm.username = loginForm.username || registerForm.username.trim();
      }

      loginForm.password = "";
      registerForm.password = "";
      registerForm.confirmPassword = "";
    },
    { immediate: true }
  );

  onBeforeUnmount(() => {
    for (const id of timeoutIds) {
      window.clearTimeout(id);
    }
    timeoutIds.clear();
  });

  function handleFieldFocus(field) {
    clearTimer("blur");
    activeField.value = field;
  }

  function handleFieldBlur() {
    clearTimer("blur");
    blurTimerId = schedule(() => {
      activeField.value = null;
      blurTimerId = 0;
    }, FIELD_BLUR_DELAY);
  }

  function handleFieldInput(field) {
    handleFieldFocus(field);
    clearFeedback();
    isTyping.value = true;
    clearTimer("typing");

    typingTimerId = schedule(() => {
      isTyping.value = false;
      typingTimerId = 0;
    }, TYPING_IDLE_DELAY);
  }

  function togglePasswordVisibility(target = "login") {
    if (target === "register") {
      passwordVisibility.register = !passwordVisibility.register;
      handleFieldFocus("password");
      clearFeedback();
      clearTimer("typing");
      isTyping.value = false;
      return;
    }

    if (target === "confirm") {
      passwordVisibility.confirm = !passwordVisibility.confirm;
      handleFieldFocus("confirm");
      clearFeedback();
      clearTimer("typing");
      isTyping.value = false;
      return;
    }

    passwordVisibility.login = !passwordVisibility.login;
    handleFieldFocus("password");
    clearFeedback();
    clearTimer("typing");
    isTyping.value = false;
  }

  async function submitLogin() {
    const username = loginForm.username.trim();

    if (!loginUsernameValid.value) {
      notify.warning(COPY.loginUsernameWarning);
      return;
    }

    if (!loginPasswordValid.value) {
      notify.warning(COPY.loginPasswordWarning);
      return;
    }

    submittingAction.value = "login";
    feedbackState.value = "submitting";

    try {
      const user = await loginAction({
        username,
        password: loginForm.password
      });

      setSession(user, {
        persist: preferences.rememberSession
      });

      feedbackState.value = "success";
      notify.success(`${COPY.loginSuccessPrefix}${user.username}`);

      const redirect = normalizeRedirect(route.query.redirect);
      await router.push(redirect || { name: "app-home" });
    } catch (error) {
      pushFeedback("error");
      notify.error(extractMessage(error, COPY.loginError));
    } finally {
      submittingAction.value = "";
      if (feedbackState.value === "submitting") {
        feedbackState.value = "idle";
      }
    }
  }

  async function submitRegister() {
    const username = registerForm.username.trim();

    if (!registerUsernameValid.value) {
      notify.warning(COPY.registerUsernameWarning);
      return;
    }

    if (!registerPasswordValid.value) {
      notify.warning(COPY.registerPasswordWarning);
      return;
    }

    if (confirmPasswordState.value !== "match") {
      notify.warning(COPY.registerConfirmWarning);
      return;
    }

    submittingAction.value = "register";
    feedbackState.value = "submitting";

    try {
      await registerAction({
        username,
        password: registerForm.password
      });

      loginForm.username = username;
      loginForm.password = "";
      registerForm.password = "";
      registerForm.confirmPassword = "";
      passwordVisibility.register = false;
      passwordVisibility.confirm = false;

      feedbackState.value = "success";
      notify.success(COPY.registerSuccess);
      await router.push(buildAuthRoute("login", route));
    } catch (error) {
      pushFeedback("error");
      notify.error(extractMessage(error, COPY.registerError));
    } finally {
      submittingAction.value = "";
      if (feedbackState.value === "submitting") {
        feedbackState.value = "idle";
      }
    }
  }

  function switchMode(nextMode) {
    router.push(buildAuthRoute(nextMode, route));
  }

  function schedule(callback, delay) {
    const id = window.setTimeout(() => {
      timeoutIds.delete(id);
      callback();
    }, delay);

    timeoutIds.add(id);
    return id;
  }

  function clearTimer(type) {
    if (type === "typing" && typingTimerId) {
      clearScheduled(typingTimerId);
      typingTimerId = 0;
    }

    if (type === "blur" && blurTimerId) {
      clearScheduled(blurTimerId);
      blurTimerId = 0;
    }

    if (type === "feedback" && feedbackTimerId) {
      clearScheduled(feedbackTimerId);
      feedbackTimerId = 0;
    }
  }

  function clearScheduled(id) {
    window.clearTimeout(id);
    timeoutIds.delete(id);
  }

  function clearFeedback() {
    if (feedbackState.value === "error" || feedbackState.value === "success") {
      feedbackState.value = "idle";
      clearTimer("feedback");
    }
  }

  function pushFeedback(nextState) {
    feedbackState.value = nextState;
    clearTimer("feedback");
    feedbackTimerId = schedule(() => {
      feedbackState.value = "idle";
      feedbackTimerId = 0;
    }, FEEDBACK_RESET_DELAY);
  }

  return {
    sceneInput,
    formModel: computed(() => ({
      mode: mode.value,
      isRegisterPage: isRegisterPage.value,
      title: title.value,
      subtitle: subtitle.value,
      surfaceState: surfaceState.value,
      loginForm,
      registerForm,
      preferences,
      passwordVisibility,
      redirectHint: redirectHint.value,
      registerUsernameHint: registerUsernameHint.value,
      registerPasswordHint: registerPasswordHint.value,
      confirmPasswordHint: confirmPasswordHint.value,
      confirmPasswordState: confirmPasswordState.value,
      passwordStrength: passwordStrength.value,
      helperNote: COPY.helperNote,
      isLoginSubmitting: isLoginSubmitting.value,
      isRegisterSubmitting: isRegisterSubmitting.value,
      loginDisabled: loginDisabled.value,
      registerDisabled: registerDisabled.value,
      handleFieldFocus,
      handleFieldBlur,
      handleFieldInput,
      togglePasswordVisibility,
      submitLogin,
      submitRegister,
      switchMode
    }))
  };
}

function resolveAuthMode(route) {
  return route.name === "register" || route.query.mode === "register"
    ? "register"
    : "login";
}

function buildAuthRoute(mode, route) {
  const redirect = normalizeRedirect(route.query.redirect);
  const target = {
    name: mode === "register" ? "register" : "login"
  };

  if (redirect) {
    target.query = {
      redirect
    };
  }

  return target;
}

function normalizeRedirect(candidate) {
  if (typeof candidate !== "string") {
    return "";
  }

  if (
    !candidate.startsWith("/") ||
    candidate.startsWith("/login") ||
    candidate.startsWith("/register")
  ) {
    return "";
  }

  return candidate;
}

function resolveRouteLabel(path) {
  if (path.startsWith("/checkout")) {
    return "\u7ed3\u7b97\u9875";
  }

  if (path.startsWith("/app/cart")) {
    return "\u8d2d\u7269\u8f66";
  }

  if (path.startsWith("/app/profile/orders")) {
    return "\u8ba2\u5355\u4e2d\u5fc3";
  }

  if (path.startsWith("/app/profile/account")) {
    return "\u8d26\u6237\u8d44\u6599";
  }

  if (path.startsWith("/app/profile/security")) {
    return "\u5bc6\u7801\u5b89\u5168";
  }

  if (path.startsWith("/app/profile")) {
    return "\u4e2a\u4eba\u4e2d\u5fc3";
  }

  if (path.startsWith("/app/flash")) {
    return "\u79d2\u6740\u6d3b\u52a8\u9875";
  }

  if (path.startsWith("/app/home")) {
    return "\u5546\u57ce\u9996\u9875";
  }

  return path;
}

function evaluatePasswordStrength(password) {
  if (!password) {
    return {
      key: "idle",
      label: "\u5f85\u8f93\u5165",
      level: 0,
      tone: "muted"
    };
  }

  let score = 0;

  if (password.length >= 6) {
    score += 1;
  }

  if (/[a-zA-Z]/.test(password)) {
    score += 1;
  }

  if (/\d/.test(password)) {
    score += 1;
  }

  if (/[^a-zA-Z0-9]/.test(password) || password.length >= 10) {
    score += 1;
  }

  if (password.length < 6 || score <= 1) {
    return {
      key: "weak",
      label: "\u8f83\u5f31",
      level: 1,
      tone: "danger"
    };
  }

  if (score === 2 || score === 3) {
    return {
      key: "medium",
      label: "\u4e2d\u7b49",
      level: 2,
      tone: "warning"
    };
  }

  return {
    key: "strong",
    label: "\u8f83\u5f3a",
    level: 3,
    tone: "success"
  };
}

function extractMessage(error, fallback) {
  return error instanceof Error && error.message ? error.message : fallback;
}
