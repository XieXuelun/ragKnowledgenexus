import { createRouter, createWebHashHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import AppLayout from "@/components/AppLayout.vue";

const routes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "Login",
    component: () => import("@/views/login/LoginView.vue"),
    meta: { title: "登录", hidden: true },
  },
  {
    path: "/",
    component: AppLayout,
    redirect: "/dashboard",
    children: [
      {
        path: "dashboard",
        name: "Dashboard",
        component: () => import("@/views/dashboard/DashboardView.vue"),
        meta: { title: "工作台", icon: "Odometer" },
      },
      {
        path: "knowledgeBase",
        name: "KnowledgeBase",
        component: () => import("@/views/knowledgeBase/KBListView.vue"),
        meta: { title: "知识库管理", icon: "FolderOpened" },
      },
      {
        path: "knowledgeBase/:id",
        name: "KBDetail",
        component: () => import("@/views/knowledgeBase/KBDetailView.vue"),
        meta: { title: "知识库详情", hidden: true },
      },
      {
        path: "document",
        name: "Document",
        component: () => import("@/views/document/DocumentView.vue"),
        meta: { title: "文档管理", icon: "Document" },
      },
      {
        path: "qa",
        name: "QA",
        component: () => import("@/views/qa/QAView.vue"),
        meta: { title: "智能问答", icon: "ChatDotRound" },
      },
      {
        path: "ticket",
        name: "Ticket",
        component: () => import("@/views/ticket/TicketView.vue"),
        meta: { title: "工单管理", icon: "List" },
      },
      {
        path: "user",
        name: "UserManage",
        component: () => import("@/views/user/UserManage.vue"),
        meta: { title: "用户管理", icon: "User" },
      },
      {
        path: "department",
        name: "DepartmentManage",
        component: () => import("@/views/user/DepartmentManage.vue"),
        meta: { title: "部门管理", icon: "Tree" },
      },
      {
        path: "profile",
        name: "Profile",
        component: () => import("@/views/profile/ProfileView.vue"),
        meta: { title: "个人中心", hidden: true },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHashHistory(),
  routes,
});

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem("token");
  if (to.name !== "Login" && !token) {
    next({ name: "Login" });
  } else if (to.name === "Login" && token) {
    next({ name: "Dashboard" });
  } else {
    next();
  }
});

export default router;
