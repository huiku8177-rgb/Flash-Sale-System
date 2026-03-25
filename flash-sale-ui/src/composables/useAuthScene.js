import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";

const GLANCE_DURATION = 720;
const BLINK_DURATION = 150;
const PEEK_DURATION = 780;

const DEFAULT_INPUT = {
  mode: "login",
  activeField: null,
  passwordField: "password",
  visiblePasswordField: null,
  isTyping: false,
  isSubmitting: false,
  feedbackState: "idle",
  isPasswordVisible: false,
  hasPasswordValue: false,
  confirmState: "idle"
};

const CHARACTER_CONFIG = {
  purple: {
    faceBase: { left: 60, top: 60 },
    facePointer: { x: 8, y: 5 },
    pupil: { maxX: 5, maxY: 5 },
    transformPointer: { x: 4, y: -2, skew: -3 }
  },
  orange: {
    faceBase: { left: 98, top: 118 },
    facePointer: { x: 4, y: 3 },
    eye: { maxX: 4, maxY: 4 },
    transformPointer: { x: 2, y: -1, skew: 0 }
  },
  black: {
    faceBase: { left: 37, top: 48 },
    facePointer: { x: 6, y: 5 },
    pupil: { maxX: 4, maxY: 4 },
    transformPointer: { x: 3, y: -2, skew: -2 }
  },
  yellow: {
    faceBase: { left: 49, top: 60 },
    facePointer: { x: 4, y: 3 },
    mouthBase: { left: 34, top: 128 },
    mouthPointer: { x: 2, y: 1 },
    eye: { maxX: 4, maxY: 4 },
    transformPointer: { x: 2, y: -1, skew: 0 }
  }
};

export function useAuthScene(sceneInput) {
  const panelRef = ref(null);
  const reducedMotion = ref(false);
  const hasMouseMoved = ref(false);
  const pairLookActive = ref(false);
  const purpleBlinking = ref(false);
  const blackBlinking = ref(false);
  const purplePeeking = ref(false);

  const pointer = reactive({
    x: 0,
    y: 0
  });

  const pointerTarget = reactive({
    x: 0,
    y: 0
  });

  const timers = {
    glance: 0,
    purpleBlink: 0,
    blackBlink: 0,
    purpleBlinkReset: 0,
    blackBlinkReset: 0,
    peek: 0,
    peekReset: 0
  };

  const timeoutIds = new Set();
  let rafId = 0;
  let motionMediaQuery = null;
  let removeMotionListener = () => {};

  const input = computed(() => sceneInput.value || DEFAULT_INPUT);

  const sceneMode = computed(() => {
    const isPasswordFieldActive =
      input.value.activeField === "password" || input.value.activeField === "confirm";
    const shouldShowPasswordReaction =
      input.value.isPasswordVisible && (input.value.hasPasswordValue || isPasswordFieldActive);

    if (input.value.feedbackState === "error") {
      return "error";
    }

    if (input.value.feedbackState === "success") {
      return "success";
    }

    if (input.value.isSubmitting) {
      return "submitting";
    }

    if (shouldShowPasswordReaction) {
      return "password-visible";
    }

    if (isPasswordFieldActive && input.value.hasPasswordValue) {
      return "password-hidden";
    }

    if (
      input.value.mode === "register" &&
      input.value.activeField === "confirm" &&
      input.value.confirmState === "mismatch"
    ) {
      return "register-confirm-mismatch";
    }

    if (
      input.value.mode === "register" &&
      input.value.activeField === "confirm" &&
      input.value.confirmState === "match"
    ) {
      return "register-confirm-match";
    }

    if (input.value.activeField === "username" || input.value.isTyping) {
      return "username-focus";
    }

    return "idle";
  });

  const mousePose = computed(() => {
    if (reducedMotion.value || !hasMouseMoved.value) {
      return {
        x: 0,
        y: 0
      };
    }

    return {
      x: pointer.x,
      y: pointer.y
    };
  });

  const characters = computed(() => {
    const scene = sceneMode.value;
    const pose = mousePose.value;

    return {
      purple: {
        wrapperStyle: resolveCharacterStyle("purple", scene, pose),
        faceStyle: resolveFaceStyle("purple", scene, pose, pairLookActive.value),
        pupilStyle: resolvePupilStyle("purple", scene, pose, pairLookActive.value, purplePeeking.value),
        blinking: purpleBlinking.value
      },
      orange: {
        wrapperStyle: resolveCharacterStyle("orange", scene, pose),
        faceStyle: resolveFaceStyle("orange", scene, pose, pairLookActive.value),
        dotStyle: resolveDotStyle("orange", scene, pose, pairLookActive.value)
      },
      black: {
        wrapperStyle: resolveCharacterStyle("black", scene, pose),
        faceStyle: resolveFaceStyle("black", scene, pose, pairLookActive.value),
        pupilStyle: resolvePupilStyle("black", scene, pose, pairLookActive.value, false),
        blinking: blackBlinking.value
      },
      yellow: {
        wrapperStyle: resolveCharacterStyle("yellow", scene, pose),
        faceStyle: resolveFaceStyle("yellow", scene, pose, pairLookActive.value),
        dotStyle: resolveDotStyle("yellow", scene, pose, pairLookActive.value),
        mouthStyle: resolveMouthStyle(scene, pose)
      }
    };
  });

  onMounted(() => {
    if (typeof window === "undefined") {
      return;
    }

    startReducedMotionWatcher();
    window.addEventListener("pointermove", handlePointerMove, { passive: true });
    restartAmbientMotion();
  });

  onBeforeUnmount(() => {
    if (typeof window !== "undefined") {
      window.removeEventListener("pointermove", handlePointerMove);
    }

    removeMotionListener();
    cancelAnimationFrameIfNeeded();
    clearAllTimers();
  });

  watch(
    sceneMode,
    (nextMode, previousMode) => {
      clearTimer("glance");

      if (reducedMotion.value) {
        pairLookActive.value = false;
        return;
      }

      if (shouldPairLook(nextMode) && previousMode !== nextMode) {
        pairLookActive.value = true;
        timers.glance = schedule(() => {
          pairLookActive.value = false;
          timers.glance = 0;
        }, GLANCE_DURATION);
        return;
      }

      if (!shouldPairLook(nextMode) || nextMode === "password-visible") {
        pairLookActive.value = false;
      }
    },
    { immediate: true }
  );

  watch(
    () => sceneMode.value === "password-visible",
    (visible) => {
      clearTimer("peek");
      clearTimer("peekReset");
      purplePeeking.value = false;

      if (visible && !reducedMotion.value) {
        schedulePurplePeek();
      }
    },
    { immediate: true }
  );

  watch(reducedMotion, (nextValue) => {
    if (nextValue) {
      pairLookActive.value = false;
      purpleBlinking.value = false;
      blackBlinking.value = false;
      purplePeeking.value = false;
      pointer.x = 0;
      pointer.y = 0;
      pointerTarget.x = 0;
      pointerTarget.y = 0;
      cancelAnimationFrameIfNeeded();
      clearTimer("peek");
      clearTimer("peekReset");
      clearTimer("purpleBlink");
      clearTimer("blackBlink");
      clearTimer("purpleBlinkReset");
      clearTimer("blackBlinkReset");
      return;
    }

    restartAmbientMotion();
    if (sceneMode.value === "password-visible") {
      schedulePurplePeek();
    }
  });

  function handlePointerMove(event) {
    if (reducedMotion.value || !panelRef.value) {
      return;
    }

    const rect = panelRef.value.getBoundingClientRect();
    if (!rect.width || !rect.height) {
      return;
    }

    hasMouseMoved.value = true;

    const focusX = rect.left + rect.width * 0.38;
    const focusY = rect.top + rect.height * 0.58;

    pointerTarget.x = clamp((event.clientX - focusX) / (rect.width * 0.32), -1, 1);
    pointerTarget.y = clamp((event.clientY - focusY) / (rect.height * 0.34), -1, 1);

    if (!rafId) {
      rafId = window.requestAnimationFrame(updatePointer);
    }
  }

  function updatePointer() {
    rafId = 0;

    pointer.x = interpolate(pointer.x, pointerTarget.x, 0.18);
    pointer.y = interpolate(pointer.y, pointerTarget.y, 0.18);

    if (
      Math.abs(pointer.x - pointerTarget.x) > 0.001 ||
      Math.abs(pointer.y - pointerTarget.y) > 0.001
    ) {
      rafId = window.requestAnimationFrame(updatePointer);
    }
  }

  function restartAmbientMotion() {
    if (reducedMotion.value) {
      return;
    }

    if (!timers.purpleBlink) {
      scheduleBlink("purple");
    }

    if (!timers.blackBlink) {
      scheduleBlink("black");
    }
  }

  function scheduleBlink(target) {
    const timerKey = target === "purple" ? "purpleBlink" : "blackBlink";
    const resetKey = target === "purple" ? "purpleBlinkReset" : "blackBlinkReset";
    clearTimer(timerKey);
    clearTimer(resetKey);

    timers[timerKey] = schedule(() => {
      if (target === "purple") {
        purpleBlinking.value = true;
      } else {
        blackBlinking.value = true;
      }

      timers[resetKey] = schedule(() => {
        if (target === "purple") {
          purpleBlinking.value = false;
        } else {
          blackBlinking.value = false;
        }

        timers[resetKey] = 0;
        scheduleBlink(target);
      }, BLINK_DURATION);

      timers[timerKey] = 0;
    }, randomInRange(3200, 6800));
  }

  function schedulePurplePeek() {
    clearTimer("peek");
    clearTimer("peekReset");

    timers.peek = schedule(() => {
      purplePeeking.value = true;

      timers.peekReset = schedule(() => {
        purplePeeking.value = false;
        timers.peekReset = 0;

        if (sceneMode.value === "password-visible" && !reducedMotion.value) {
          schedulePurplePeek();
        }
      }, PEEK_DURATION);

      timers.peek = 0;
    }, randomInRange(2200, 4400));
  }

  function startReducedMotionWatcher() {
    motionMediaQuery = window.matchMedia("(prefers-reduced-motion: reduce)");
    reducedMotion.value = motionMediaQuery.matches;

    const handleChange = (event) => {
      reducedMotion.value = event.matches;
    };

    if (typeof motionMediaQuery.addEventListener === "function") {
      motionMediaQuery.addEventListener("change", handleChange);
      removeMotionListener = () => {
        motionMediaQuery.removeEventListener("change", handleChange);
      };
      return;
    }

    motionMediaQuery.addListener(handleChange);
    removeMotionListener = () => {
      motionMediaQuery.removeListener(handleChange);
    };
  }

  function schedule(callback, delay) {
    const id = window.setTimeout(() => {
      timeoutIds.delete(id);
      callback();
    }, delay);

    timeoutIds.add(id);
    return id;
  }

  function clearTimer(key) {
    if (!timers[key]) {
      return;
    }

    window.clearTimeout(timers[key]);
    timeoutIds.delete(timers[key]);
    timers[key] = 0;
  }

  function clearAllTimers() {
    for (const key of Object.keys(timers)) {
      clearTimer(key);
    }
  }

  function cancelAnimationFrameIfNeeded() {
    if (!rafId) {
      return;
    }

    window.cancelAnimationFrame(rafId);
    rafId = 0;
  }

  return {
    panelRef,
    reducedMotion,
    characters
  };
}

function shouldPairLook(sceneMode) {
  return (
    sceneMode === "username-focus" ||
    sceneMode === "password-hidden" ||
    sceneMode === "register-confirm-match" ||
    sceneMode === "register-confirm-mismatch"
  );
}

function resolveCharacterStyle(name, sceneMode, pose) {
  const config = CHARACTER_CONFIG[name];
  const pointerX = pose.x * config.transformPointer.x;
  const pointerY = pose.y * config.transformPointer.y;
  const pointerSkew = pose.x * config.transformPointer.skew;

  let translateX = pointerX;
  let translateY = pointerY;
  let skewX = pointerSkew;

  if (sceneMode === "username-focus") {
    if (name === "purple") {
      translateX += 26;
      translateY -= 10;
      skewX -= 8;
    } else if (name === "black") {
      translateX += 10;
      translateY -= 4;
      skewX -= 3;
    } else if (name === "yellow") {
      translateX += 6;
      translateY -= 3;
    } else if (name === "orange") {
      translateX -= 2;
    }
  }

  if (sceneMode === "password-hidden") {
    if (name === "purple") {
      translateX += 18;
      translateY -= 6;
      skewX -= 4;
    } else if (name === "black") {
      translateX += 8;
      translateY -= 3;
      skewX -= 2;
    } else if (name === "yellow") {
      translateX += 4;
    }
  }

  if (sceneMode === "password-visible") {
    if (name === "purple") {
      translateX += 4;
      translateY -= 2;
      skewX += 1;
    } else if (name === "black") {
      translateY += 1;
    }
  }

  if (sceneMode === "register-confirm-match") {
    translateY -= name === "purple" || name === "black" ? 4 : 3;
  }

  if (sceneMode === "register-confirm-mismatch") {
    if (name === "purple") {
      translateX -= 2;
      translateY += 3;
      skewX -= 2;
    } else if (name === "black") {
      translateY += 2;
    } else {
      translateY += 1;
    }
  }

  if (sceneMode === "submitting") {
    translateY -= name === "purple" || name === "black" ? 8 : 4;
  }

  if (sceneMode === "success") {
    translateY -= name === "purple" || name === "black" ? 12 : 8;
  }

  if (sceneMode === "error") {
    if (name === "purple") {
      translateX -= 4;
      translateY += 2;
    } else if (name === "black") {
      translateY += 2;
    } else {
      translateY += 1;
    }
  }

  return {
    transform: `translate3d(${translateX}px, ${translateY}px, 0) skewX(${skewX}deg)`
  };
}

function resolveFaceStyle(name, sceneMode, pose, pairLookActive) {
  const config = CHARACTER_CONFIG[name];
  const base = resolveFaceAnchor(name, sceneMode, pairLookActive);

  return {
    left: `${base.left + pose.x * config.facePointer.x}px`,
    top: `${base.top + pose.y * config.facePointer.y}px`
  };
}

function resolvePupilStyle(name, sceneMode, pose, pairLookActive, purplePeeking) {
  const config = CHARACTER_CONFIG[name];
  const look = resolveLookVector(name, sceneMode, pose, pairLookActive, purplePeeking);

  return {
    transform: `translate(${look.x}px, ${look.y}px)`
  };
}

function resolveDotStyle(name, sceneMode, pose, pairLookActive) {
  const config = CHARACTER_CONFIG[name];
  const look = resolveLookVector(name, sceneMode, pose, pairLookActive, false);

  return {
    transform: `translate(${clamp(look.x, -config.eye.maxX, config.eye.maxX)}px, ${clamp(
      look.y,
      -config.eye.maxY,
      config.eye.maxY
    )}px)`
  };
}

function resolveMouthStyle(sceneMode, pose) {
  const { mouthBase, mouthPointer } = CHARACTER_CONFIG.yellow;
  let left = mouthBase.left + pose.x * mouthPointer.x;
  let top = mouthBase.top + pose.y * mouthPointer.y;
  let width = 72;

  if (sceneMode === "password-visible") {
    left = 12;
    top = 88;
    width = 62;
  }

  if (sceneMode === "register-confirm-match") {
    top -= 4;
  }

  if (sceneMode === "register-confirm-mismatch") {
    top += 6;
  }

  if (sceneMode === "success") {
    top -= 4;
  }

  if (sceneMode === "error") {
    top += 4;
  }

  return {
    left: `${left}px`,
    top: `${top}px`,
    width: `${width}px`
  };
}

function resolveFaceAnchor(name, sceneMode, pairLookActive) {
  if (pairLookActive && name === "purple") {
    return {
      left: 56,
      top: 60
    };
  }

  if (pairLookActive && name === "black") {
    return {
      left: 31,
      top: 16
    };
  }

  const base = CHARACTER_CONFIG[name].faceBase;

  if (sceneMode === "password-visible") {
    if (name === "purple") {
      return { left: 22, top: 36 };
    }

    if (name === "black") {
      return { left: 12, top: 30 };
    }

    if (name === "orange") {
      return { left: 82, top: 102 };
    }

    if (name === "yellow") {
      return { left: 24, top: 40 };
    }
  }

  if (sceneMode === "username-focus") {
    if (name === "purple") {
      return { left: 58, top: 56 };
    }

    if (name === "black") {
      return { left: 34, top: 38 };
    }
  }

  if (sceneMode === "password-hidden") {
    if (name === "purple") {
      return { left: 56, top: 54 };
    }

    if (name === "black") {
      return { left: 34, top: 36 };
    }
  }

  if (sceneMode === "register-confirm-match") {
    if (name === "purple" || name === "black") {
      return {
        left: base.left,
        top: base.top - 4
      };
    }
  }

  if (sceneMode === "register-confirm-mismatch") {
    if (name === "purple") {
      return { left: base.left, top: base.top + 6 };
    }

    if (name === "black") {
      return { left: base.left, top: base.top + 4 };
    }

    if (name === "yellow") {
      return { left: base.left, top: base.top + 4 };
    }
  }

  if (sceneMode === "success") {
    if (name === "purple" || name === "black") {
      return {
        left: base.left,
        top: base.top - 4
      };
    }
  }

  if (sceneMode === "error") {
    if (name === "purple") {
      return { left: base.left, top: base.top + 6 };
    }

    if (name === "black") {
      return { left: base.left, top: base.top + 4 };
    }
  }

  return base;
}

function resolveLookVector(name, sceneMode, pose, pairLookActive, purplePeeking) {
  if (sceneMode === "idle") {
    return basePointerLook(name, pose);
  }

  if (pairLookActive) {
    if (name === "purple") {
      return { x: 4, y: 4 };
    }

    if (name === "black") {
      return { x: -4, y: -3 };
    }

    return basePointerLook(name, pose, 0.4);
  }

  if (sceneMode === "username-focus") {
    if (name === "purple" || name === "black") {
      return { x: 4, y: -1 };
    }

    if (name === "yellow") {
      return { x: 3, y: 0 };
    }

    return { x: 1, y: 0 };
  }

  if (sceneMode === "password-hidden") {
    if (name === "purple" || name === "black") {
      return { x: 3, y: -2 };
    }

    if (name === "yellow") {
      return { x: 2, y: -1 };
    }

    return { x: 1, y: 0 };
  }

  if (sceneMode === "password-visible") {
    if (name === "purple") {
      return purplePeeking ? { x: 4, y: 4 } : { x: -4, y: -4 };
    }

    if (name === "black") {
      return { x: -4, y: -3 };
    }

    if (name === "orange") {
      return { x: -3, y: -2 };
    }

    return { x: -4, y: -3 };
  }

  if (sceneMode === "register-confirm-match") {
    return { x: 2, y: -1 };
  }

  if (sceneMode === "register-confirm-mismatch") {
    return { x: 0, y: 3 };
  }

  if (sceneMode === "success") {
    return { x: 2, y: -2 };
  }

  if (sceneMode === "error") {
    return { x: 0, y: 2 };
  }

  return { x: 0, y: 0 };
}

function basePointerLook(name, pose, factor = 1) {
  if (name === "purple") {
    return {
      x: clamp(pose.x * 4.2 * factor, -5, 5),
      y: clamp(pose.y * 3.4 * factor, -5, 5)
    };
  }

  if (name === "black") {
    return {
      x: clamp(pose.x * 3.8 * factor, -4, 4),
      y: clamp(pose.y * 3.2 * factor, -4, 4)
    };
  }

  return {
    x: clamp(pose.x * 3.2 * factor, -4, 4),
    y: clamp(pose.y * 2.8 * factor, -4, 4)
  };
}

function randomInRange(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function interpolate(from, to, factor) {
  return from + (to - from) * factor;
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max);
}
