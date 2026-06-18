<script setup lang="ts">
import { useRouter } from "vue-router";
import { computed } from "vue";
import { useUserStore } from "@/stores/user";
import { useAppStore } from "@/stores/app";
import { Expand, Fold } from "@element-plus/icons-vue";
import { Menu, X } from "lucide-vue-next";

const router = useRouter();
const userStore = useUserStore();
const appStore = useAppStore();

const displayName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || "用户");

async function handleLogout() {
  await userStore.logout();
  router.push("/login");
}

function goToProfile() {
  router.push("/profile");
}
</script>

<template>
  <div class="navbar">
    <div class="navbar-left">
      <!-- Mobile hamburger toggle -->
      <button
        class="md:hidden p-1.5 -ml-1 rounded-lg hover:bg-gray-100 text-gray-500 hover:text-gray-700 transition-colors"
        @click="appStore.toggleMobileMenu"
      >
        <Transition name="icon-swap" mode="out-in">
          <X v-if="appStore.mobileMenuOpen" :size="22" />
          <Menu v-else :size="22" />
        </Transition>
      </button>

      <!-- Desktop collapse toggle -->
      <el-icon class="collapse-btn hidden md:flex" @click="appStore.toggleSidebar" :size="20">
        <Fold v-if="!appStore.sidebarCollapsed" />
        <Expand v-else />
      </el-icon>
    </div>
    <div class="navbar-right">
      <el-dropdown trigger="click">
        <span class="user-info">
          <el-avatar :size="32" :src="userStore.userInfo?.avatarUrl">
            {{ displayName.charAt(0).toUpperCase() }}
          </el-avatar>
          <span class="user-name">{{ displayName }}</span>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="goToProfile">个人中心</el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.navbar {
  height: $navbar-height;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: $bg-white;
  border-bottom: 1px solid $border-light;
  flex-shrink: 0;
}

.navbar-left {
  display: flex;
  align-items: center;
}

.collapse-btn {
  cursor: pointer;
  color: $text-secondary;
  &:hover {
    color: $primary;
  }
}

.navbar-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.user-name {
  font-size: 13px;
  color: $text-primary;
}

/* Icon swap transition */
.icon-swap-enter-active,
.icon-swap-leave-active {
  transition: all 0.15s ease;
}
.icon-swap-enter-from {
  opacity: 0;
  transform: scale(0.8) rotate(-90deg);
}
.icon-swap-leave-to {
  opacity: 0;
  transform: scale(0.8) rotate(90deg);
}
</style>
