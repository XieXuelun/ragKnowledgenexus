<script setup lang="ts">
import { computed, ref } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useAppStore } from "@/stores/app";
import { useUserStore } from "@/stores/user";
import {
  LayoutDashboard,
  Library,
  FileText,
  MessageSquareMore,
  ClipboardList,
  Users,
  Network,
  User,
  ChevronDown,
  X,
} from "lucide-vue-next";

const router = useRouter();
const route = useRoute();
const appStore = useAppStore();
const userStore = useUserStore();

interface MenuItem {
  name: string;
  path: string;
  title: string;
  icon: typeof LayoutDashboard;
}

const menuItems: MenuItem[] = [
  { name: "Dashboard", path: "/dashboard", title: "工作台", icon: LayoutDashboard },
  { name: "KnowledgeBase", path: "/knowledgeBase", title: "知识库管理", icon: Library },
  { name: "Document", path: "/document", title: "文档管理", icon: FileText },
  { name: "QA", path: "/qa", title: "智能问答", icon: MessageSquareMore },
  { name: "Ticket", path: "/ticket", title: "工单管理", icon: ClipboardList },
  { name: "UserManage", path: "/user", title: "用户管理", icon: Users },
  { name: "DepartmentManage", path: "/department", title: "部门管理", icon: Network },
];

const activeMenu = computed(() => route.name as string);
const displayName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || "用户"
);
const userEmail = computed(() => userStore.userInfo?.email || "");

// ----- Collapsible section state -----
const extraOptionsOpen = ref(false);
const moreInfoOpen = ref(false);

function handleSelect(item: MenuItem) {
  router.push(item.path);
  appStore.closeMobileMenu();
}

function goToProfile() {
  router.push("/profile");
  appStore.closeMobileMenu();
}
</script>

<template>
  <div>
    <!-- ===================== Mobile overlay backdrop ===================== -->
    <Transition name="fade">
      <div
        v-if="appStore.mobileMenuOpen"
        class="md:hidden fixed inset-0 z-40 bg-black/50"
        @click="appStore.closeMobileMenu"
      />
    </Transition>

    <!-- ===================== Mobile slide-over sidebar ===================== -->
    <Transition name="slide-left">
      <aside
        v-if="appStore.mobileMenuOpen"
        class="md:hidden fixed inset-y-0 left-0 z-50 w-72 bg-white shadow-xl flex flex-col"
      >
        <!-- Mobile sidebar header -->
        <div class="flex items-center justify-between px-5 py-4 border-b border-gray-100">
          <div class="flex items-center gap-2.5" @click="goToProfile">
            <el-avatar :size="40" :src="userStore.userInfo?.avatarUrl">
              <User class="h-5 w-5 text-blue-600" />
            </el-avatar>
            <div class="min-w-0">
              <p class="font-semibold text-sm text-gray-900 truncate">{{ displayName }}</p>
              <p class="text-xs text-gray-400 truncate">{{ userEmail }}</p>
            </div>
          </div>
          <button
            class="p-1.5 rounded-lg hover:bg-gray-100 text-gray-400 hover:text-gray-600 transition-colors"
            @click="appStore.closeMobileMenu"
          >
            <X :size="20" />
          </button>
        </div>

        <!-- Mobile nav -->
        <nav class="flex-1 overflow-y-auto px-3 py-4">
          <ul class="space-y-1">
            <li v-for="item in menuItems" :key="item.name">
              <button
                class="flex items-center gap-3 w-full py-2.5 px-3 rounded-xl text-sm font-medium transition-colors"
                :class="
                  activeMenu === item.name
                    ? 'bg-blue-50 text-blue-600'
                    : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                "
                @click="handleSelect(item)"
              >
                <component :is="item.icon" :size="20" />
                {{ item.title }}
              </button>
            </li>
          </ul>

          <!-- Collapsible sections -->
          <div class="mt-6 px-3">
            <button
              class="flex items-center justify-between w-full py-2 text-sm font-semibold text-gray-500 hover:text-gray-700 transition-colors"
              @click="extraOptionsOpen = !extraOptionsOpen"
            >
              <span>更多选项</span>
              <ChevronDown
                :size="16"
                class="transition-transform duration-200"
                :class="{ 'rotate-180': extraOptionsOpen }"
              />
            </button>
            <div
              v-show="extraOptionsOpen"
              class="mt-1 ml-2 space-y-1 border-l-2 border-gray-100 pl-3"
            >
              <button class="w-full text-left py-1.5 px-2 text-sm text-gray-500 rounded-lg hover:bg-gray-50 hover:text-gray-700 transition-colors">
                订阅管理
              </button>
              <button class="w-full text-left py-1.5 px-2 text-sm text-gray-500 rounded-lg hover:bg-gray-50 hover:text-gray-700 transition-colors">
                外观设置
              </button>
            </div>

            <button
              class="flex items-center justify-between w-full py-2 mt-2 text-sm font-semibold text-gray-500 hover:text-gray-700 transition-colors"
              @click="moreInfoOpen = !moreInfoOpen"
            >
              <span>更多信息</span>
              <ChevronDown
                :size="16"
                class="transition-transform duration-200"
                :class="{ 'rotate-180': moreInfoOpen }"
              />
            </button>
            <div v-show="moreInfoOpen" class="mt-1 ml-2 border-l-2 border-gray-100 pl-3">
              <p class="text-xs text-gray-400 py-1.5 px-2">
                在这里可以找到更多详细信息和设置选项。
              </p>
            </div>
          </div>
        </nav>

        <!-- Mobile sidebar footer -->
        <div class="px-4 py-3 border-t border-gray-100">
          <button
            class="w-full py-2.5 text-sm font-medium text-center rounded-xl bg-blue-50 text-blue-600 hover:bg-blue-100 transition-colors"
            @click="goToProfile"
          >
            查看个人资料
          </button>
        </div>
      </aside>
    </Transition>

    <!-- ===================== Desktop sidebar ===================== -->
    <aside
      class="hidden md:flex flex-col fixed top-0 left-0 h-full bg-white border-r border-gray-100 z-30 transition-all duration-300 shadow-sm"
      :class="appStore.sidebarCollapsed ? 'w-16' : 'w-60'"
    >
      <!-- Logo area (click to home) -->
      <div
        class="flex items-center h-14 px-4 border-b border-gray-100 cursor-pointer gap-3 overflow-hidden"
        @click="router.push('/dashboard')"
      >
        <div class="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center flex-shrink-0">
          <span class="text-white font-bold text-sm">R</span>
        </div>
        <span
          class="font-semibold text-sm text-gray-900 whitespace-nowrap transition-opacity duration-300"
          :class="appStore.sidebarCollapsed ? 'opacity-0 w-0' : 'opacity-100'"
        >
          KnowledgeNexus
        </span>
      </div>

      <!-- User profile (desktop, expanded only) -->
      <div
        class="px-4 py-3 border-b border-gray-100 overflow-hidden transition-all duration-300"
        :class="appStore.sidebarCollapsed ? 'py-3 px-0 flex justify-center' : ''"
      >
        <div
          v-if="!appStore.sidebarCollapsed"
          class="flex items-center gap-2.5 cursor-pointer"
          @click="goToProfile"
        >
          <el-avatar :size="36" :src="userStore.userInfo?.avatarUrl" class="flex-shrink-0">
            <User class="h-4 w-4 text-blue-600" />
          </el-avatar>
          <div class="min-w-0">
            <p class="font-semibold text-xs text-gray-900 truncate">{{ displayName }}</p>
            <p class="text-xs text-gray-400 truncate">{{ userEmail }}</p>
          </div>
        </div>
        <div
          v-else
          class="flex justify-center cursor-pointer"
          @click="goToProfile"
        >
          <el-avatar :size="36" :src="userStore.userInfo?.avatarUrl">
            <User class="h-4 w-4 text-blue-600" />
          </el-avatar>
        </div>
      </div>

      <!-- Navigation -->
      <nav class="flex-1 overflow-y-auto px-2 py-3">
        <ul class="space-y-0.5">
          <li v-for="item in menuItems" :key="item.name">
            <button
              class="flex items-center gap-3 w-full py-2.5 px-3 rounded-xl text-sm font-medium transition-colors overflow-hidden"
              :class="[
                activeMenu === item.name
                  ? 'bg-blue-50 text-blue-600'
                  : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900',
                appStore.sidebarCollapsed ? 'justify-center px-0' : '',
              ]"
              :title="appStore.sidebarCollapsed ? item.title : ''"
              @click="handleSelect(item)"
            >
              <component :is="item.icon" :size="20" class="flex-shrink-0" />
              <span
                class="whitespace-nowrap transition-opacity duration-300"
                :class="appStore.sidebarCollapsed ? 'opacity-0 w-0 hidden' : 'opacity-100'"
              >
                {{ item.title }}
              </span>
            </button>
          </li>
        </ul>
      </nav>

      <!-- Desktop sidebar footer -->
      <div
        class="px-3 py-3 border-t border-gray-100 overflow-hidden transition-all duration-300"
        :class="appStore.sidebarCollapsed ? 'px-1' : ''"
      >
        <button
          class="w-full py-2.5 text-sm font-medium text-center rounded-xl bg-blue-50 text-blue-600 hover:bg-blue-100 transition-colors whitespace-nowrap overflow-hidden"
          :class="appStore.sidebarCollapsed ? 'text-xs px-1' : ''"
          @click="goToProfile"
        >
          {{ appStore.sidebarCollapsed ? "资料" : "查看个人资料" }}
        </button>
      </div>
    </aside>
  </div>
</template>


<style scoped>
/* ----- Transition: fade (backdrop) ----- */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ----- Transition: slide-left (mobile sidebar) ----- */
.slide-left-enter-active,
.slide-left-leave-active {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.slide-left-enter-from,
.slide-left-leave-to {
  transform: translateX(-100%);
}
</style>
