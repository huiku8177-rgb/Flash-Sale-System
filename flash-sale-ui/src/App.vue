<script setup>
import { onBeforeUnmount, onMounted, provide, reactive } from "vue";
import { useMallApp } from "./composables/useMallApp";

const mallApp = reactive(useMallApp());

provide("mallApp", mallApp);

onMounted(async () => {
  await mallApp.init();
});

onBeforeUnmount(() => {
  mallApp.dispose();
});
</script>

<template>
  <router-view />
</template>
