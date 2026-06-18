<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import request from "@/api/request";
import { getDepartmentTree } from "@/api/user";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus, Edit, Delete, Search, Refresh, User, FolderOpened } from "@element-plus/icons-vue";

interface DeptNode {
  id: number; name: string; parentId: number; sort?: number;
  remark?: string; userCount?: number; children?: DeptNode[];
}

const loading = ref(false);
const treeData = ref<DeptNode[]>([]);
const selectedDept = ref<DeptNode | null>(null);
const dialogVisible = ref(false);
const dialogTitle = ref("");
const isEdit = ref(false);
const searchQuery = ref("");
const members = ref<any[]>([]);
const membersLoading = ref(false);
const form = ref({ id: 0, name: "", parentId: 0, sort: 0, remark: "" });

async function fetchTree() {
  loading.value = true;
  try {
    const res = await getDepartmentTree();
    treeData.value = (res as any).data || [];
  } catch { treeData.value = []; }
  loading.value = false;
}

async function loadMembers(deptId: number) {
  membersLoading.value = true;
  try {
    const res = await request.get("/api/v1/department/" + deptId + "/members");
    members.value = (res as any).data || [];
  } catch { members.value = []; }
  membersLoading.value = false;
}

function handleNodeClick(data: DeptNode) {
  selectedDept.value = data;
  loadMembers(data.id);
}

function handleAdd() {
  isEdit.value = false;
  dialogTitle.value = "新建部门";
  form.value = { id: 0, name: "", parentId: selectedDept.value?.id || 0, sort: 0, remark: "" };
  dialogVisible.value = true;
}

function handleEdit(dept: DeptNode) {
  isEdit.value = true;
  dialogTitle.value = "编辑部门";
  form.value = { id: dept.id, name: dept.name, parentId: dept.parentId, sort: dept.sort || 0, remark: dept.remark || "" };
  dialogVisible.value = true;
}

async function handleDelete(dept: DeptNode) {
  try {
    await ElMessageBox.confirm(`确定删除部门「${dept.name}」？`, "确认删除", {
      confirmButtonText: "删除", cancelButtonText: "取消", type: "warning"
    });
  } catch { return; }
  try {
    await request.delete("/api/v1/department/delete/" + dept.id);
    ElMessage.success("删除成功");
    selectedDept.value = null;
    members.value = [];
    fetchTree();
  } catch {}
}

async function handleSave() {
  if (!form.value.name) { ElMessage.warning("部门名称不能为空"); return; }
  try {
    if (isEdit.value) {
      await request.post("/api/v1/department/update", form.value);
      ElMessage.success("更新成功");
    } else {
      await request.post("/api/v1/department/create", form.value);
      ElMessage.success("创建成功");
    }
    dialogVisible.value = false;
    fetchTree();
  } catch {}
}

const filteredTree = computed(() => {
  if (!searchQuery.value) return treeData.value;
  const q = searchQuery.value.toLowerCase();
  function filter(nodes: DeptNode[]): DeptNode[] {
    return nodes.filter(n => {
      const match = n.name.toLowerCase().includes(q);
      const childMatch = n.children ? filter(n.children) : [];
      return match || childMatch.length > 0;
    }).map(n => ({ ...n, children: n.children ? filter(n.children) : undefined }));
  }
  return filter(treeData.value);
});

onMounted(fetchTree);
</script>

<template>
  <div class="page-container">
    <div class="page-header" style="margin-bottom:16px">
      <div>
        <h3 class="page-title">部门管理</h3>
        <p class="page-desc">管理组织架构，支持新增、编辑和删除部门，查看部门成员</p>
      </div>
      <div class="page-actions">
        <el-button size="small" class="btn-secondary-sm" :icon="Refresh" @click="fetchTree">刷新</el-button>
        <el-button size="small" :icon="Plus" class="btn-dark" @click="handleAdd">新建部门</el-button>
      </div>
    </div>

    <div class="dept-layout">
      <!-- Left: Tree -->
      <div class="dept-tree-panel">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-header-row">
              <span class="panel-title">组织结构</span>
              <el-input v-model="searchQuery" placeholder="搜索" clearable size="small" style="width:110px" />
            </div>
          </template>
          <el-tree
            :data="filteredTree"
            :props="{ children: 'children', label: 'name' }"
            node-key="id"
            default-expand-all
            highlight-current
            :expand-on-click-node="false"
            v-loading="loading"
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <span class="tree-node-row">
                <span class="tree-node-name">{{ node.label }}</span>
                <span class="tree-node-count">{{ data.userCount || 0 }}人</span>
              </span>
            </template>
          </el-tree>
          <div v-if="!loading && treeData.length === 0" class="panel-empty">暂无部门数据</div>
        </el-card>
      </div>

      <!-- Right: Detail + Members -->
      <div class="dept-detail-panel">
        <template v-if="selectedDept">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-header-row">
                <span class="panel-title">{{ selectedDept.name }}</span>
                <div style="display:flex;gap:6px">
                  <el-button size="small" class="btn-secondary-sm" :icon="Edit" @click="handleEdit(selectedDept!)">编辑</el-button>
                  <el-button size="small" class="btn-danger-sm" :icon="Delete" @click="handleDelete(selectedDept!)">删除</el-button>
                </div>
              </div>
            </template>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="ID">{{ selectedDept.id }}</el-descriptions-item>
              <el-descriptions-item label="部门名称">{{ selectedDept.name }}</el-descriptions-item>
              <el-descriptions-item label="上级ID">{{ selectedDept.parentId || "-" }}</el-descriptions-item>
              <el-descriptions-item label="排序">{{ selectedDept.sort || 0 }}</el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">{{ selectedDept.remark || "-" }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <el-card shadow="never" class="panel-card member-card">
            <template #header>
              <div class="panel-header-row">
                <span class="panel-title">
                  <el-icon style="margin-right:4px;vertical-align:-2px"><User /></el-icon>
                  部门成员（{{ members.length }}人）
                </span>
              </div>
            </template>
            <el-table :data="members" v-loading="membersLoading" stripe size="small">
              <el-table-column type="index" label="#" width="50" align="center" />
              <el-table-column prop="username" label="用户名" />
              <el-table-column prop="nickname" label="姓名" />
              <el-table-column prop="email" label="邮箱" min-width="160" />
              <el-table-column label="角色" width="110">
                <template #default="{ row }">{{ row.role === 0 ? "超管" : row.role === 1 ? "KB管理员" : "员工" }}</template>
              </el-table-column>
            </el-table>
            <div v-if="!membersLoading && members.length === 0" class="panel-empty">暂无成员</div>
          </el-card>
        </template>

        <el-card shadow="never" class="panel-card empty-panel" v-else>
          <div class="empty-guide">
            <el-icon :size="56" style="color:#d1d5db;margin-bottom:16px"><FolderOpened /></el-icon>
            <p class="empty-guide-text">请从左侧选择一个部门</p>
            <p class="empty-guide-hint">选择部门后可查看详细信息及成员列表</p>
          </div>
        </el-card>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="部门名称" required>
          <el-input v-model="form.name" placeholder="请输入部门名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="上级部门">
          <el-tree-select v-model="form.parentId" :data="treeData"
            :props="{ children: 'children', label: 'name', value: 'id' }"
            placeholder="不选则为根部门" clearable style="width:100%" />
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" :max="999" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.dept-layout { display: flex; gap: 16px; flex: 1; min-height: 0; }
.dept-tree-panel { width: 300px; flex-shrink: 0; }
.dept-detail-panel { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 10px; }
.panel-header-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.panel-title { font-weight: 600; font-size: 13px; color: #333; }
.panel-empty { text-align: center; padding: 32px 0; color: #9ca3af; font-size: 13px; }
.member-card { flex: 1; overflow: hidden; display: flex; flex-direction: column; }
.member-card :deep(.el-card__body) { flex: 1; overflow: auto; padding: 0; }

.tree-node-row { display: flex; align-items: center; justify-content: space-between; width: 100%; font-size: 13px; }
.tree-node-name { color: #333; }
.tree-node-count { font-size: 11px; color: #9ca3af; background: #f3f4f6; padding: 0 6px; border-radius: 9999px; line-height: 18px; }

.empty-panel { flex: 1; display: flex; align-items: center; }
.empty-panel :deep(.el-card__body) { flex: 1; display: flex; align-items: center; justify-content: center; }

.empty-guide { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; }
.empty-guide-text { color: #6b7280; font-size: 14px; font-weight: 500; margin: 0 0 4px; }
.empty-guide-hint { color: #bfbfbf; font-size: 13px; margin: 0; }
</style>
