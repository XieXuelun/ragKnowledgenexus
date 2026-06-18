<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { getKBList, deleteKB, createKB, updateKB } from "@/api/knowledgeBase";
import type { KnowledgeBase } from "@/types/knowledgeBase";
import { Plus, Edit, Delete, Search, View } from "@element-plus/icons-vue";

const router = useRouter();
const loading = ref(false);
const list = ref<KnowledgeBase[]>([]);
const keyword = ref("");
const dialogVisible = ref(false);
const dialogTitle = ref("新建知识库");
const form = ref({ id: "", name: "", description: "", visibility: 0, deptId: 0 });
const detailVisible = ref(false);
const detailRow = ref<KnowledgeBase | null>(null);

const allColumns = [
  { key: "name", label: "名称" },
  { key: "description", label: "描述" },
  { key: "visibility", label: "可见范围" },
  { key: "docCount", label: "文档数" },
  { key: "createTime", label: "创建时间" },
];
const visibleColumns = ref(allColumns.map((c) => c.key));

const visibilityConfig: Record<number, { label: string; className: string }> = {
  0: { label: "私有", className: "vis-tag-private" },
  1: { label: "部门可见", className: "vis-tag-dept" },
  2: { label: "全员可见", className: "vis-tag-public" },
};

const filteredList = computed(() => {
  if (!keyword.value) return list.value;
  const kw = keyword.value.toLowerCase();
  return list.value.filter(
    (item) =>
      item.name.toLowerCase().includes(kw) ||
      (item.description && item.description.toLowerCase().includes(kw))
  );
});

async function fetchData() {
  loading.value = true;
  try {
    const res = await getKBList({ page: 1, pageSize: 100, keyword: keyword.value });
    list.value = (res as any).data || [];
  } finally { loading.value = false; }
}

function handleAdd() {
  dialogTitle.value = "新建知识库";
  form.value = { id: "", name: "", description: "", visibility: 0, deptId: 0 };
  dialogVisible.value = true;
}
function handleEdit(row: KnowledgeBase) {
  dialogTitle.value = "编辑知识库";
  form.value = { id: row.id, name: row.name, description: row.description || "", visibility: row.visibility, deptId: (row as any).deptId || 0 };
  dialogVisible.value = true;
}
async function handleDelete(row: KnowledgeBase) {
  try { await ElMessageBox.confirm(`确定删除知识库「${row.name}」？`, "确认删除", { type: "warning" }); }
  catch { return; }
  await deleteKB(row.id);
  ElMessage.success("删除成功");
  fetchData();
}
async function handleSave() {
  if (!form.value.name) { ElMessage.warning("请输入知识库名称"); return; }
  try {
    if (form.value.id) {
      await updateKB({ id: form.value.id, name: form.value.name, description: form.value.description });
      ElMessage.success("更新成功");
    } else {
      const res = await createKB(form.value);
      const created = (res as any).data;
      ElMessage.success("创建成功");
      dialogVisible.value = false;
      // Navigate directly to the new KB's document list
      if (created && created.id) {
        router.push(`/knowledgeBase/${created.id}`);
        return;
      }
    }
  } catch {
    return;
  }
  dialogVisible.value = false;
  fetchData();
}
function handleView(row: KnowledgeBase) { detailRow.value = row; detailVisible.value = true; }
function goToDetail(row: KnowledgeBase) { router.push(`/knowledgeBase/${row.id}`); }

onMounted(fetchData);
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <div>
            <h3 class="page-title">知识库管理</h3>
            <p class="page-desc">管理知识库，支持文档上传、检索配置和权限控制</p>
          </div>
          <div class="page-actions">
            <el-input v-model="keyword" placeholder="搜索知识库..." clearable size="default" style="width:200px" @clear="fetchData" @keyup.enter="fetchData">
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
            <el-button size="small" :icon="Plus" class="btn-dark" @click="handleAdd">新建知识库</el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredList" v-loading="loading" stripe class="kb-table" @row-click="goToDetail">
        <el-table-column type="index" label="#" width="56" align="center" />
        <el-table-column v-if="visibleColumns.includes('name')" prop="name" label="名称" min-width="200">
          <template #default="{ row }">
            <span class="kb-link">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="visibleColumns.includes('description')" prop="description" label="描述" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="desc-cell">{{ row.description || "—" }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="visibleColumns.includes('visibility')" label="可见范围" width="110" align="center">
          <template #default="{ row }">
            <span class="vis-tag" :class="visibilityConfig[row.visibility]?.className || 'vis-tag-private'">
              {{ visibilityConfig[row.visibility]?.label || "未知" }}
            </span>
          </template>
        </el-table-column>
        <el-table-column v-if="visibleColumns.includes('docCount')" prop="docCount" label="文档数" width="80" align="center" />
        <el-table-column v-if="visibleColumns.includes('createTime')" prop="createTime" label="创建时间" width="175" />
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <button class="abtn abtn-view" @click.stop="handleView(row)">查看</button>
              <button class="abtn abtn-edit" @click.stop="handleEdit(row)">编辑</button>
              <button class="abtn abtn-del" @click.stop="handleDelete(row)">删除</button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && filteredList.length === 0" class="empty-state">
        <el-empty :image-size="80" description="暂无知识库" />
      </div>
    </el-card>

    <!-- Create / Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" destroy-on-close>
      <el-form :model="form" label-width="80px" class="pt-2">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="知识库名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="描述（可选）" />
        </el-form-item>
        <el-form-item label="可见范围">
          <el-radio-group v-model="form.visibility">
            <el-radio :value="0">私有</el-radio>
            <el-radio :value="1">部门可见</el-radio>
            <el-radio :value="2">全员可见</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- Detail Dialog -->
    <el-dialog v-model="detailVisible" title="知识库详情" width="460px" destroy-on-close>
      <div v-if="detailRow" class="detail-grid">
        <div class="detail-row"><span class="detail-label">名称</span><span class="detail-value fw-600">{{ detailRow.name }}</span></div>
        <div class="detail-row"><span class="detail-label">描述</span><span class="detail-value">{{ detailRow.description || "—" }}</span></div>
        <div class="detail-row"><span class="detail-label">可见范围</span><span class="vis-tag" :class="visibilityConfig[detailRow.visibility]?.className || 'vis-tag-private'">{{ visibilityConfig[detailRow.visibility]?.label }}</span></div>
        <div class="detail-row"><span class="detail-label">文档数</span><span class="detail-value">{{ detailRow.docCount }}</span></div>
        <div class="detail-row"><span class="detail-label">创建者</span><span class="detail-value">{{ detailRow.creatorName || "—" }}</span></div>
        <div class="detail-row"><span class="detail-label">创建时间</span><span class="detail-value">{{ detailRow.createTime }}</span></div>
        <div class="detail-row" style="border-bottom:none"><span class="detail-label">更新时间</span><span class="detail-value">{{ detailRow.updateTime || "—" }}</span></div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="detailVisible = false; goToDetail(detailRow!)">查看文档</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
/* ═══════════════════════════════════════════
   Table element overrides
   ═══════════════════════════════════════════ */
:deep(.kb-table) {
  --el-table-border-color: transparent;
}

/* header */
:deep(.kb-table .el-table__header-wrapper th) {
  font-size: 12px !important;
  font-weight: 600 !important;
  color: #6b7280 !important;
  background: #f9fafb !important;
  border-bottom: 2px solid #e5e7eb !important;
  padding: 14px 0 !important;
}

/* body cells */
:deep(.kb-table .el-table__body-wrapper td) {
  font-size: 13.5px !important;
  padding: 14px 0 !important;
  border-bottom: 1px solid #f0f1f3 !important;
  color: #374151 !important;
  vertical-align: middle !important;
}

/* zebra — Element Plus adds el-table__row--striped on the TR */
:deep(.kb-table tr.el-table__row--striped td) {
  background: #fafbfc !important;
}

/* hover — target the TR inside the body wrapper's table */
:deep(.kb-table .el-table__body-wrapper table tr.el-table__row:hover td) {
  background: #f0f4ff !important;
  cursor: pointer !important;
}

/* smooth row transition */
:deep(.kb-table .el-table__body-wrapper tr.el-table__row) {
  transition: background 0.12s ease !important;
}

/* ═══════════════════════════════════════════
   Description cell
   ═══════════════════════════════════════════ */
.desc-cell {
  display: block;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #9ca3af;
  line-height: 1.5;
}

/* ═══════════════════════════════════════════
   Action buttons — native <button> elements
   ═══════════════════════════════════════════ */
.action-btns {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.abtn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 30px;
  padding: 0 14px;
  border-radius: 6px;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1px solid transparent;
  outline: none;
  white-space: nowrap;
  font-family: inherit;
  line-height: 1;
}

/* 查看 — low-key text style */
.abtn-view {
  background: transparent;
  color: #6b7280;
  border: none;

  &:hover {
    color: #1677ff;
    background: #f0f5ff;
  }
}

/* 编辑 — outline style */
.abtn-edit {
  background: #fff;
  color: #374151;
  border-color: #d1d5db;

  &:hover {
    color: #1677ff;
    border-color: #1677ff;
    background: #f0f5ff;
  }
}

/* 删除 — danger style */
.abtn-del {
  background: #fef2f2;
  color: #dc2626;
  border-color: #fecaca;

  &:hover {
    background: #dc2626;
    color: #fff;
    border-color: #dc2626;
  }
}

/* ═══════════════════════════════════════════
   Visibility pill tags
   ═══════════════════════════════════════════ */
.vis-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  padding: 3px 12px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.5;
  white-space: nowrap;
  min-width: 64px;
}

.vis-tag-private {
  background: #f3f4f6 !important;
  color: #6b7280 !important;
}

.vis-tag-dept {
  background: #e6f4ff !important;
  color: #1677ff !important;
}

.vis-tag-public {
  background: #ecfdf5 !important;
  color: #059669 !important;
}

/* ═══════════════════════════════════════════
   Detail dialog
   ═══════════════════════════════════════════ */
.detail-grid {
  display: flex;
  flex-direction: column;
  padding-top: 4px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 13px 0;
  border-bottom: 1px solid #f3f4f6;
}

.detail-label {
  font-size: 13px;
  color: #9ca3af;
  flex-shrink: 0;
}

.detail-value {
  font-size: 13.5px;
  color: #1f2937;
  text-align: right;
  max-width: 280px;
}

.fw-600 { font-weight: 600; }
</style>
