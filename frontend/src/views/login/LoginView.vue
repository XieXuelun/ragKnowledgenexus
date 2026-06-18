<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";
import { register } from "@/api/auth";
import { getDepartmentTree } from "@/api/user";
import { Eye, EyeOff, ArrowRight, BookOpen, X } from "lucide-vue-next";

const router = useRouter();
const userStore = useUserStore();

// ─── Login ──────────────────────────────────────────────────────────────────
const username = ref("");
const password = ref("");
const isPasswordVisible = ref(false);
const loading = ref(false);
const isHovered = ref(false);
const formVisible = ref(false);

async function handleLogin() {
  if (!username.value || !password.value) {
    ElMessage.warning("请输入用户名和密码");
    return;
  }
  loading.value = true;
  try {
    await userStore.login({ username: username.value, password: password.value });
    ElMessage.success("登录成功");
    router.push("/dashboard");
  } catch {
    // error already handled by interceptor
  } finally {
    loading.value = false;
  }
}

function handleGoogleLogin() {
  console.log("Google sign-in");
}

// ─── Register ───────────────────────────────────────────────────────────────
const showRegister = ref(false);
const registerLoading = ref(false);
const departments = ref<{ id: number; name: string }[]>([]);
const registerForm = ref({
  username: "",
  password: "",
  realName: "",
  email: "",
  deptId: null as number | null,
});

async function fetchDepartments() {
  try {
    const res = await getDepartmentTree();
    // Flatten tree to flat list
    const flat: { id: number; name: string }[] = [];
    function walk(items: any[]) {
      for (const item of items) {
        flat.push({ id: item.id, name: item.name });
        if (item.children && item.children.length > 0) walk(item.children);
      }
    }
    walk(res.data || []);
    departments.value = flat;
  } catch {
    departments.value = [];
  }
}

async function handleRegister() {
  const { username: uname, password: pwd, realName, email: mail, deptId } = registerForm.value;
  if (!uname || !pwd) {
    ElMessage.warning("用户名和密码为必填项");
    return;
  }
  if (pwd.length < 6) {
    ElMessage.warning("密码长度不能少于6位");
    return;
  }
  registerLoading.value = true;
  try {
    await register({
      username: uname,
      password: pwd,
      realName: realName || undefined,
      email: mail || undefined,
      deptId: deptId ?? undefined,
    });
    ElMessage.success("注册成功，请登录");
    showRegister.value = false;
    username.value = uname;
  } catch {
    // error already handled by interceptor
  } finally {
    registerLoading.value = false;
  }
}

function resetRegisterForm() {
  registerForm.value = { username: "", password: "", realName: "", email: "", deptId: null };
}

watch(showRegister, (val) => {
  if (val) fetchDepartments();
});

// ─── Knowledge Graph Canvas ─────────────────────────────────────────────────
const canvasRef = ref<HTMLCanvasElement | null>(null);
let animationFrameId = 0;
let startTime = 0;
let resizeObserver: ResizeObserver | null = null;

interface GraphNode {
  x: number; y: number; vx: number; vy: number;
  radius: number; opacity: number; label: string;
}

interface GraphEdge {
  from: number; to: number;
  progress: number; speed: number;
  color: string;
}

const KNOWLEDGE_LABELS = [
  "BERT", "RAG", "Transformer", "Embedding",
  "Vector DB", "LLM", "Prompt", "Chunk",
  "Retrieval", "Ranking", "Fine-tune", "Semantic",
];

function generateKnowledgeGraph(width: number, height: number) {
  const nodes: GraphNode[] = [];
  const edges: GraphEdge[] = [];

  const count = 18;
  for (let i = 0; i < count; i++) {
    const angle = (i / count) * Math.PI * 2;
    const spread = Math.min(width, height) * 0.32;
    const cx = width / 2;
    const cy = height / 2 + 10;
    nodes.push({
      x: cx + Math.cos(angle) * spread * (0.6 + Math.random() * 0.4),
      y: cy + Math.sin(angle) * spread * (0.6 + Math.random() * 0.4),
      vx: (Math.random() - 0.5) * 0.15,
      vy: (Math.random() - 0.5) * 0.15,
      radius: 2.5 + Math.random() * 2,
      opacity: 0.35 + Math.random() * 0.4,
      label: KNOWLEDGE_LABELS[i % KNOWLEDGE_LABELS.length],
    });
  }

  nodes.push({
    x: width / 2, y: height / 2 + 10,
    vx: 0, vy: 0,
    radius: 5, opacity: 0.9,
    label: "KnowledgeNexus",
  });
  const centerIdx = nodes.length - 1;

  for (let i = 0; i < count; i++) {
    edges.push({
      from: i, to: centerIdx,
      progress: Math.random(),
      speed: 0.002 + Math.random() * 0.004,
      color: `hsla(0, 0%, ${55 + Math.random() * 20}%, ${0.15 + Math.random() * 0.2})`,
    });
    if (Math.random() > 0.6) {
      const j = Math.floor(Math.random() * count);
      if (j !== i) {
        edges.push({
          from: i, to: j,
          progress: Math.random(),
          speed: 0.001 + Math.random() * 0.002,
          color: `hsla(0, 0%, ${55 + Math.random() * 15}%, ${0.08 + Math.random() * 0.12})`,
        });
      }
    }
  }

  return { nodes, edges, centerIdx };
}

onMounted(() => {
  const canvas = canvasRef.value;
  if (!canvas) return;
  const parent = canvas.parentElement;
  if (!parent) return;

  setTimeout(() => { formVisible.value = true; }, 200);

  resizeObserver = new ResizeObserver((entries) => {
    const { width, height } = entries[0].contentRect;
    canvas.width = width;
    canvas.height = height;
  });
  resizeObserver.observe(parent);

  const ctx = canvas.getContext("2d");
  if (!ctx) return;

  let { nodes, edges } = generateKnowledgeGraph(600, 600);
  startTime = Date.now();

  const particles: { x: number; y: number; r: number; dx: number; dy: number; o: number }[] = [];
  for (let i = 0; i < 40; i++) {
    particles.push({
      x: Math.random() * 600, y: Math.random() * 600,
      r: 0.5 + Math.random() * 1,
      dx: (Math.random() - 0.5) * 0.2, dy: (Math.random() - 0.5) * 0.2,
      o: 0.1 + Math.random() * 0.2,
    });
  }

  function drawGraph(w: number, h: number) {
    ctx!.clearRect(0, 0, w, h);

    particles.forEach(p => {
      p.x += p.dx; p.y += p.dy;
      if (p.x < 0 || p.x > w) p.dx *= -1;
      if (p.y < 0 || p.y > h) p.dy *= -1;
      ctx!.beginPath();
      ctx!.arc(p.x, p.y, p.r, 0, Math.PI * 2);
      ctx!.fillStyle = `rgba(161, 161, 161, ${p.o})`;
      ctx!.fill();
    });

    if (Math.abs(w - 600) > 50 || Math.abs(h - 600) > 50) {
      const g = generateKnowledgeGraph(w, h);
      nodes = g.nodes;
      edges = g.edges;
    }

    const cx = w / 2, cy = h / 2 + 10;
    const centerNode = nodes[nodes.length - 1];
    centerNode.x = cx;
    centerNode.y = cy;

    edges.forEach((edge) => {
      const fromNode = nodes[edge.from];
      const toNode = nodes[edge.to];
      if (!fromNode || !toNode) return;
      edge.progress += edge.speed;
      if (edge.progress > 1) edge.progress = 0;

      ctx!.beginPath();
      ctx!.moveTo(fromNode.x, fromNode.y);
      ctx!.lineTo(toNode.x, toNode.y);
      ctx!.strokeStyle = edge.color;
      ctx!.lineWidth = 0.8;
      ctx!.stroke();

      const px = fromNode.x + (toNode.x - fromNode.x) * edge.progress;
      const py = fromNode.y + (toNode.y - fromNode.y) * edge.progress;
      ctx!.beginPath();
      ctx!.arc(px, py, 2, 0, Math.PI * 2);
      ctx!.fillStyle = "rgba(161, 161, 161, 0.6)";
      ctx!.fill();

      ctx!.beginPath();
      ctx!.arc(px, py, 4, 0, Math.PI * 2);
      ctx!.fillStyle = "rgba(161, 161, 161, 0.2)";
      ctx!.fill();
    });

    nodes.forEach((node) => {
      node.x += node.vx; node.y += node.vy;
      const m = 40;
      if (node.x < m || node.x > w - m) node.vx *= -1;
      if (node.y < m || node.y > h - m) node.vy *= -1;

      if (node.label === "KnowledgeNexus") {
        ctx!.beginPath();
        ctx!.arc(node.x, node.y, 14, 0, Math.PI * 2);
        ctx!.fillStyle = "rgba(161, 161, 161, 0.15)";
        ctx!.fill();
      }
      ctx!.beginPath();
      ctx!.arc(node.x, node.y, node.radius, 0, Math.PI * 2);
      ctx!.fillStyle = `rgba(161, 161, 161, ${node.opacity})`;
      ctx!.fill();

      if (node.radius > 3) {
        ctx!.font = "8px system-ui, sans-serif";
        ctx!.fillStyle = "rgba(161, 161, 161, 0.5)";
        ctx!.textAlign = "center";
        ctx!.fillText(node.label, node.x, node.y - node.radius - 4);
      }
    });

    const pulse = Math.sin((Date.now() - startTime) / 2000) * 0.3 + 0.7;
    ctx!.beginPath();
    ctx!.arc(cx, cy, 7 + (1 - pulse) * 4, 0, Math.PI * 2);
    ctx!.fillStyle = `rgba(161, 161, 161, ${0.2 * pulse})`;
    ctx!.fill();
  }

  function animate() {
    if (!canvas) return;
    drawGraph(canvas.width, canvas.height);
    animationFrameId = requestAnimationFrame(animate);
  }

  animate();
});

onUnmounted(() => {
  if (animationFrameId) cancelAnimationFrame(animationFrameId);
  if (resizeObserver) resizeObserver.disconnect();
});
</script>

<template>
  <div class="min-h-screen w-full flex items-center justify-center bg-gradient-to-br from-neutral-50 to-neutral-100 p-4 relative overflow-hidden">
    <!-- Background decorative blur elements -->
    <div class="absolute inset-0 overflow-hidden pointer-events-none">
      <div class="absolute -top-40 -right-40 w-80 h-80 rounded-full bg-neutral-200/30 blur-3xl"></div>
      <div class="absolute -bottom-40 -left-40 w-80 h-80 rounded-full bg-neutral-300/20 blur-3xl"></div>
    </div>

    <div
      class="w-full overflow-hidden rounded-2xl flex bg-white/80 backdrop-blur-sm shadow-2xl shadow-black/5 border border-neutral-100/60 relative"
      style="max-width: 960px;"
    >
      <!-- Left side - Knowledge Graph -->
      <div class="hidden lg:block w-1/2 h-[640px] relative overflow-hidden bg-gradient-to-br from-neutral-950 via-neutral-900 to-neutral-800">
        <canvas ref="canvasRef" class="absolute inset-0 w-full h-full" />
        <div class="absolute inset-0 bg-gradient-to-t from-neutral-950/60 via-transparent to-transparent pointer-events-none"></div>

        <div class="absolute inset-0 flex flex-col items-center justify-center p-10 z-10">
          <div
            class="mb-5 transition-all duration-700 ease-out"
            :class="formVisible ? 'opacity-100 translate-y-0' : 'opacity-0 -translate-y-4'"
          >
            <div class="h-14 w-14 rounded-xl bg-gradient-to-br from-neutral-500 to-neutral-700 flex items-center justify-center shadow-lg shadow-black/20 ring-1 ring-white/10">
              <BookOpen class="text-white h-7 w-7" />
            </div>
          </div>

          <h2
            class="text-2xl font-bold mb-2 text-center text-white transition-all duration-700 delay-100 ease-out"
            :class="formVisible ? 'opacity-100 translate-y-0' : 'opacity-0 -translate-y-4'"
          >
            RAG-KnowledgeNexus
          </h2>

          <p
            class="text-sm text-center text-neutral-300/80 max-w-xs leading-relaxed transition-all duration-700 delay-200 ease-out"
            :class="formVisible ? 'opacity-100 translate-y-0' : 'opacity-0 -translate-y-4'"
          >
            企业智能文档问答系统
          </p>

          <div
            class="mt-8 flex flex-col gap-2 w-full max-w-[220px] transition-all duration-700 delay-300 ease-out"
            :class="formVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-4'"
          >
            <div class="flex items-center gap-2.5 text-xs text-neutral-400/70">
              <div class="w-1.5 h-1.5 rounded-full bg-neutral-400/60"></div>
              <span>智能文档解析与分片</span>
            </div>
            <div class="flex items-center gap-2.5 text-xs text-neutral-400/70">
              <div class="w-1.5 h-1.5 rounded-full bg-neutral-400/60"></div>
              <span>语义向量检索与重排序</span>
            </div>
            <div class="flex items-center gap-2.5 text-xs text-neutral-400/70">
              <div class="w-1.5 h-1.5 rounded-full bg-neutral-400/60"></div>
              <span>大模型驱动的智能问答</span>
            </div>
          </div>

          <div
            class="absolute bottom-10 left-10 flex items-center gap-2 transition-all duration-700 delay-500 ease-out"
            :class="formVisible ? 'opacity-100' : 'opacity-0'"
          >
            <span class="relative flex h-2 w-2">
              <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>
              <span class="relative inline-flex rounded-full h-2 w-2 bg-green-500"></span>
            </span>
            <span class="text-xs text-neutral-400/60">系统运行中</span>
          </div>
        </div>
      </div>

      <!-- Right side - Sign In Form -->
      <div class="w-full lg:w-1/2 p-8 md:p-10 lg:p-12 flex flex-col justify-center bg-white">
        <div class="lg:hidden flex items-center gap-3 mb-8">
          <div class="h-9 w-9 rounded-lg bg-gradient-to-br from-neutral-700 to-neutral-900 flex items-center justify-center shadow-md">
            <BookOpen class="text-white h-5 w-5" />
          </div>
          <div>
            <h2 class="text-sm font-semibold text-gray-800">RAG-KnowledgeNexus</h2>
            <p class="text-xs text-gray-400">企业智能文档问答系统</p>
          </div>
        </div>

        <Transition
          enter-active-class="transition-all duration-500 ease-out"
          enter-from-class="opacity-0 translate-y-4"
          enter-to-class="opacity-100 translate-y-0"
        >
          <div v-if="formVisible">
            <h1 class="text-2xl md:text-3xl font-bold text-gray-900">欢迎回来</h1>
            <p class="text-gray-400 mt-1 mb-7">登录您的账号以继续</p>

            <button
              class="w-full flex items-center justify-center gap-2.5 bg-white border border-gray-200 rounded-xl py-3 hover:bg-gray-50 hover:border-gray-300 transition-all duration-200 text-gray-600 text-sm shadow-sm hover:shadow-md"
              @click="handleGoogleLogin"
            >
              <svg class="h-5 w-5 flex-shrink-0" viewBox="0 0 24 24">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
              </svg>
              <span>使用 Google 登录</span>
            </button>

            <div class="relative my-6">
              <div class="absolute inset-0 flex items-center">
                <div class="w-full border-t border-gray-100"></div>
              </div>
              <div class="relative flex justify-center text-xs">
                <span class="px-3 bg-white text-gray-300">或使用账号密码登录</span>
              </div>
            </div>

            <form @submit.prevent="handleLogin" class="space-y-4">
              <div>
                <label for="username" class="block text-sm font-medium text-gray-600 mb-1.5">
                  用户名 <span class="text-neutral-500">*</span>
                </label>
                <div class="relative group">
                  <input
                    id="username"
                    type="text"
                    v-model="username"
                    placeholder="请输入用户名"
                    required
                    autocomplete="username"
                    class="w-full h-11 rounded-xl border border-gray-200 bg-gray-50/80 pl-4 pr-4 text-sm text-gray-800 placeholder:text-gray-300 outline-none transition-all duration-200 focus:border-neutral-400 focus:bg-white focus:ring-4 focus:ring-neutral-100 group-hover:border-gray-300"
                  />
                </div>
              </div>

              <div>
                <label for="password" class="block text-sm font-medium text-gray-600 mb-1.5">
                  密码 <span class="text-neutral-500">*</span>
                </label>
                <div class="relative group">
                  <input
                    id="password"
                    :type="isPasswordVisible ? 'text' : 'password'"
                    v-model="password"
                    placeholder="请输入密码"
                    required
                    autocomplete="current-password"
                    class="w-full h-11 rounded-xl border border-gray-200 bg-gray-50/80 pl-4 pr-11 text-sm text-gray-800 placeholder:text-gray-300 outline-none transition-all duration-200 focus:border-neutral-400 focus:bg-white focus:ring-4 focus:ring-neutral-100 group-hover:border-gray-300"
                  />
                  <button
                    type="button"
                    class="absolute inset-y-0 right-0 flex items-center pr-3.5 text-gray-400 hover:text-gray-600 transition-colors"
                    @click="isPasswordVisible = !isPasswordVisible"
                    tabindex="-1"
                  >
                    <EyeOff v-if="isPasswordVisible" :size="17" />
                    <Eye v-else :size="17" />
                  </button>
                </div>
              </div>

              <div class="flex justify-end">
                <a href="#" class="text-xs text-neutral-500 hover:text-neutral-700 transition-colors" @click.prevent>
                  忘记密码？
                </a>
              </div>

              <div
                class="pt-1"
                @mouseenter="isHovered = true"
                @mouseleave="isHovered = false"
              >
                <button
                  type="submit"
                  :disabled="loading"
                  class="w-full h-11 inline-flex items-center justify-center gap-2 rounded-xl text-sm font-semibold relative overflow-hidden transition-all duration-300"
                  :class="[
                    loading
                      ? 'bg-neutral-400 text-white cursor-not-allowed'
                      : 'bg-gradient-to-r from-neutral-800 to-neutral-900 hover:from-neutral-900 hover:to-neutral-950 text-white shadow-md hover:shadow-lg hover:shadow-neutral-200/50',
                    isHovered && !loading ? 'shadow-lg shadow-neutral-200/50 scale-[1.01]' : ''
                  ]"
                >
                  <span class="flex items-center justify-center gap-2">
                    <span v-if="loading" class="inline-block h-4 w-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
                    {{ loading ? '登录中...' : '登录' }}
                    <ArrowRight v-if="!loading" :size="16" class="transition-transform duration-300" :class="isHovered ? 'translate-x-0.5' : ''" />
                  </span>
                  <span
                    v-if="isHovered && !loading"
                    class="absolute top-0 bottom-0 w-16 bg-gradient-to-r from-transparent via-white/25 to-transparent"
                    style="filter: blur(6px); animation: shine 1.2s ease-in-out infinite"
                  ></span>
                </button>
              </div>
            </form>

            <p class="text-center text-xs text-gray-400 mt-8">
              首次使用？
              <a
                href="#"
                class="text-neutral-600 hover:text-neutral-800 font-medium transition-colors"
                @click.prevent="resetRegisterForm(); showRegister = true"
              >
                创建账号
              </a>
            </p>
          </div>
        </Transition>
      </div>
    </div>

    <!-- ─── Register Dialog ────────────────────────────────────────────── -->
    <Transition name="dialog">
      <div v-if="showRegister" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="showRegister = false">
        <div class="absolute inset-0 bg-black/30 backdrop-blur-sm"></div>

        <div class="relative w-full max-w-md bg-white rounded-2xl shadow-2xl overflow-hidden animate-[fadeInUp_0.3s_ease-out]">
          <div class="flex items-center justify-between px-6 pt-6 pb-4 border-b border-gray-100">
            <div>
              <h3 class="text-lg font-semibold text-gray-800">创建账号</h3>
              <p class="text-xs text-gray-400 mt-0.5">注册后即可使用全部功能</p>
            </div>
            <button
              class="h-8 w-8 rounded-lg flex items-center justify-center text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
              @click="showRegister = false"
            >
              <X :size="18" />
            </button>
          </div>

          <form @submit.prevent="handleRegister" class="p-6 space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-600 mb-1.5">
                用户名 <span class="text-neutral-500">*</span>
              </label>
              <input
                type="text"
                v-model="registerForm.username"
                placeholder="请输入用户名"
                required
                class="w-full h-11 rounded-xl border border-gray-200 bg-gray-50/80 px-4 text-sm text-gray-800 placeholder:text-gray-300 outline-none transition-all duration-200 focus:border-neutral-400 focus:bg-white focus:ring-4 focus:ring-neutral-100"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-600 mb-1.5">
                密码 <span class="text-neutral-500">*</span>
              </label>
              <input
                type="password"
                v-model="registerForm.password"
                placeholder="至少6位密码"
                required
                minlength="6"
                class="w-full h-11 rounded-xl border border-gray-200 bg-gray-50/80 px-4 text-sm text-gray-800 placeholder:text-gray-300 outline-none transition-all duration-200 focus:border-neutral-400 focus:bg-white focus:ring-4 focus:ring-neutral-100"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-600 mb-1.5">
                姓名 <span class="text-gray-300 font-normal">(选填)</span>
              </label>
              <input
                type="text"
                v-model="registerForm.realName"
                placeholder="请输入真实姓名"
                class="w-full h-11 rounded-xl border border-gray-200 bg-gray-50/80 px-4 text-sm text-gray-800 placeholder:text-gray-300 outline-none transition-all duration-200 focus:border-neutral-400 focus:bg-white focus:ring-4 focus:ring-neutral-100"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-600 mb-1.5">
                邮箱 <span class="text-gray-300 font-normal">(选填)</span>
              </label>
              <input
                type="email"
                v-model="registerForm.email"
                placeholder="请输入邮箱地址"
                class="w-full h-11 rounded-xl border border-gray-200 bg-gray-50/80 px-4 text-sm text-gray-800 placeholder:text-gray-300 outline-none transition-all duration-200 focus:border-neutral-400 focus:bg-white focus:ring-4 focus:ring-neutral-100"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-600 mb-1.5">
                部门 <span class="text-gray-300 font-normal">(选填)</span>
              </label>
              <select
                v-model="registerForm.deptId"
                class="w-full h-11 rounded-xl border border-gray-200 bg-gray-50/80 px-4 text-sm text-gray-800 outline-none transition-all duration-200 focus:border-neutral-400 focus:bg-white focus:ring-4 focus:ring-neutral-100 appearance-none cursor-pointer"
                style="background-image: none; padding-right: 36px;"
              >
                <option :value="null" disabled selected>请选择所属部门</option>
                <option v-for="d in departments" :key="d.id" :value="d.id">{{ d.name }}</option>
              </select>
            </div>
            <button
              type="submit"
              :disabled="registerLoading"
              class="w-full h-11 inline-flex items-center justify-center gap-2 rounded-xl text-sm font-semibold bg-gradient-to-r from-neutral-800 to-neutral-900 hover:from-neutral-900 hover:to-neutral-950 text-white shadow-md transition-all duration-200 disabled:opacity-60 disabled:cursor-not-allowed"
            >
              <span v-if="registerLoading" class="inline-block h-4 w-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
              {{ registerLoading ? '注册中...' : '注册' }}
            </button>
          </form>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
@keyframes shine {
  from { left: -60%; }
  to { left: 160%; }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(16px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.dialog-enter-active,
.dialog-leave-active {
  transition: opacity 0.25s ease;
}
.dialog-enter-from,
.dialog-leave-to {
  opacity: 0;
}
</style>





