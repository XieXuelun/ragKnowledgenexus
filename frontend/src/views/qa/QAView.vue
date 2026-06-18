<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from "vue";
import { getKBList } from "@/api/knowledgeBase";
import { createConversation, askQuestion, getConversationList, getConversationDetail } from "@/api/qa";
import { ElMessage } from "element-plus";
import { Promotion, Plus, ChatDotRound, Message } from "@element-plus/icons-vue";

interface Message {
  role: "user" | "assistant";
  content: string;
  sources?: { chunkId: number; content: string; score: number }[];
}

interface Conversation {
  id: number;
  title: string;
  kbId: number;
  createTime: string;
}

const messages = ref<Message[]>([]);
const question = ref("");
const loading = ref(false);
const selectedKb = ref<number>();
const kbOptions = ref<{ id: number; label: string }[]>([]);
const convId = ref<number | null>(null);
const conversations = ref<Conversation[]>([]);
const convLoading = ref(false);
const activeConvId = ref<number | null>(null);
const chatContainer = ref<HTMLElement | null>(null);

function addWelcome() {
  messages.value = [{
    role: "assistant",
    content: "你好！我是企业智能问答助手。请选择知识库并输入你的问题。",
  }];
}

async function loadConversations() {
  convLoading.value = true;
  try {
    const res = await getConversationList({ page: 1, pageSize: 50 });
    conversations.value = ((res as any).data as any[]) || [];
  } catch {}
  convLoading.value = false;
}

async function switchConversation(conv: Conversation) {
  activeConvId.value = conv.id;
  convId.value = conv.id;
  selectedKb.value = conv.kbId;
  loading.value = true;
  try {
    const res = await getConversationDetail(conv.id);
    const data = (res as any).data || {};
    const msgs = data.messages || [];
    const merged: Message[] = [];
    for (const m of msgs) {
      if (m.question) merged.push({ role: "user", content: m.question });
      if (m.answer) merged.push({ role: "assistant", content: m.answer });
    }
    messages.value = merged.length ? merged : [{ role: "assistant", content: "此对话暂无消息" }];
  } catch {
    messages.value = [{ role: "assistant", content: "加载对话失败" }];
  }
  loading.value = false;
}

async function handleSend() {
  if (!question.value.trim()) return;
  if (!selectedKb.value) {
    ElMessage.warning("请先选择一个知识库");
    return;
  }
  if (!convId.value) {
    try {
      const convRes = await createConversation(selectedKb.value);
      convId.value = (convRes as any).data.conversationId;
      activeConvId.value = (convRes as any).data.conversationId;
      loadConversations();
    } catch {
      ElMessage.error("创建对话失败");
      return;
    }
  }
  const userMsg: Message = { role: "user", content: question.value };
  messages.value.push(userMsg);
  const q = question.value;
  question.value = "";
  loading.value = true;

  nextTick(() => {
    chatContainer.value?.scrollTo({ top: chatContainer.value.scrollHeight, behavior: "smooth" });
  });

  try {
    const res = await askQuestion({ conversationId: convId.value, question: q });
    messages.value.push({
      role: "assistant",
      content: (res as any).data.answer,
      sources: (res as any).data.sources,
    });
  } catch {
    messages.value.push({ role: "assistant", content: "抱歉，回答时出错了，请稍后重试。" });
  }
  loading.value = false;

  nextTick(() => {
    chatContainer.value?.scrollTo({ top: chatContainer.value.scrollHeight, behavior: "smooth" });
  });
}

function startNewChat() {
  convId.value = null;
  activeConvId.value = null;
  addWelcome();
}

async function handleKbChange() {
  convId.value = null;
  activeConvId.value = null;
  addWelcome();
  await loadConversations();
  if (selectedKb.value) {
    conversations.value = conversations.value.filter(c => c.kbId === selectedKb.value);
  }
}

onMounted(async () => {
  try {
    const kbRes = await getKBList({ page: 1, pageSize: 100 });
    kbOptions.value = ((kbRes as any).data as any[]).map((k: any) => ({ id: k.id, label: k.name }));
  } catch {}
  addWelcome();
  loadConversations();
});

watch(selectedKb, () => {
  handleKbChange();
});
</script>

<template>
  <div class="qa-layout">
    <!-- Left: Conversations sidebar -->
    <div class="qa-sidebar">
      <div class="sidebar-header">
        <el-select v-model="selectedKb" placeholder="选择知识库" size="default" clearable style="width:100%">
          <el-option v-for="kb in kbOptions" :key="kb.id" :label="kb.label" :value="kb.id" />
        </el-select>
      </div>
      <div class="sidebar-actions">
        <el-button :icon="Plus" class="btn-dark" style="width:100%" @click="startNewChat">新对话</el-button>
      </div>
      <div class="sidebar-list" v-loading="convLoading">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="conv-item"
          :class="{ active: conv.id === activeConvId }"
          @click="switchConversation(conv)"
        >
          <el-icon :size="16"><Message /></el-icon>
          <span class="conv-title">{{ conv.title || "新对话" }}</span>
          <span class="conv-time">{{ conv.createTime?.slice(5, 10) || "" }}</span>
        </div>
        <div v-if="!convLoading && conversations.length === 0" class="empty-hint">暂无历史对话</div>
      </div>
    </div>

    <!-- Right: Chat area -->
    <div class="qa-main">
      <div class="qa-header">
        <h3 class="qa-title">
          <ChatDotRound style="margin-right:6px" />
          智能问答
        </h3>
      </div>

      <div class="qa-messages" ref="chatContainer">
        <div v-for="(msg, idx) in messages" :key="idx" class="message" :class="msg.role">
          <div class="msg-avatar">{{ msg.role === "user" ? "U" : "A" }}</div>
          <div class="msg-body">
            <div class="msg-content" v-html="msg.content.replace(/\n/g, '<br>')"></div>
            <div v-if="msg.sources && msg.sources.length" class="msg-refs">
              <div class="ref-title">参考来源 ({{ msg.sources.length }})</div>
              <div v-for="(ref, ri) in msg.sources.slice(0, 3)" :key="ri" class="ref-item">
                <span class="ref-doc">片段 #{{ ref.chunkId }}</span>
                <span class="ref-score">相似度 {{ (ref.score * 100).toFixed(1) }}%</span>
              </div>
            </div>
          </div>
        </div>
        <div v-if="loading" class="message assistant">
          <div class="msg-avatar">A</div>
          <div class="msg-content typing">思考中...</div>
        </div>
      </div>

      <div class="qa-input-area">
        <el-input
          v-model="question"
          type="textarea"
          :rows="2"
          placeholder="输入你的问题，Enter 发送..."
          :disabled="loading"
          @keyup.enter.prevent="handleSend"
        />
        <div class="input-actions">
          <span class="input-hint">Enter 发送</span>
          <el-button type="primary" :icon="Promotion" :loading="loading" @click="handleSend">发送</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.qa-layout {
  display: flex;
  height: calc(100vh - 200px);
  gap: 16px;
}

// ===== Left sidebar =====
.qa-sidebar {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
}
.sidebar-header {
  padding: 16px 14px 10px;
  border-bottom: 1px solid #f3f4f6;
}
.sidebar-actions {
  padding: 10px 14px;
  border-bottom: 1px solid #f3f4f6;
}
.sidebar-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px 8px;
}
.conv-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: #374151;
  transition: background 0.15s;
  &:hover { background: #f3f4f6; }
  &.active { background: #eff6ff; color: #2563eb; }
}
.conv-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}
.conv-time {
  font-size: 11px;
  color: #9ca3af;
  flex-shrink: 0;
}
.empty-hint {
  text-align: center;
  padding: 40px 0;
  color: #9ca3af;
  font-size: 13px;
}

// ===== Right main =====
.qa-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
  min-width: 0;
}
.qa-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid #f3f4f6;
}
.qa-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  display: flex;
  align-items: center;
}
.qa-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}
.message {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}
.message.user {
  flex-direction: row-reverse;
  .msg-avatar { background: #2563eb; }
}
.msg-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #6b7280;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}
.msg-body { max-width: 70%; }
.msg-content {
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 13.5px;
  line-height: 1.7;
}
.message.user .msg-content {
  background: #2563eb;
  color: #fff;
}
.message.assistant .msg-content {
  background: #f9fafb;
  color: #1f2937;
  border: 1px solid #f3f4f6;
}
.typing { color: #9ca3af; font-style: italic; }
.msg-refs {
  margin-top: 8px;
  padding: 10px 14px;
  background: #fff;
  border: 1px solid #f3f4f6;
  border-radius: 8px;
  font-size: 12px;
}
.ref-title { font-weight: 600; color: #6b7280; margin-bottom: 6px; }
.ref-item {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  color: #6b7280;
}
.ref-doc { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; margin-right: 8px; }
.ref-score { flex-shrink: 0; color: #2563eb; }
.qa-input-area {
  border-top: 1px solid #f3f4f6;
  padding: 14px 24px;
}
.input-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 10px;
}
.input-hint {
  font-size: 12px;
  color: #9ca3af;
}
</style>
