<script setup lang="ts">
import Sidebar from "./Sidebar.vue";
import Navbar from "./Navbar.vue";
import TagsView from "./TagsView.vue";
import { useAppStore } from "@/stores/app";

const appStore = useAppStore();
</script>

<template>
  <div class="app-layout" :class="{ collapsed: appStore.sidebarCollapsed }">
    <Sidebar />
    <div class="app-main">
      <Navbar />
      <TagsView />
      <div class="app-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.app-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.app-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-left: $sidebar-width;
  transition: margin-left $transition-base;
  min-width: 0;

  @media (max-width: 767px) {
    margin-left: 0 !important;
  }
}

.collapsed .app-main {
  margin-left: $sidebar-collapsed-width;
}

.app-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: $bg-page;
}

/* Page transition */
.fade-page-enter-active,
.fade-page-leave-active {
  transition: opacity 0.2s ease;
}
.fade-page-enter-from,
.fade-page-leave-to {
  opacity: 0;
}
</style>
