<script setup lang="ts">
import AppLayout from "@/components/AppLayout.vue";
import { useUserStore } from "@/stores/user";
import { useRouter, useRoute } from "vue-router";
import { onMounted } from "vue";

const userStore = useUserStore();
const router = useRouter();
const route = useRoute();

onMounted(() => {
  const token = localStorage.getItem("token");
  if (token) {
    userStore.setToken(token);
    userStore.fetchUserInfo().catch(() => {
      router.push("/login");
    });
  }
});
</script>

<template>
  <router-view />
</template>

<style lang="scss">
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto,
    "Helvetica Neue", Arial, "Noto Sans SC", sans-serif;
}
</style>
