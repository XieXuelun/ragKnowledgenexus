<script setup lang="ts">
import { ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { getTicketList, resolveTicket } from "@/api/ticket";
import { Search, View, Check } from "@element-plus/icons-vue";

interface Ticket {
  id: number; title: string; description: string; kbId: number;
  status: number; statusDesc: string; priority: number; priorityDesc: string;
  reply: string; createName: string; createTime: string;
}

const loading = ref(false);
const list = ref<Ticket[]>([]);
const total = ref(0);
const page = ref(1);
const statusFilter = ref<number>();

const allColumns = [
  { key: "title", label: "标题" },
  { key: "statusDesc", label: "状态" },
  { key: "priorityDesc", label: "优先级" },
  { key: "createName", label: "创建人" },
  { key: "createTime", label: "创建时间" },
  { key: "reply", label: "处理结果" },
];
const visibleColumns = ref(allColumns.map((c) => c.key));

const statusOptions = [
  { value: 0, label: "待处理" }, { value: 1, label: "处理中" },
  { value: 2, label: "已完成" }, { value: 3, label: "已关闭" },
];

const resolveDialogVisible = ref(false);
const resolveRow = ref<Ticket | null>(null);
const resolveForm = ref({ reply: "" });

const priorityConfig: Record<number, { label: string; className: string }> = {
  1: { label: "低", className: "vis-tag-private" },
  2: { label: "中", className: "vis-tag-dept" },
  3: { label: "高", className: "vis-tag-public" },
};

async function fetchData() {
  loading.value = true;
  try {
    const res = await getTicketList({ page: page.value, pageSize: 10, status: statusFilter.value });
    list.value = (res as any).data as Ticket[];
    total.value = (res as any).total || 0;
  } finally { loading.value = false; }
}

function openResolveDialog(row: Ticket) {
  resolveRow.value = row;
  resolveForm.value = { reply: "" };
  resolveDialogVisible.value = true;
}

async function handleResolve() {
  if (!resolveRow.value || !resolveForm.value.reply.trim()) {
    ElMessage.warning("请填写处理结果"); return;
  }
  try {
    await ElMessageBox.confirm(`确认解决工单 #${resolveRow.value.id}？`, "确认", { type: "warning" });
  } catch { return; }
  await resolveTicket(resolveRow.value.id, { reply: resolveForm.value.reply, status: 2 });
  ElMessage.success("工单已解决");
  resolveDialogVisible.value = false;
  fetchData();
}

onMounted(fetchData);
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <div>
            <h3 class="page-title">工单管理</h3>
            <p class="page-desc">查看和处理用户提交的工单问题，支持状态筛选和结果回复</p>
          </div>
          <div class="page-actions">
            <el-select v-model="statusFilter" placeholder="工单状态" clearable size="default" style="width:130px" @change="fetchData">
              <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
            <el-popover placement="bottom-end" :width="160" trigger="click">
              <template #reference>
                <el-button size="small" plain><el-icon><View /></el-icon> 列显隐</el-button>
              </template>
              <el-checkbox-group v-model="visibleColumns" class="flex flex-col gap-1.5 px-1 py-1">
                <el-checkbox v-for="col in allColumns" :key="col.key" :label="col.key" size="small">{{ col.label }}</el-checkbox>
              </el-checkbox-group>
            </el-popover>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column type="index" label="#" width="56" align="center" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip v-if="visibleColumns.includes('title')" />
        <el-table-column label="状态" width="90" align="center" v-if="visibleColumns.includes('statusDesc')">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 2 ? 'success' : row.status === 3 ? 'info' : row.status === 1 ? 'warning' : 'info'"
              size="small"
            >
              {{ row.statusDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="80" align="center" v-if="visibleColumns.includes('priorityDesc')">
          <template #default="{ row }">
            <span class="vis-tag" :class="priorityConfig[row.priority]?.className || 'vis-tag-private'">
              {{ priorityConfig[row.priority]?.label || row.priorityDesc }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createName" label="创建人" width="100" v-if="visibleColumns.includes('createName')" />
        <el-table-column prop="createTime" label="创建时间" width="170" v-if="visibleColumns.includes('createTime')" />
        <el-table-column label="处理结果" min-width="160" show-overflow-tooltip v-if="visibleColumns.includes('reply')">
          <template #default="{ row }">
            <span :class="row.reply ? 'text-regular' : 'text-placeholder'">{{ row.reply || "暂未处理" }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status !== 2 && row.status !== 3"
              size="small" class="btn-link-sm" :icon="Check"
              @click="openResolveDialog(row)"
            >解决</el-button>
            <span v-else class="text-placeholder">—</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && list.length === 0" class="empty-state">
        <el-empty :image-size="80" description="暂无工单" />
      </div>

      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" :total="total" :page-size="10" layout="total, prev, pager, next" background @current-change="fetchData" />
      </div>
    </el-card>

    <el-dialog v-model="resolveDialogVisible" title="解决工单" width="500px" destroy-on-close>
      <el-form :model="resolveForm" label-width="80px">
        <el-form-item label="工单标题">
          <span class="text-regular">{{ resolveRow?.title }}</span>
        </el-form-item>
        <el-form-item label="处理结果" required>
          <el-input v-model="resolveForm.reply" type="textarea" :rows="4" placeholder="请填写处理结果或回复" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResolve">确认解决</el-button>
      </template>
    </el-dialog>
  </div>
</template>
