<script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";
import { getUserInfo, updateUser, uploadAvatar } from "@/api/user";
import { Camera } from "@element-plus/icons-vue";

const userStore = useUserStore();

const loading = ref(false);
const pwdLoading = ref(false);
const infoLoading = ref(true);

const form = reactive({
  username: "",
  nickname: "",
  email: "",
  phone: "",
});

const passwordForm = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
});

const changed = computed(() => {
  const u = userStore.userInfo;
  if (!u) return false;
  return form.nickname !== (u.nickname || "")
    || form.email !== (u.email || "")
    || form.phone !== (u.phone || "");
});

const emailValid = computed(() => !form.email || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email));
const phoneValid = computed(() => !form.phone || /^1[3-9]\d{9}$/.test(form.phone));
const pwdMatch = computed(() => !passwordForm.confirmPassword || passwordForm.newPassword === passwordForm.confirmPassword);

const pwdStrength = computed(() => {
  const p = passwordForm.newPassword;
  if (!p) return { label: "", level: 0 };
  let score = 0;
  if (p.length >= 8) score++;
  if (/[a-z]/.test(p) && /[A-Z]/.test(p)) score++;
  if (/\d/.test(p)) score++;
  if (/[^a-zA-Z\d]/.test(p)) score++;
  return score <= 1 ? { label: "弱", level: 1 } : score === 2 ? { label: "中", level: 2 } : { label: "强", level: 3 };
});

const canSave = computed(() => changed.value && emailValid.value && phoneValid.value);
const canChangePwd = computed(() =>
  passwordForm.oldPassword && passwordForm.newPassword && passwordForm.confirmPassword && pwdMatch.value
);

const avatarKey = ref(0);
const avatarSrc = computed(() => userStore.userInfo?.avatarUrl || "");

async function fetchProfile() {
  infoLoading.value = true;
  try {
    const res: any = await getUserInfo();
    const user = res.data || {};
    form.username = user.username || "";
    form.nickname = user.nickname || "";
    form.email = user.email || "";
    form.phone = user.phone || "";
    userStore.userInfo = { ...user };
  } catch {
    const u = userStore.userInfo;
    if (u) {
      form.username = u.username || "";
      form.nickname = u.nickname || "";
      form.email = u.email || "";
      form.phone = u.phone || "";
    }
  } finally {
    infoLoading.value = false;
  }
}

async function handleSaveProfile() {
  if (!canSave.value) return;
  loading.value = true;
  try {
    await updateUser({
      id: userStore.userInfo?.id,
      nickname: form.nickname,
      email: form.email,
    });
    ElMessage.success("个人信息更新成功");
    await userStore.fetchUserInfo();
  } catch {
    /* interceptor handles */
  } finally {
    loading.value = false;
  }
}

async function handleAvatarUpload() {
  const input = document.createElement("input");
  input.type = "file";
  input.accept = "image/*";
  input.onchange = async () => {
    const file = input.files?.[0];
    if (!file) return;
    try {
      const res: any = await uploadAvatar(file);
      const url = res?.data;
      if (url && typeof url === "string") {
        if (userStore.userInfo) {
          userStore.userInfo = { ...userStore.userInfo, avatarUrl: url };
        }
        avatarKey.value++;
        ElMessage.success("头像已更新");
      } else {
        // fallback: refresh from server
        await userStore.fetchUserInfo();
        avatarKey.value++;
        ElMessage.success("头像已更新");
      }
    } catch { /* handled */ }
  };
  input.click();
}

async function handleChangePassword() {
  if (!canChangePwd.value) return;
  pwdLoading.value = true;
  try {
    await updateUser({
      id: userStore.userInfo?.id,
      oldPassword: passwordForm.oldPassword,
      password: passwordForm.newPassword,
    });
    ElMessage.success("密码修改成功");
    passwordForm.oldPassword = "";
    passwordForm.newPassword = "";
    passwordForm.confirmPassword = "";
  } catch { /* handled */ }
  pwdLoading.value = false;
}

onMounted(fetchProfile);
</script>

<template>
  <div class="profile-page">
    <h2 class="page-title">个人中心</h2>

    <div class="profile-grid">
      <!-- ====== Left: Avatar + Info ====== -->
      <el-card shadow="never" class="profile-card" v-loading="infoLoading">
        <!-- Avatar -->
        <div class="avatar-section">
          <div class="avatar-wrap" @click="handleAvatarUpload" title="更换头像">
            <el-avatar :size="72" :src="avatarSrc" :key="avatarKey" />
            <div class="avatar-overlay">
              <el-icon :size="18"><Camera /></el-icon>
              <span class="avatar-tip">更换头像</span>
            </div>
          </div>
          <div class="avatar-info">
            <p class="avatar-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username || "用户" }}</p>
            <p class="avatar-role">{{ userStore.userInfo?.deptName || "系统用户" }}</p>
          </div>
        </div>

        <!-- Form -->
        <el-form :model="form" label-width="64px" class="profile-form">
          <el-form-item label="用户名">
            <div class="readonly-field">{{ form.username || "—" }}</div>
          </el-form-item>

          <el-form-item label="昵称" required>
            <el-input
              v-model="form.nickname"
              placeholder="请输入昵称"
              maxlength="50"
              show-word-limit
              clearable
            />
          </el-form-item>

          <el-form-item label="邮箱">
            <el-input
              v-model="form.email"
              placeholder="请输入邮箱"
              clearable
            />
            <span v-if="form.email && !emailValid" class="field-err">邮箱格式不正确</span>
          </el-form-item>

          <el-form-item label="手机号">
            <el-input
              v-model="form.phone"
              placeholder="请输入手机号"
              maxlength="11"
              clearable
            />
            <span v-if="form.phone && !phoneValid" class="field-err">手机号格式不正确</span>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              :disabled="!canSave"
              @click="handleSaveProfile"
            >
              保存修改
            </el-button>
            <span v-if="!changed" class="field-hint ml-2">未做修改</span>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- ====== Right: Password ====== -->
      <el-card shadow="never" class="profile-card">
        <template #header>
          <span class="card-title">修改密码</span>
        </template>

        <el-form :model="passwordForm" label-width="80px" class="password-form">
          <el-form-item label="当前密码" required>
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              show-password
              placeholder="请输入当前密码"
            />
          </el-form-item>

          <el-form-item label="新密码" required>
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              show-password
              placeholder="请输入新密码"
            />
            <div v-if="passwordForm.newPassword" class="pwd-strength">
              <span class="pwd-bar" :class="'level-' + pwdStrength.level" />
              <span class="pwd-label">{{ pwdStrength.label }}</span>
            </div>
          </el-form-item>

          <el-form-item label="确认密码" required>
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              show-password
              placeholder="请再次输入新密码"
            />
            <span v-if="passwordForm.confirmPassword && !pwdMatch" class="field-err">
              两次输入密码不一致
            </span>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              :loading="pwdLoading"
              :disabled="!canChangePwd"
              @click="handleChangePassword"
            >
              修改密码
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.profile-page {
  max-width: 840px;
}

.page-title {
  margin: 0 0 22px;
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  letter-spacing: -0.02em;
}

.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  align-items: start;

  @media (max-width: 720px) {
    grid-template-columns: 1fr;
  }
}

.profile-card {
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
}

.card-title {
  font-weight: 600;
  font-size: 15px;
  color: #1f2937;
}

/* ── Avatar ── */
.avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  padding-bottom: 22px;
  border-bottom: 1px solid #f3f4f6;
}

.avatar-wrap {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  flex-shrink: 0;

  &:hover .avatar-overlay { opacity: 1; }
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0,0,0,0.45);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: #fff;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-tip {
  font-size: 10px;
  font-weight: 500;
}

.avatar-info { min-width: 0; }

.avatar-name {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.avatar-role {
  margin: 3px 0 0;
  font-size: 13px;
  color: #9ca3af;
}

/* ── Readonly field ── */
.readonly-field {
  display: flex;
  align-items: center;
  height: 32px;
  padding: 0 12px;
  font-size: 13px;
  color: #9ca3af;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

/* ── Form ── */
.profile-form :deep(.el-form-item),
.password-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

/* Validation */
.field-err {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #ef4444;
  line-height: 1.4;
}

.field-hint {
  font-size: 12px;
  color: #9ca3af;
}

/* Password strength */
.pwd-strength {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}

.pwd-bar {
  height: 4px;
  border-radius: 2px;
  flex: 1;
  max-width: 80px;
  transition: all 0.2s;

  &.level-1 { background: #ef4444; width: 30%; }
  &.level-2 { background: #f59e0b; width: 60%; }
  &.level-3 { background: #10b981; width: 100%; }
}

.pwd-label {
  font-size: 12px;
  color: #6b7280;
}
</style>
