<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getDocumentsByKbId, uploadDocument, deleteDocument, getDocumentPreviewUrl, reparseDocument } from "@/api/document";
import { getKBDetail } from "@/api/knowledgeBase";
import { ElMessage, ElMessageBox } from "element-plus";
import { Upload, Delete, ArrowLeft, Refresh } from "@element-plus/icons-vue";

const route = useRoute();
const router = useRouter();
const kbId = route.params.id as string;

const kbName = ref("");
const loading = ref(false);
const list = ref<any[]>([]);
let pollTimer: ReturnType<typeof setInterval> | null = null;

const parseStatusMap: Record<number, { text: string; className: string }> = {
  0: { text: "待处理", className: "ps-pending" },
  1: { text: "解析中", className: "ps-parsing" },
  2: { text: "已完成", className: "ps-done" },
  3: { text: "失败", className: "ps-failed" },
};

const previewVisible = ref(false);
const previewUrl = ref("");

function stopPoll() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
}

function startPoll() {
  stopPoll();
  // Poll every 2s for up to 60s to catch parse status changes
  let elapsed = 0;
  pollTimer = setInterval(() => {
    elapsed += 2;
    if (elapsed > 60) { stopPoll(); return; }
    // Only keep polling while there are docs in pending/parsing status
    const hasPending = list.value.some((d: any) => d.parseStatus === 0 || d.parseStatus === 1);
    if (!hasPending) { stopPoll(); return; }
    fetchDocuments();
  }, 2000);
}

async function fetchKBInfo() {
  try {
    const res = await getKBDetail(kbId);
    kbName.value = (res as any).data?.name || `知识库 #${kbId}`;
  } catch {
    kbName.value = `知识库 #${kbId}`;
  }
}

async function fetchDocuments() {
  loading.value = true;
  try {
    const res: any = await getDocumentsByKbId(kbId);
    list.value = res.data || [];
  } catch {
    list.value = [];
  } finally {
    loading.value = false;
  }
}

async function handleUpload() {
  const input = document.createElement("input");
  input.type = "file";
  input.accept = ".pdf,.docx,.txt,.md,.xlsx,.pptx";
  input.onchange = async () => {
    const file = input.files?.[0];
    if (!file) return;
    try {
      await uploadDocument(kbId, file);
      ElMessage.success("上传成功，正在解析...");
      await fetchDocuments();
      startPoll();
    } catch { /* handled */ }
  };
  input.click();
}

async function handleReparse(row: any) {
  try {
    await reparseDocument(row.id);
    ElMessage.success("已触发重新解析");
    fetchDocuments();
    startPoll();
  } catch { /* handled */ }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.fileName}」？`, "确认删除", { type: "warning" });
  } catch {
    return;
  }
  try {
    await deleteDocument(row.id);
    ElMessage.success("已删除");
    stopPoll();
    fetchDocuments();
  } catch {
    ElMessage.error("删除失败，请稍后重试");
  }
}

async function handlePreview(row: any) {
  try {
    const res: any = await getDocumentPreviewUrl(row.id);
    previewUrl.value = res.data?.previewUrl || "";
    previewVisible.value = true;
  } catch {
    ElMessage.error("获取预览地址失败");
  }
}

function formatSize(bytes: number) {
  if (!bytes) return "—";
  if (bytes < 1024) return bytes + " B";
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
  return (bytes / 1024 / 1024).toFixed(1) + " MB";
}

function goBack() {
  router.push("/knowledgeBase");
}

onMounted(() => {
  fetchKBInfo();
  fetchDocuments();
});

onUnmounted(() => {
  stopPoll();
});
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <div>
            <div class="back-row">
              <button class="back-btn" @click="goBack">
                <el-icon><ArrowLeft /></el-icon>
                返回列表
              </button>
            </div>
            <h3 class="page-title">{{ kbName }}</h3>
            <p class="page-desc">文档列表 · 共 {{ list.length }} 个文档</p>
          </div>
          <div class="page-actions">
            <el-button size="small" :icon="Upload" class="btn-dark" @click="handleUpload">
              上传文档
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" stripe class="doc-table" @row-click="handlePreview">
        <el-table-column type="index" label="#" width="56" align="center" />
        <el-table-column prop="fileName" label="文件名" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="kb-link">{{ row.fileName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="fileType" label="类型" width="80" align="center">
          <template #default="{ row }">
            <span class="file-type-badge">{{ row.fileType }}</span>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="100" align="center">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="解析状态" width="100" align="center">
          <template #default="{ row }">
            <span class="ps-tag" :class="parseStatusMap[row.parseStatus]?.className || 'ps-pending'">
              {{ parseStatusMap[row.parseStatus]?.text || "—" }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="片段数" width="72" align="center" />
        <el-table-column prop="createTime" label="上传时间" width="175" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <button class="abtn abtn-view" @click.stop="handlePreview(row)">预览</button>
              <button
                v-if="row.parseStatus === 0 || row.parseStatus === 3"
                class="abtn abtn-retry"
                @click.stop="handleReparse(row)"
              >重解析</button>
              <button class="abtn abtn-del" @click.stop="handleDelete(row)">删除</button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && list.length === 0" class="empty-state">
        <el-empty :image-size="80" description="该知识库下暂无文档，请上传文档" />
      </div>
    </el-card>

    <el-dialog v-model="previewVisible" title="文档预览" width="80%" top="3vh" destroy-on-close>
      <div v-if="previewUrl" class="preview-frame">
        <iframe :src="previewUrl" class="preview-iframe" />
      </div>
      <div v-else class="preview-empty">无法加载预览</div>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
/* back link */
.back-row { margin-bottom: 2px; }
.back-btn {
  display: inline-flex; align-items: center; gap: 4px;
  background: none; border: none; color: #6b7280;
  font-size: 13px; cursor: pointer; padding: 0;
  font-family: inherit; transition: color 0.15s;
  &:hover { color: #1677ff; }
}

/* ── Table ── */
:deep(.doc-table) { --el-table-border-color: transparent; }

:deep(.doc-table .el-table__header-wrapper th) {
  font-size: 12px !important; font-weight: 600 !important;
  color: #6b7280 !important; background: #f9fafb !important;
  border-bottom: 2px solid #e5e7eb !important; padding: 14px 0 !important;
}

:deep(.doc-table .el-table__body-wrapper td) {
  font-size: 13.5px !important; padding: 14px 0 !important;
  border-bottom: 1px solid #f0f1f3 !important;
  color: #374151 !important; vertical-align: middle !important;
}

:deep(.doc-table tr.el-table__row--striped td) { background: #fafbfc !important; }

:deep(.doc-table .el-table__body-wrapper table tr.el-table__row:hover td) {
  background: #f0f4ff !important; cursor: pointer !important;
}

:deep(.doc-table .el-table__body-wrapper tr.el-table__row) {
  transition: background 0.12s ease !important;
}

/* ── Cell content ── */
.file-type-badge {
  display: inline-block; padding: 2px 8px; border-radius: 4px;
  font-size: 11.5px; font-weight: 500; background: #f3f4f6;
  color: #6b7280; text-transform: uppercase; letter-spacing: 0.04em;
}

/* ── Parse status pill ── */
.ps-tag {
  display: inline-flex; align-items: center; justify-content: center;
  border-radius: 9999px; padding: 3px 12px; font-size: 12px;
  font-weight: 500; line-height: 1.5; white-space: nowrap; min-width: 56px;
}
.ps-pending  { background: #f3f4f6; color: #9ca3af; }
.ps-parsing  { background: #fef9e7; color: #d97706; }
.ps-done     { background: #ecfdf5; color: #059669; }
.ps-failed   { background: #fef2f2; color: #dc2626; }

/* ── Actions ── */
.action-btns { display: inline-flex; align-items: center; gap: 8px; }
.abtn {
  display: inline-flex; align-items: center; justify-content: center;
  height: 28px; padding: 0 10px; border-radius: 6px; font-size: 12px;
  font-weight: 500; cursor: pointer; transition: all 0.15s ease;
  border: 1px solid transparent; outline: none; white-space: nowrap;
  font-family: inherit; line-height: 1; flex-shrink: 0;
}
.abtn-view { background: transparent; color: #6b7280; border: none;
  &:hover { color: #1677ff; background: #f0f5ff; }
}
.abtn-del { background: #fef2f2; color: #dc2626; border-color: #fecaca;
  &:hover { background: #dc2626; color: #fff; border-color: #dc2626; }
}
.abtn-retry { background: #fef9e7; color: #d97706; border-color: #fde68a;
  &:hover { background: #d97706; color: #fff; border-color: #d97706; }
}

/* ── Empty ── */
.empty-state { display: flex; justify-content: center; padding: 56px 0 40px; }

/* ── Preview ── */
.preview-frame { height: 70vh; border-radius: 6px; overflow: hidden; border: 1px solid #e5e7eb; }
.preview-iframe { width: 100%; height: 100%; border: none; }
.preview-empty { display: flex; align-items: center; justify-content: center; height: 200px; color: #9ca3af; font-size: 14px; }
</style>
