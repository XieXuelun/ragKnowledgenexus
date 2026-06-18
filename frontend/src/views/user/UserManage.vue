<script setup lang="ts">
import { ref, onMounted } from "vue";
import request from "@/api/request";
import { getUserList, deleteUser, setStatus } from "@/api/user";
import { ElMessage, ElMessageBox } from "element-plus";
import { Search, Plus, Delete, Switch, View } from "@element-plus/icons-vue";

const roleMap: Record<number, string> = {
  0: "超级管理员", 1: "知识库管理员", 2: "普通员工",
};
function getRoleName(role: number): string { return roleMap[role] || "未知"; }

const loading = ref(false);
const list = ref<any[]>([]);
const total = ref(0);
const page = ref(1);
const keyword = ref("");
const dialogVisible = ref(false);
const form = ref({ username: "", password: "", realName: "", email: "", role: 2 });

const allColumns = [
  { key: "username", label: "用户名" },
  { key: "nickname", label: "姓名" },
  { key: "email", label: "邮箱" },
  { key: "role", label: "角色" },
  { key: "deptName", label: "部门" },
  { key: "status", label: "状态" },
  { key: "createTime", label: "创建时间" },
];
const visibleColumns = ref(allColumns.map((c) => c.key));

async function fetchData() {
  loading.value = true;
  try {
    const res = await getUserList({ page: page.value, pageSize: 10, keyword: keyword.value });
    list.value = (res as any).data as any[];
    total.value = (res as any).total || 0;
  } catch {}
  loading.value = false;
}

async function handleSave() {
  if (!form.value.username || !form.value.password) { ElMessage.warning("用户名和密码不能为空"); return; }
  try {
    await request.post("/api/v1/user/register", form.value);
    ElMessage.success("创建成功"); dialogVisible.value = false; fetchData();
  } catch {}
}

async function handleToggleStatus(row: any) {
  const newStatus = row.status === 1 ? 0 : 1;
  const label = newStatus === 1 ? "启用" : "禁用";
  try {
    await ElMessageBox.confirm(`确定${label}用户 "${row.username}" 吗？`, "提示", { type: "warning" });
    await setStatus({ id: row.id, status: newStatus });
    ElMessage.success(`${label}成功`); fetchData();
  } catch {}
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除用户 "${row.username}" 吗？此操作不可恢复。`, "删除确认", {
      type: "error", confirmButtonText: "删除", cancelButtonText: "取消",
    });
    await deleteUser(row.id);
    ElMessage.success("删除成功"); fetchData();
  } catch {}
}

onMounted(fetchData);
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <div>
            <h3 class="page-title">用户管理</h3>
            <p class="page-desc">管理系统用户账号，支持创建、启用/禁用和删除用户</p>
          </div>
          <div class="page-actions">
            <el-input v-model="keyword" placeholder="搜索用户" clearable size="default" style="width:180px" @clear="fetchData" @keyup.enter="fetchData">
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
            <el-button size="small" :icon="Plus" class="btn-dark" @click="dialogVisible = true">新增用户</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column type="index" label="#" width="56" align="center" />
        <el-table-column prop="username" label="用户名" min-width="120" v-if="visibleColumns.includes('username')" />
        <el-table-column prop="nickname" label="姓名" min-width="120" v-if="visibleColumns.includes('nickname')" />
        <el-table-column prop="email" label="邮箱" min-width="180" v-if="visibleColumns.includes('email')" />
        <el-table-column label="角色" width="120" align="center" v-if="visibleColumns.includes('role')">
          <template #default="{ row }">{{ getRoleName(row.role) }}</template>
        </el-table-column>
        <el-table-column prop="deptName" label="部门" min-width="110" v-if="visibleColumns.includes('deptName')" />
        <el-table-column label="状态" width="80" align="center" v-if="visibleColumns.includes('status')">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? "正常" : "禁用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" v-if="visibleColumns.includes('createTime')" />
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-cell">
              <el-button size="small" class="btn-secondary-sm" :icon="Switch" @click="handleToggleStatus(row)">
                {{ row.status === 1 ? "禁用" : "启用" }}
              </el-button>
              <el-button size="small" class="btn-danger-sm" :icon="Delete" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && list.length === 0" class="empty-state">
        <el-empty :image-size="80" description="暂无用户" />
      </div>

      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" :total="total" :page-size="10" layout="total, prev, pager, next" background @current-change="fetchData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新增用户" width="480px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名" required><el-input v-model="form.username" placeholder="登录账号" /></el-form-item>
        <el-form-item label="密码" required><el-input v-model="form.password" type="password" placeholder="初始密码" show-password /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" placeholder="真实姓名" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" placeholder="邮箱地址" /></el-form-item>
        <el-form-item label="角色">
          <el-radio-group v-model="form.role">
            <el-radio :value="1">知识库管理员</el-radio>
            <el-radio :value="2">普通员工</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>
