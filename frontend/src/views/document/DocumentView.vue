<script setup lang="ts">
import { ref, onMounted } from "vue";
import { getDocumentList, uploadDocument, deleteDocument } from "@/api/document";
import { getKBList } from "@/api/knowledgeBase";
import { ElMessage, ElMessageBox } from "element-plus";
import { Upload, Delete, Search, View } from "@element-plus/icons-vue";
import type { KnowledgeBase } from "@/types/knowledgeBase";

const loading = ref(false);
const list = ref<any[]>([]);
const total = ref(0);
const page = ref(1);
const keyword = ref("");
const filterKbId = ref<number | undefined>();
const kbOptions = ref<{ id: number; name: string }[]>([]);

const allColumns = [
  { key: "fileName", label: "文件名" },
  { key: "knowledgeName", label: "所属知识库" },
  { key: "fileType", label: "类型" },
  { key: "fileSize", label: "大小" },
  { key: "parseStatus", label: "解析状态" },
  { key: "chunkCount", label: "片段数" },
  { key: "createTime", label: "上传时间" },
];
const visibleColumns = ref(allColumns.map((c) => c.key));

const parseStatusMap: Record<number, { text: string; type: "success" | "warning" | "danger" | "info" }> = {
  0: { text: "待处理", type: "info" },
  1: { text: "解析中", type: "warning" },
  2: { text: "已完成", type: "success" },
  3: { text: "失败", type: "danger" },
};

async function fetchData() {
  loading.value = true;
  try {
    const res = await getDocumentList({
      page: page.value, pageSize: 10,
      keyword: keyword.value, kbId: filterKbId.value,
    });
    const rawData = (res as any).data as any[] || [];
    const nameCount: Record<string, number> = {};
    rawData.forEach((d: any) => { nameCount[d.fileName] = (nameCount[d.fileName] || 0) + 1; });
    const nameIdx: Record<string, number> = {};
    list.value = rawData.map((d: any) => {
      const key = d.fileName;
      nameIdx[key] = (nameIdx[key] || 0) + 1;
      return { ...d, _version: nameCount[key] > 1 ? `v${nameIdx[key]}` : null, _hasDup: nameCount[key] > 1 };
    });
    total.value = (res as any).total || 0;
  } finally { loading.value = false; }
}

async function handleUpload() {
  const input = document.createElement("input");
  input.type = "file";
  input.accept = ".pdf,.docx,.txt";
  input.onchange = async () => {
    const file = input.files?.[0];
    if (!file) return;
    if (!filterKbId.value) { ElMessage.warning("请先筛选或选择一个知识库"); return; }
    await uploadDocument(filterKbId.value, file);
    ElMessage.success("上传成功");
    fetchData();
  };
  input.click();
}

async function handleDelete(row: any) {
  try { await ElMessageBox.confirm(`确定删除「${row.fileName}」？`, "确认", { type: "warning" }); }
  catch { return; }
  await deleteDocument(row.id);
  ElMessage.success("已删除");
  fetchData();
}

function formatSize(bytes: number) {
  if (!bytes) return "-";
  if (bytes < 1024) return bytes + " B";
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
  return (bytes / 1024 / 1024).toFixed(1) + " MB";
}

onMounted(async () => {
  try {
    const kbRes = await getKBList({ page: 1, pageSize: 100 });
    kbOptions.value = ((kbRes as any).data || []).map((k: KnowledgeBase) => ({ id: k.id, name: k.name }));
  } catch {}
  fetchData();
});
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <div>
            <h3 class="page-title">文档管理</h3>
            <p class="page-desc">管理知识库中的文档，支持上传、删除和查看解析状态</p>
          </div>
          <div class="page-actions">
            <el-select v-model="filterKbId" placeholder="知识库" clearable size="default" style="width:140px" @change="fetchData">
              <el-option v-for="kb in kbOptions" :key="kb.id" :label="kb.name" :value="kb.id" />
            </el-select>
            <el-input v-model="keyword" placeholder="搜索文件" clearable size="default" style="width:180px" @clear="fetchData" @keyup.enter="fetchData">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-popover placement="bottom-end" :width="160" trigger="click">
              <template #reference>
                <el-button size="small" plain><el-icon><View /></el-icon> 列显隐</el-button>
              </template>
              <el-checkbox-group v-model="visibleColumns" class="flex flex-col gap-1.5 px-1 py-1">
                <el-checkbox v-for="col in allColumns" :key="col.key" :label="col.key" size="small">{{ col.label }}</el-checkbox>
              </el-checkbox-group>
            </el-popover>
            <el-button size="small" :icon="Upload" class="btn-dark" @click="handleUpload">上传文档</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column type="index" label="#" width="56" align="center" />
        <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip v-if="visibleColumns.includes('fileName')">
          <template #default="{ row }">
            <div class="file-cell">
              <span class="file-name">{{ row.fileName }}</span>
              <el-tag v-if="row._hasDup && row._version" size="small" class="version-tag">{{ row._version }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="knowledgeName" label="所属知识库" min-width="120" show-overflow-tooltip v-if="visibleColumns.includes('knowledgeName')" />
        <el-table-column prop="fileType" label="类型" width="70" align="center" v-if="visibleColumns.includes('fileType')" />
        <el-table-column label="大小" width="90" align="center" v-if="visibleColumns.includes('fileSize')">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="解析状态" width="90" align="center" v-if="visibleColumns.includes('parseStatus')">
          <template #default="{ row }">
            <el-tag :type="parseStatusMap[row.parseStatus]?.type" size="small">
              {{ parseStatusMap[row.parseStatus]?.text || "未知" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="片段数" width="72" align="center" v-if="visibleColumns.includes('chunkCount')" />
        <el-table-column prop="createTime" label="上传时间" width="170" v-if="visibleColumns.includes('createTime')" />
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" class="btn-danger-sm" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && list.length === 0" class="empty-state">
        <el-empty :image-size="80" description="暂无文档" />
      </div>

      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" :total="total" :page-size="10" layout="total, prev, pager, next" background @current-change="fetchData" />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.file-cell { display: flex; align-items: center; gap: 6px; }
.file-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.version-tag { font-size: 11px; padding: 0 5px; height: 18px; line-height: 18px; border-radius: 4px; background: #e6f4ff; color: #1677ff; border: none; }
</style>
