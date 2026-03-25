<script setup>
import { toRef } from "vue";
import { useAuthScene } from "../../composables/useAuthScene";

const labels = {
  privacy: "\u9690\u79c1\u653f\u7b56",
  terms: "\u670d\u52a1\u6761\u6b3e"
};

const props = defineProps({
  input: {
    type: Object,
    required: true
  }
});

const { panelRef, reducedMotion, characters } = useAuthScene(toRef(props, "input"));
</script>

<template>
  <section ref="panelRef" class="showcase-panel" :class="{ 'is-reduced-motion': reducedMotion }">
    <div class="showcase-grid"></div>
    <div class="showcase-glow showcase-glow--top"></div>
    <div class="showcase-glow showcase-glow--bottom"></div>

    <div class="showcase-brand">
      <span class="showcase-brand__mark">
        <span class="showcase-brand__icon">
          <span class="showcase-brand__needle"></span>
        </span>
      </span>
      <span class="showcase-brand__text">FlashSale</span>
    </div>

    <div class="scene-art">
      <div class="scene-art__shadow"></div>

      <div class="character character--purple" :style="characters.purple.wrapperStyle">
        <div class="character__body">
          <div class="character__face character__face--purple" :style="characters.purple.faceStyle">
            <span class="character__eye" :class="{ 'is-blinking': characters.purple.blinking }">
              <span class="character__pupil" :style="characters.purple.pupilStyle"></span>
            </span>
            <span class="character__eye" :class="{ 'is-blinking': characters.purple.blinking }">
              <span class="character__pupil" :style="characters.purple.pupilStyle"></span>
            </span>
          </div>
        </div>
      </div>

      <div class="character character--orange" :style="characters.orange.wrapperStyle">
        <div class="character__body">
          <div class="character__face character__face--orange" :style="characters.orange.faceStyle">
            <span class="character__dot-eye" :style="characters.orange.dotStyle"></span>
            <span class="character__dot-eye" :style="characters.orange.dotStyle"></span>
          </div>
        </div>
      </div>

      <div class="character character--black" :style="characters.black.wrapperStyle">
        <div class="character__body">
          <div class="character__face character__face--black" :style="characters.black.faceStyle">
            <span class="character__eye" :class="{ 'is-blinking': characters.black.blinking }">
              <span class="character__pupil" :style="characters.black.pupilStyle"></span>
            </span>
            <span class="character__eye" :class="{ 'is-blinking': characters.black.blinking }">
              <span class="character__pupil" :style="characters.black.pupilStyle"></span>
            </span>
          </div>
        </div>
      </div>

      <div class="character character--yellow" :style="characters.yellow.wrapperStyle">
        <div class="character__body">
          <div class="character__face character__face--yellow" :style="characters.yellow.faceStyle">
            <span class="character__dot-eye" :style="characters.yellow.dotStyle"></span>
            <span class="character__dot-eye" :style="characters.yellow.dotStyle"></span>
          </div>
          <span class="character__mouth" :style="characters.yellow.mouthStyle"></span>
        </div>
      </div>
    </div>

    <div class="showcase-links">
      <a href="#" @click.prevent>{{ labels.privacy }}</a>
      <a href="#" @click.prevent>{{ labels.terms }}</a>
    </div>
  </section>
</template>

<style scoped>
.showcase-panel {
  position: relative;
  overflow: hidden;
  min-height: 100vh;
  background: radial-gradient(circle at 28% 18%, #b4bcc7 0, #8b95a3 34%, #6f7888 68%, #60697a 100%);
}

.showcase-grid,
.showcase-glow {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.showcase-grid {
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  background-size: 120px 120px;
  mask-image: radial-gradient(circle at center, rgba(0, 0, 0, 0.72), transparent 92%);
}

.showcase-glow {
  filter: blur(72px);
  opacity: 0.34;
}

.showcase-glow--top {
  inset: 5% 18% auto;
  height: 160px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.55) 0, transparent 70%);
}

.showcase-glow--bottom {
  inset: auto 10% 10%;
  height: 220px;
  background: radial-gradient(circle, rgba(99, 113, 135, 0.42) 0, transparent 72%);
}

.showcase-brand {
  position: absolute;
  top: 26px;
  left: 26px;
  z-index: 3;
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: #ffffff;
  font-weight: 700;
}

.showcase-brand__mark {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(12px);
}

.showcase-brand__icon {
  position: relative;
  width: 23px;
  height: 23px;
  border-radius: 50%;
  background: #111827;
}

.showcase-brand__icon::before {
  content: "";
  position: absolute;
  inset: 6px;
  border: 2px solid #ffffff;
  border-radius: 50%;
}

.showcase-brand__needle {
  position: absolute;
  left: 10px;
  top: 4px;
  width: 2px;
  height: 14px;
  border-radius: 999px;
  background: #ffffff;
  transform: rotate(40deg);
  transform-origin: center center;
}

.showcase-brand__text {
  font-size: 20px;
  letter-spacing: -0.03em;
}

.scene-art {
  position: absolute;
  left: 50%;
  bottom: 192px;
  width: 550px;
  height: 400px;
  transform: translateX(-50%) scale(0.84);
  transform-origin: bottom center;
}

.scene-art__shadow {
  position: absolute;
  left: 12%;
  right: 14%;
  bottom: -10px;
  height: 34px;
  border-radius: 999px;
  background: rgba(36, 45, 59, 0.2);
  filter: blur(18px);
}

.character {
  position: absolute;
  bottom: 0;
  transform-origin: center bottom;
  transition: transform 0.45s cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform;
}

.character__body,
.character__face,
.character__mouth,
.character__eye,
.character__pupil,
.character__dot-eye {
  transition:
    transform 0.2s ease-out,
    top 0.24s ease-out,
    left 0.24s ease-out,
    width 0.24s ease-out;
  will-change: transform, top, left;
}

.character__body {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.character__face {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  width: max-content;
}

.character__eye {
  position: relative;
  width: 18px;
  height: 18px;
  overflow: hidden;
  border-radius: 999px;
  background: #ffffff;
  flex: 0 0 auto;
}

.character__eye.is-blinking {
  transform: scaleY(0.12);
}

.character__pupil {
  position: absolute;
  top: 6px;
  left: 6px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #2f2d2d;
}

.character__dot-eye {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #2f2d2d;
  flex: 0 0 auto;
}

.character__mouth {
  position: absolute;
  height: 5px;
  border-radius: 999px;
  background: #2f2d2d;
}

.character--purple {
  left: 70px;
  width: 180px;
  height: 400px;
  z-index: 1;
}

.character--purple .character__body {
  background: #6c3ff5;
  border-radius: 10px 10px 0 0;
}

.character__face--purple {
  gap: 18px;
}

.character--orange {
  left: 0;
  width: 240px;
  height: 200px;
  z-index: 3;
}

.character--orange .character__body {
  background: #ff9b6b;
  border-radius: 120px 120px 0 0;
}

.character__face--orange {
  gap: 18px;
}

.character--black {
  left: 240px;
  width: 120px;
  height: 310px;
  z-index: 2;
}

.character--black .character__body {
  background: #2f2d2d;
  border-radius: 8px 8px 0 0;
}

.character__face--black {
  gap: 18px;
}

.character--yellow {
  left: 310px;
  width: 140px;
  height: 230px;
  z-index: 4;
}

.character--yellow .character__body {
  background: #e8d754;
  border-radius: 70px 70px 0 0;
}

.character__face--yellow {
  gap: 18px;
}

.character__face--black .character__eye {
  width: 16px;
  height: 16px;
}

.character__face--black .character__pupil {
  top: 5px;
  left: 5px;
  width: 6px;
  height: 6px;
}

.showcase-links {
  position: absolute;
  left: 26px;
  bottom: 24px;
  z-index: 3;
  display: inline-flex;
  gap: 30px;
}

.showcase-links a {
  color: rgba(31, 41, 55, 0.42);
  font-size: 14px;
  text-decoration: none;
}

.showcase-links a:hover {
  color: rgba(17, 24, 39, 0.62);
}

.showcase-panel.is-reduced-motion .character,
.showcase-panel.is-reduced-motion .character__body,
.showcase-panel.is-reduced-motion .character__face,
.showcase-panel.is-reduced-motion .character__mouth,
.showcase-panel.is-reduced-motion .character__eye,
.showcase-panel.is-reduced-motion .character__pupil,
.showcase-panel.is-reduced-motion .character__dot-eye {
  transition: none;
}

@media (max-width: 1120px) {
  .showcase-panel {
    min-height: 520px;
  }

  .scene-art {
    bottom: 150px;
    transform: translateX(-50%) scale(0.76);
  }
}

@media (max-width: 720px) {
  .showcase-panel {
    min-height: 430px;
  }

  .showcase-brand {
    top: 18px;
    left: 18px;
  }

  .showcase-brand__mark {
    width: 38px;
    height: 38px;
  }

  .showcase-brand__text {
    font-size: 17px;
  }

  .scene-art {
    bottom: 112px;
    transform: translateX(-50%) scale(0.62);
  }

  .showcase-links {
    left: 18px;
    bottom: 16px;
    gap: 18px;
  }

  .showcase-links a {
    font-size: 12px;
  }
}
</style>
