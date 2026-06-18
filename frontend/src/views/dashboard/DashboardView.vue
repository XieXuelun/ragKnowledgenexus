<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import { getKBList, getDocumentList } from "@/api";
import type { DocumentItem } from "@/types/knowledgeBase";
import VChart from "vue-echarts";
import "echarts";

const userStore = useUserStore();
const router = useRouter();

const kbCount = ref(0);
const docCount = ref(0);
const qaCount = ref(0);
const recentDocs = ref<DocumentItem[]>([]);
const loading = ref(true);

const chartOption = ref({
  tooltip: { trigger: "axis" },
  xAxis: {
    type: "category",
    data: ["周一", "周二", "周三", "周四", "周五", "周六", "周日"],
    axisLine: { lineStyle: { color: "#e5e7eb" } },
  },
  yAxis: { type: "value", splitLine: { lineStyle: { color: "#f3f4f6" } } },
  series: [
    {
      data: [12, 18, 8, 24, 16, 10, 6],
      type: "line",
      smooth: true,
      lineStyle: { color: "#2563eb", width: 2 },
      areaStyle: { color: "rgba(37, 99, 235, 0.08)" },
      symbol: "circle",
      symbolSize: 6,
    },
  ],
  grid: { top: 20, bottom: 20, left: 40, right: 20 },
});

const parseStatusMap: Record<number, { text: string; type: "success" | "warning" | "danger" | "info" }> = {
  0: { text: "待处理", type: "info" },
  1: { text: "解析中", type: "warning" },
  2: { text: "已完成", type: "success" },
  3: { text: "失败", type: "danger" },
};

onMounted(async () => {
  try {
    const [kbRes, docRes] = await Promise.all([
      getKBList({ page: 1, pageSize: 1 }),
      getDocumentList({ page: 1, pageSize: 5 }),
    ]);
    kbCount.value = (kbRes as any).total || 0;
    docCount.value = (docRes as any).total || 0;
    recentDocs.value = (docRes as any).data || [];
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="dashboard">
    <!-- Title -->
    <div class="page-title">欢迎回来，{{ userStore.userInfo?.nickname || "用户" }}</div>
    <p class="page-desc" style="margin-top:-2px;margin-bottom:20px">以下是系统概览数据</p>

    <!-- Stat cards -->
    <div class="stat-cards">
      <el-card class="stat-card" shadow="never" @click="router.push('/knowledgeBase')">
        <div class="stat-value">{{ kbCount }}</div>
        <div class="stat-label">知识库</div>
      </el-card>
      <el-card class="stat-card" shadow="never" @click="router.push('/document')">
        <div class="stat-value">{{ docCount }}</div>
        <div class="stat-label">文档数</div>
      </el-card>
      <el-card class="stat-card" shadow="never" @click="router.push('/qa')">
        <div class="stat-value">{{ qaCount || "-" }}</div>
        <div class="stat-label">问答次数</div>
      </el-card>
      <el-card class="stat-card accent" shadow="never" @click="router.push('/qa')">
        <div class="stat-value">去提问</div>
        <div class="stat-label">智能问答</div>
      </el-card>
    </div>

    <!-- Chart + Recent docs -->
    <div class="dashboard-grid">
      <el-card class="chart-card" shadow="never">
        <template #header><span class="card-title">近7日问答趋势</span></template>
        <VChart :option="chartOption" style="height: 260px" />
      </el-card>
      <el-card class="recent-card" shadow="never">
        <template #header><span class="card-title">最近文档</span></template>
        <div v-if="recentDocs.length === 0" class="empty-hint">暂无文档</div>
        <div v-for="doc in recentDocs" :key="doc.id" class="recent-item">
          <span class="doc-name">{{ doc.fileName }}</span>
          <el-tag :type="parseStatusMap[doc.parseStatus]?.type" size="small">
            {{ parseStatusMap[doc.parseStatus]?.text || "未知" }}
          </el-tag>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.dashboard {
  max-width: 1200px;
}

.card-title {
  font-weight: 600;
  font-size: 14px;
  color: #1f2937;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;

  @media (max-width: 900px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.stat-card {
  cursor: pointer;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  transition: transform 0.2s, box-shadow 0.2s;
  &:hover {
    transform: translateY(-2px);
    box-shadow: $shadow-md;
  }
  &.accent {
    background: #1e293b;
    color: #fff;
    .stat-label { color: rgba(255, 255, 255, 0.7); }
  }
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: $text-secondary;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 16px;

  @media (max-width: 900px) {
    grid-template-columns: 1fr;
  }
}

.recent-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid $border-light;
  &:last-child { border-bottom: none; }
}

.doc-name {
  font-size: 13px;
  color: $text-primary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  margin-right: 12px;
}

.empty-hint {
  text-align: center;
  color: $text-placeholder;
  padding: 24px 0;
  font-size: 13px;
}
</style>
