<script setup>
import { ArrowRight, ChatDotRound, Connection, Delete, MagicStick, RefreshRight } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { computed, nextTick, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { chatWithAssistant, deleteChatSession, getChatSession, listChatSessions, resolveProductQuestion } from "../api/ai";
import { authState } from "../stores/auth";
import { formatDateTime } from "../utils/format";

const route = useRoute();
const router = useRouter();

const sessionId = ref("");
const question = ref("");
const sending = ref(false);
const resolvingProduct = ref(false);
const loadingSession = ref(false);
const loadingSessions = ref(false);
const deletingSessionId = ref("");
const records = ref([]);
const sessionSummaries = ref([]);
const latestMeta = ref(null);
const messagesViewport = ref(null);

const productContext = ref(null);
const pendingQuestion = ref("");
const candidateKeyword = ref("");
const candidateList = ref([]);

const answerPolicyMap = {
  FIXED_TEMPLATE: "固定模板回答",
  OUT_OF_SCOPE_REFUSAL: "范围外问题拒答",
  RAG_MODEL: "知识检索后生成回答",
  RAG_FALLBACK_NO_KNOWLEDGE: "降级回答",
  RAG_FALLBACK_MODEL_ERROR: "模型降级回答",
  REALTIME_ONLY: "仅实时数据回答"
};

const fallbackReasonMap = {
  NO_RELEVANT_KNOWLEDGE: "没有足够相关证据",
  KNOWLEDGE_NOT_READY: "知识库未就绪",
  EMBEDDING_UNAVAILABLE: "向量服务不可用",
  RETRIEVAL_UNAVAILABLE: "知识检索不可用",
  MODEL_UNAVAILABLE: "大模型不可用",
  OUT_OF_SCOPE: "问题超出服务范围",
  GREETING: "问候类问题",
  NO_DISCOVERY_RESULTS: "没有找到匹配商品"
};

const quickPrompts = computed(() => {
  if (productContext.value) {
    return [
      { label: "商品卖点", text: "这款商品的主要卖点是什么？" },
      { label: "适用人群", text: "这款商品更适合什么人？" },
      { label: "售后政策", text: "这款商品支持七天无理由退货吗？" },
      { label: "实时价格", text: "这款商品现在多少钱？还有库存吗？" }
    ];
  }
  return [
    { label: "iPhone", text: "iPhone 15 适合什么人？" },
    { label: "AirPods", text: "AirPods Pro 支持七天无理由退货吗？" },
    { label: "显示器", text: "4K 显示器一般适合什么场景？" },
    { label: "秒杀商品", text: "MacBook Pro M4 秒杀活动什么时候结束？" }
  ];
});

const capabilityItems = [
  "先按商品名或型号定位商品，再进入商品客服问答",
  "回答商品用途、卖点、规格、配送和售后规则",
  "库存、价格、秒杀时间优先参考实时信息",
  "没有明确商品时，会先让你确认候选商品，避免答非所问"
];

const currentProductLabel = computed(() => {
  if (!productContext.value) {
    return "";
  }
  const typeLabel = productContext.value.productType === "seckill" ? "秒杀商品" : "普通商品";
  return `${productContext.value.name} · ${typeLabel}`;
});

const statusCopy = computed(() => formatAnswerPolicy(latestMeta.value?.answerPolicy) || "等待首条问题");
const sourceCount = computed(() => latestMeta.value?.sources?.length || 0);
const canSend = computed(() => question.value.trim().length > 0 && !sending.value && !resolvingProduct.value);

const placeholderCopy = computed(() => {
  if (currentProductLabel.value) {
    return `已锁定 ${currentProductLabel.value}，现在可以直接问“这款商品适合什么人？”`;
  }
  return "例如：iPhone 15 适合什么人？AirPods Pro 支持七天无理由退货吗？";
});

const messageList = computed(() =>
  records.value.flatMap((record) => [
    {
      id: `question-${record.recordNo}`,
      role: "user",
      content: record.question,
      createdAt: record.createdAt
    },
    {
      id: `answer-${record.recordNo}`,
      role: "assistant",
      content: record.answer,
      createdAt: record.createdAt,
      sources: record.sources || [],
      confidence: record.confidence,
      answerPolicy: record.answerPolicy,
      fallbackReason: record.fallbackReason
    }
  ])
);

const groupedSessionSummaries = computed(() => {
  const groups = new Map([
    ["today", { key: "today", label: "今天", items: [] }],
    ["earlier", { key: "earlier", label: "更早", items: [] }]
  ]);

  sessionSummaries.value.forEach((summary) => {
    const activityTime = summary?.lastActiveAt || summary?.createdAt;
    const groupKey = isToday(activityTime) ? "today" : "earlier";
    groups.get(groupKey).items.push(summary);
  });

  return Array.from(groups.values()).filter((group) => group.items.length > 0);
});

watch(
  () => messageList.value.length,
  async () => {
    await nextTick();
    if (messagesViewport.value) {
      messagesViewport.value.scrollTop = messagesViewport.value.scrollHeight;
    }
  }
);

watch(
  () => [route.query.productId, route.query.productName, route.query.productType],
  () => syncRouteContext(true)
);

onMounted(() => {
  syncRouteContext(false);
  loadSessionSummaries();
});

function syncRouteContext(resetSession) {
  const productId = typeof route.query.productId === "string" ? Number(route.query.productId) : null;
  const productName = typeof route.query.productName === "string" ? route.query.productName.trim() : "";
  const productType = typeof route.query.productType === "string" ? route.query.productType.trim() : "normal";

  if (productId && Number.isFinite(productId) && productId > 0) {
    productContext.value = {
      productId,
      name: productName || `商品 #${productId}`,
      productType
    };
    if (resetSession) {
      resetConversation(false);
    }
    return;
  }

  if (resetSession) {
    productContext.value = null;
    resetConversation(false);
  }
}

function formatConfidence(value) {
  if (value === null || value === undefined || value === "") {
    return "--";
  }
  const number = Number(value);
  if (Number.isNaN(number)) {
    return "--";
  }
  return `${Math.round(number * 100)}%`;
}

function formatAnswerPolicy(value) {
  return value ? answerPolicyMap[value] || value : "";
}

function formatFallbackReason(value) {
  return value ? fallbackReasonMap[value] || value : "无";
}

function formatSessionTime(value) {
  if (!value) {
    return "--";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return formatDateTime(value);
  }

  const diff = Date.now() - date.getTime();
  if (diff < 60 * 1000) {
    return "刚刚";
  }
  if (diff < 60 * 60 * 1000) {
    return `${Math.max(Math.floor(diff / (60 * 1000)), 1)} 分钟前`;
  }
  if (diff < 24 * 60 * 60 * 1000) {
    return `${Math.max(Math.floor(diff / (60 * 60 * 1000)), 1)} 小时前`;
  }

  return formatDateTime(value);
}

function isToday(value) {
  if (!value) {
    return false;
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return false;
  }

  const now = new Date();
  return (
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate()
  );
}

function formatSessionId(value) {
  if (!value) {
    return "未开始";
  }
  return value.length > 12 ? `${value.slice(0, 8)}...${value.slice(-4)}` : value;
}

function getSessionTitle(summary) {
  return summary?.lastQuestion || summary?.currentProductName || "新会话";
}

function getSessionPreview(summary) {
  return summary?.lastAnswerSummary || "这条会话还没有生成摘要，点开后可以继续追问。";
}

function getSessionTypeLabel(summary) {
  return summary?.contextType === "product-detail" ? "商品问答" : "通用咨询";
}

function getSessionContextLabel(summary) {
  if (summary?.currentProductName) {
    return summary.currentProductName;
  }
  return summary?.contextType === "product-detail" ? "商品上下文已绑定" : "未绑定商品上下文";
}

function getSessionMessageCount(summary) {
  const count = Number(summary?.messageCount || 0);
  return `${count} 条消息`;
}

async function loadSessionSummaries(options = {}) {
  const { silent = false } = options;

  loadingSessions.value = true;
  try {
    sessionSummaries.value = (await listChatSessions(12)) || [];
  } catch (error) {
    if (!silent) {
      ElMessage.error(error.message || "聊天记录加载失败");
    }
  } finally {
    loadingSessions.value = false;
  }
}

function applyPrompt(prompt) {
  question.value = prompt;
}

function updateProductContext(candidate, resetSession = false) {
  productContext.value = {
    productId: candidate.productId,
    name: candidate.name,
    productType: candidate.productType || "normal"
  };
  if (resetSession) {
    resetConversation(false);
  }
  router.replace({
    name: "app-assistant",
    query: {
      productId: String(candidate.productId),
      productName: candidate.name,
      productType: candidate.productType || "normal",
      contextType: "product-detail"
    }
  });
}

async function sendMessage() {
  const trimmedQuestion = question.value.trim();
  if (!trimmedQuestion) {
    return;
  }

  candidateList.value = [];
  candidateKeyword.value = "";

  if (productContext.value?.productId) {
    await submitChat(trimmedQuestion, productContext.value.productId);
    return;
  }

  resolvingProduct.value = true;
  try {
    const resolution = await resolveProductQuestion(trimmedQuestion);
    candidateList.value = resolution.candidates || [];
    candidateKeyword.value = resolution.keyword || "";

    if (resolution.resolved && resolution.selectedCandidate) {
      updateProductContext(resolution.selectedCandidate);
      await submitChat(trimmedQuestion, resolution.selectedCandidate.productId);
      return;
    }

    if (candidateList.value.length === 1) {
      updateProductContext(candidateList.value[0]);
      await submitChat(trimmedQuestion, candidateList.value[0].productId);
      return;
    }

    if (candidateList.value.length > 1) {
      pendingQuestion.value = trimmedQuestion;
      question.value = "";
      ElMessage.info("先确认你想咨询的具体商品，我再继续回答。");
      return;
    }

    await submitChat(trimmedQuestion, null);
  } catch (error) {
    ElMessage.error(error.message || "商品解析失败，请稍后再试");
  } finally {
    resolvingProduct.value = false;
  }
}

async function submitChat(trimmedQuestion, productId) {
  sending.value = true;
  try {
    const activeProduct = productContext.value?.productId === productId ? productContext.value : null;
    const response = await chatWithAssistant({
      question: trimmedQuestion,
      productId: productId || undefined,
      productName: activeProduct?.name || undefined,
      productType: activeProduct?.productType || undefined,
      sessionId: sessionId.value || undefined,
      contextType: productId ? "product-detail" : "global-assistant"
    });

    sessionId.value = response.sessionId;
    latestMeta.value = response;
    question.value = "";
    pendingQuestion.value = "";
    await loadSession();
    await loadSessionSummaries({ silent: true });
  } catch (error) {
    ElMessage.error(error.message || "AI 回答失败，请稍后重试");
  } finally {
    sending.value = false;
  }
}

async function confirmCandidate(candidate) {
  updateProductContext(candidate);
  candidateList.value = [];

  const nextQuestion = pendingQuestion.value || question.value.trim();
  if (!nextQuestion) {
    return;
  }
  await submitChat(nextQuestion, candidate.productId);
}

async function loadSession() {
  if (!sessionId.value) {
    return;
  }

  loadingSession.value = true;
  try {
    const session = await getChatSession(sessionId.value);
    records.value = session.records || [];

    const contextState = session.contextState || {};
    if (contextState.currentProductId) {
      productContext.value = {
        productId: contextState.currentProductId,
        name: contextState.currentProductName || `商品 #${contextState.currentProductId}`,
        productType: contextState.currentProductType || "normal"
      };
    } else {
      productContext.value = null;
    }

    const latestRecord = records.value[records.value.length - 1];
    if (latestRecord) {
      latestMeta.value = {
        answerPolicy: latestRecord.answerPolicy,
        sources: latestRecord.sources || [],
        confidence: latestRecord.confidence,
        fallbackReason: latestRecord.fallbackReason
      };
    } else {
      latestMeta.value = null;
    }
  } catch (error) {
    ElMessage.error(error.message || "会话记录加载失败");
  } finally {
    loadingSession.value = false;
  }
}

async function openSession(summary) {
  if (!summary?.sessionId || summary.sessionId === sessionId.value) {
    return;
  }

  sessionId.value = summary.sessionId;
  question.value = "";
  pendingQuestion.value = "";
  candidateKeyword.value = "";
  candidateList.value = [];
  await loadSession();
}

async function removeSession(summary) {
  if (!summary?.sessionId) {
    return;
  }

  try {
    await ElMessageBox.confirm("删除后这条聊天记录将不再出现在历史列表中，是否继续？", "删除会话", {
      confirmButtonText: "删除",
      cancelButtonText: "取消",
      type: "warning"
    });
  } catch {
    return;
  }

  deletingSessionId.value = summary.sessionId;
  try {
    await deleteChatSession(summary.sessionId);
    sessionSummaries.value = sessionSummaries.value.filter((item) => item.sessionId !== summary.sessionId);

    if (summary.sessionId === sessionId.value) {
      resetConversation(false);
    }

    ElMessage.success("会话已删除");
    await loadSessionSummaries({ silent: true });
  } catch (error) {
    ElMessage.error(error.message || "删除会话失败");
  } finally {
    deletingSessionId.value = "";
  }
}

function clearProductContext() {
  productContext.value = null;
  candidateList.value = [];
  candidateKeyword.value = "";
  pendingQuestion.value = "";
  router.replace({ name: "app-assistant", query: {} });
}

function resetConversation(clearContext = true) {
  sessionId.value = "";
  records.value = [];
  latestMeta.value = null;
  question.value = "";
  pendingQuestion.value = "";
  candidateList.value = [];
  candidateKeyword.value = "";
  if (clearContext) {
    clearProductContext();
  }
}
</script>

<template>
  <div class="ai-page">
    <section class="ai-hero">
      <div class="ai-hero-copy">
        <p class="eyebrow">AI Assistant</p>
        <h1>AI 商品问答</h1>
        <p>
          先按商品名或型号定位商品，再进入问答。你不需要知道商品 ID，也不需要自己查数据库。
          如果命中多个候选商品，系统会先让你确认，尽量避免答非所问。
        </p>
      </div>

      <div class="ai-hero-status">
        <div>
          <small>当前用户</small>
          <strong>{{ authState.username || `用户 #${authState.userId || "--"}` }}</strong>
        </div>
        <div>
          <small>当前会话</small>
          <strong>{{ sessionId || "未开始" }}</strong>
        </div>
        <div>
          <small>回答策略</small>
          <strong>{{ statusCopy }}</strong>
        </div>
      </div>
    </section>

    <section class="ai-layout">
      <aside class="ai-sidebar">
        <div class="ai-panel ai-panel--history">
          <div class="ai-panel-head">
            <div>
              <p class="eyebrow">Chat History</p>
              <h3>聊天记录</h3>
            </div>
            <el-button text :icon="RefreshRight" :loading="loadingSessions" @click="loadSessionSummaries">
              刷新
            </el-button>
          </div>

          <div class="ai-history-summary">
            <span>最近 {{ sessionSummaries.length }} 条会话</span>
            <small>点击一条记录即可切换到对应上下文</small>
          </div>

          <div v-if="loadingSessions && !sessionSummaries.length" class="ai-history-skeleton">
            <div v-for="index in 4" :key="index" class="ai-history-skeleton-item" />
          </div>

          <div v-else-if="groupedSessionSummaries.length" class="ai-history-list">
            <section
              v-for="group in groupedSessionSummaries"
              :key="group.key"
              class="ai-history-group"
            >
              <div class="ai-history-group-head">
                <span>{{ group.label }}</span>
                <small>{{ group.items.length }} 条</small>
              </div>

              <article
                v-for="summary in group.items"
                :key="summary.sessionId"
                class="ai-history-item"
                :class="{ 'is-active': summary.sessionId === sessionId }"
              >
                <button
                  type="button"
                  class="ai-history-main"
                  :title="getSessionTitle(summary)"
                  @click="openSession(summary)"
                >
                  <div class="ai-history-topline">
                    <span class="ai-history-type">{{ getSessionTypeLabel(summary) }}</span>
                    <small>{{ formatSessionTime(summary.lastActiveAt || summary.createdAt) }}</small>
                  </div>
                  <strong>{{ getSessionTitle(summary) }}</strong>
                  <p>{{ getSessionPreview(summary) }}</p>
                  <div class="ai-history-meta">
                    <span>{{ getSessionContextLabel(summary) }}</span>
                    <span>{{ getSessionMessageCount(summary) }}</span>
                  </div>
                </button>

                <button
                  type="button"
                  class="ai-history-delete"
                  :disabled="deletingSessionId === summary.sessionId"
                  :title="deletingSessionId === summary.sessionId ? '删除中' : '删除会话'"
                  @click.stop="removeSession(summary)"
                >
                  <el-icon><Delete /></el-icon>
                </button>
              </article>
            </section>
          </div>

          <div v-else class="ai-history-empty">
            <div class="ai-history-empty-mark">记录</div>
            <h4>还没有历史会话</h4>
            <p>发出第一条问题后，最近聊天记录会自动出现在这里。</p>
          </div>
        </div>

        <div class="ai-panel ai-panel--soft">
          <div class="ai-panel-head">
            <div>
              <p class="eyebrow">Capabilities</p>
              <h3>主要能做什么</h3>
            </div>
            <el-icon><MagicStick /></el-icon>
          </div>
          <ul class="ai-capability-list">
            <li v-for="item in capabilityItems" :key="item">{{ item }}</li>
          </ul>
        </div>

        <div class="ai-panel">
          <div class="ai-panel-head">
            <div>
              <p class="eyebrow">Quick Ask</p>
              <h3>快捷提问</h3>
            </div>
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div class="ai-prompt-grid">
            <button
              v-for="prompt in quickPrompts"
              :key="prompt.label"
              type="button"
              class="ai-prompt-button"
              @click="applyPrompt(prompt.text)"
            >
              <span>{{ prompt.label }}</span>
              <small>{{ prompt.text }}</small>
            </button>
          </div>
        </div>

        <div class="ai-panel">
          <div class="ai-panel-head">
            <div>
              <p class="eyebrow">Evidence</p>
              <h3>当前回答概况</h3>
            </div>
            <el-icon><Connection /></el-icon>
          </div>
          <div class="ai-meta-grid">
            <div>
              <small>置信度</small>
              <strong>{{ formatConfidence(latestMeta?.confidence) }}</strong>
            </div>
            <div>
              <small>命中来源</small>
              <strong>{{ sourceCount }}</strong>
            </div>
            <div>
              <small>回退原因</small>
              <strong>{{ formatFallbackReason(latestMeta?.fallbackReason) }}</strong>
            </div>
          </div>
        </div>
      </aside>

      <section class="ai-chat-shell">
        <div class="ai-chat-toolbar">
          <div class="ai-toolbar-main">
            <div>
              <p class="eyebrow">Conversation</p>
              <h3>AI 商品问答</h3>
            </div>
            <span class="ai-session-pill">
              {{ sessionId ? `会话 ${sessionId}` : "等待首条消息创建会话" }}
            </span>
          </div>

          <div class="ai-toolbar-actions">
            <div v-if="currentProductLabel" class="ai-context-pill">
              已锁定 {{ currentProductLabel }}
            </div>
            <el-button v-if="currentProductLabel" plain @click="clearProductContext">切换商品</el-button>
            <el-button plain :icon="RefreshRight" @click="resetConversation(false)">新会话</el-button>
          </div>
        </div>

        <div ref="messagesViewport" class="ai-messages">
          <div v-if="!messageList.length && !candidateList.length" class="ai-empty">
            <div class="ai-empty-mark">AI</div>
            <h3>先说商品名，再说问题</h3>
            <p>
              比如直接问“iPhone 15 适合什么人”或“AirPods Pro 支持退货吗”。
              系统会先定位商品，再进入可靠的客服问答。
            </p>
          </div>

          <section v-if="candidateList.length" class="ai-candidate-panel">
            <div class="ai-candidate-head">
              <div>
                <p class="eyebrow">Match Candidates</p>
                <h3>先确认你想问哪件商品</h3>
              </div>
              <small v-if="candidateKeyword">解析关键词：{{ candidateKeyword }}</small>
            </div>
            <p class="ai-candidate-copy">
              {{ pendingQuestion || question }}
            </p>
            <div class="ai-candidate-grid">
              <button
                v-for="candidate in candidateList"
                :key="`${candidate.productType}-${candidate.productId}`"
                type="button"
                class="ai-candidate-button"
                @click="confirmCandidate(candidate)"
              >
                <strong>{{ candidate.name }}</strong>
                <small>{{ candidate.productType === "seckill" ? "秒杀商品" : "普通商品" }}</small>
              </button>
            </div>
          </section>

          <article
            v-for="message in messageList"
            :key="message.id"
            class="ai-message"
            :class="`is-${message.role}`"
          >
            <div class="ai-message-shell">
              <div class="ai-message-label">
                <span>{{ message.role === "user" ? "你" : "AI 助手" }}</span>
                <small>{{ formatSessionTime(message.createdAt) }}</small>
              </div>

              <div class="ai-message-bubble">
                <p>{{ message.content }}</p>

                <template v-if="message.role === 'assistant'">
                <div v-if="message.sources?.length" class="ai-message-sources">
                  <span v-for="source in message.sources" :key="source">{{ source }}</span>
                </div>
                <div class="ai-message-meta">
                  <small>策略 {{ formatAnswerPolicy(message.answerPolicy) || "--" }}</small>
                  <small>置信度 {{ formatConfidence(message.confidence) }}</small>
                  <small v-if="message.fallbackReason">回退 {{ formatFallbackReason(message.fallbackReason) }}</small>
                </div>
                </template>
              </div>
            </div>
          </article>
        </div>

        <div class="ai-composer">
          <el-input
            v-model="question"
            type="textarea"
            :rows="3"
            resize="none"
            :placeholder="placeholderCopy"
            @keyup.ctrl.enter="sendMessage"
          />
          <div class="ai-composer-actions">
            <small>Ctrl + Enter 发送</small>
            <el-button
              type="danger"
              :icon="ArrowRight"
              :loading="sending || resolvingProduct || loadingSession"
              :disabled="!canSend"
              @click="sendMessage"
            >
              发送
            </el-button>
          </div>
        </div>
      </section>
    </section>
  </div>
</template>

<style scoped>
.ai-page {
  display: grid;
  gap: 18px;
}

.ai-hero,
.ai-panel,
.ai-chat-shell {
  border: 1px solid var(--line);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: var(--shadow);
}

.ai-hero {
  padding: 26px 30px;
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) 360px;
  gap: 20px;
  background:
    radial-gradient(circle at top left, rgba(47, 125, 246, 0.10), transparent 24%),
    radial-gradient(circle at bottom right, rgba(255, 80, 0, 0.10), transparent 26%),
    rgba(255, 255, 255, 0.98);
}

.ai-hero-copy h1 {
  margin: 0;
  font-size: 42px;
  letter-spacing: -0.05em;
}

.ai-hero-copy p:last-child {
  margin: 14px 0 0;
  max-width: 700px;
  color: var(--muted);
  line-height: 1.85;
}

.ai-hero-status {
  display: grid;
  gap: 12px;
}

.ai-hero-status div,
.ai-meta-grid div {
  padding: 16px 18px;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff, #fbfcfe);
}

.ai-hero-status small,
.ai-meta-grid small {
  display: block;
  color: var(--muted);
  font-size: 12px;
}

.ai-hero-status strong,
.ai-meta-grid strong {
  display: block;
  margin-top: 10px;
  font-size: 18px;
  line-height: 1.4;
}

.ai-layout {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.ai-sidebar {
  display: grid;
  gap: 16px;
}

.ai-panel {
  padding: 20px;
}

.ai-panel--soft {
  background:
    radial-gradient(circle at top right, rgba(47, 125, 246, 0.08), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(250, 252, 255, 0.98));
}

.ai-panel--history {
  padding-bottom: 16px;
  background:
    radial-gradient(circle at top left, rgba(255, 80, 0, 0.08), transparent 24%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(255, 249, 246, 0.98));
}

.ai-panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.ai-panel-head h3,
.ai-chat-toolbar h3,
.ai-candidate-head h3 {
  margin: 0;
  font-size: 24px;
  letter-spacing: -0.04em;
}

.ai-panel-head .el-icon {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: rgba(255, 80, 0, 0.08);
  color: var(--brand-deep);
  font-size: 18px;
}

.ai-history-summary {
  margin-top: 16px;
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(255, 80, 0, 0.08);
  display: grid;
  gap: 4px;
}

.ai-history-summary span {
  font-weight: 700;
}

.ai-history-summary small {
  color: var(--muted);
}

.ai-history-list {
  margin-top: 14px;
  display: grid;
  gap: 10px;
  max-height: 472px;
  overflow-y: auto;
  padding-right: 4px;
}

.ai-history-group {
  display: grid;
  gap: 10px;
}

.ai-history-group-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 2px 2px 0;
}

.ai-history-group-head span {
  font-size: 13px;
  font-weight: 700;
  color: var(--ink);
}

.ai-history-group-head small {
  color: var(--muted);
  font-size: 12px;
}

.ai-history-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: start;
}

.ai-history-item.is-active .ai-history-main {
  border-color: rgba(255, 80, 0, 0.26);
  background: linear-gradient(180deg, #fffaf7, #ffffff);
  box-shadow: 0 12px 24px rgba(255, 80, 0, 0.08);
}

.ai-history-main {
  width: 100%;
  padding: 14px 15px;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: linear-gradient(180deg, #fff, #fcfcfd);
  text-align: left;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.ai-history-main:hover {
  transform: translateY(-2px);
  border-color: rgba(255, 80, 0, 0.18);
  box-shadow: 0 12px 24px rgba(17, 24, 39, 0.06);
}

.ai-history-topline,
.ai-history-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.ai-history-type {
  padding: 4px 8px;
  border-radius: 999px;
  background: #fff1eb;
  color: var(--brand-deep);
  font-size: 11px;
  font-weight: 700;
}

.ai-history-topline small,
.ai-history-meta span {
  color: var(--muted);
  font-size: 12px;
}

.ai-history-main strong {
  display: block;
  margin-top: 10px;
  font-size: 15px;
  line-height: 1.55;
}

.ai-history-main p {
  margin: 8px 0 0;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.ai-history-meta {
  margin-top: 12px;
}

.ai-history-delete {
  width: 40px;
  height: 40px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fff;
  color: var(--muted);
  cursor: pointer;
  transition:
    color 0.18s ease,
    border-color 0.18s ease,
    background 0.18s ease;
}

.ai-history-delete:hover:not(:disabled) {
  color: var(--brand-deep);
  border-color: rgba(255, 80, 0, 0.18);
  background: #fff4ed;
}

.ai-history-delete:disabled {
  cursor: wait;
  opacity: 0.65;
}

.ai-history-empty {
  margin-top: 14px;
  padding: 24px 16px 18px;
  border: 1px dashed rgba(17, 24, 39, 0.12);
  border-radius: 20px;
  display: grid;
  justify-items: center;
  text-align: center;
  gap: 10px;
}

.ai-history-empty-mark {
  padding: 8px 12px;
  border-radius: 999px;
  background: #fff1eb;
  color: var(--brand-deep);
  font-size: 12px;
  font-weight: 700;
}

.ai-history-empty h4 {
  margin: 0;
  font-size: 16px;
}

.ai-history-empty p {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.65;
}

.ai-history-skeleton {
  margin-top: 14px;
  display: grid;
  gap: 10px;
}

.ai-history-skeleton-item {
  height: 94px;
  border-radius: 18px;
  background:
    linear-gradient(90deg, rgba(244, 246, 248, 0.9), rgba(255, 255, 255, 0.96), rgba(244, 246, 248, 0.9));
  background-size: 240% 100%;
  animation: ai-history-shimmer 1.4s ease infinite;
}

.ai-capability-list {
  margin: 18px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 10px;
}

.ai-capability-list li {
  padding: 12px 14px;
  border-radius: 16px;
  background: var(--paper-soft);
  color: var(--ink);
  line-height: 1.6;
}

.ai-prompt-grid {
  margin-top: 18px;
  display: grid;
  gap: 10px;
}

.ai-prompt-button {
  padding: 14px 16px;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: linear-gradient(180deg, #fff, #fcfcfd);
  text-align: left;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.ai-prompt-button:hover,
.ai-candidate-button:hover {
  transform: translateY(-2px);
  border-color: rgba(255, 80, 0, 0.18);
  box-shadow: 0 12px 24px rgba(17, 24, 39, 0.06);
}

.ai-prompt-button span {
  display: block;
  font-weight: 700;
}

.ai-prompt-button small {
  display: block;
  margin-top: 6px;
  color: var(--muted);
  line-height: 1.55;
}

.ai-meta-grid {
  margin-top: 18px;
  display: grid;
  gap: 10px;
}

.ai-chat-shell {
  min-height: 760px;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  overflow: hidden;
}

.ai-chat-toolbar {
  padding: 22px 24px 18px;
  border-bottom: 1px solid var(--line);
  display: grid;
  gap: 16px;
}

.ai-toolbar-main,
.ai-toolbar-actions,
.ai-composer-actions,
.ai-candidate-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.ai-session-pill,
.ai-context-pill {
  padding: 10px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
}

.ai-session-pill {
  background: #f7f8fa;
  color: var(--muted);
}

.ai-context-pill {
  background: #fff4ed;
  color: var(--brand-deep);
}

.ai-messages {
  padding: 22px 24px;
  overflow-y: auto;
  display: grid;
  gap: 14px;
  background: linear-gradient(180deg, rgba(250, 251, 252, 0.86), rgba(255, 255, 255, 0.98));
}

.ai-empty {
  min-height: 420px;
  display: grid;
  place-content: center;
  justify-items: center;
  text-align: center;
  gap: 12px;
}

.ai-empty-mark {
  width: 74px;
  height: 74px;
  display: grid;
  place-items: center;
  border-radius: 22px;
  background: linear-gradient(135deg, var(--brand-deep), var(--gold));
  color: #fff;
  font-size: 26px;
  font-weight: 800;
}

.ai-empty h3 {
  margin: 0;
  font-size: 28px;
  letter-spacing: -0.04em;
}

.ai-empty p,
.ai-candidate-copy {
  max-width: 560px;
  margin: 0;
  color: var(--muted);
  line-height: 1.8;
}

.ai-candidate-panel {
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 10px 24px rgba(17, 24, 39, 0.05);
}

.ai-candidate-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.ai-candidate-button {
  padding: 14px 16px;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: linear-gradient(180deg, #fff, #fcfcfd);
  text-align: left;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.ai-candidate-button strong,
.ai-candidate-button small {
  display: block;
}

.ai-candidate-button small {
  margin-top: 8px;
  color: var(--muted);
}

.ai-message {
  display: flex;
}

.ai-message.is-user {
  justify-content: flex-end;
}

.ai-message.is-assistant {
  justify-content: flex-start;
}

.ai-message-shell {
  max-width: min(80%, 760px);
  display: grid;
  gap: 8px;
}

.ai-message.is-user .ai-message-label {
  justify-items: end;
}

.ai-message.is-assistant .ai-message-label {
  justify-items: start;
}

.ai-message-label {
  display: grid;
  gap: 2px;
}

.ai-message-label span {
  font-weight: 700;
  font-size: 13px;
}

.ai-message-label small {
  color: var(--muted);
  font-size: 12px;
}

.ai-message-bubble {
  padding: 16px 18px;
  border-radius: 22px;
  box-shadow: 0 10px 24px rgba(17, 24, 39, 0.05);
}

.ai-message.is-user .ai-message-bubble {
  background: linear-gradient(135deg, var(--brand-deep), #ff7b57);
  color: #fff;
  border-bottom-right-radius: 8px;
}

.ai-message.is-assistant .ai-message-bubble {
  background: #fff;
  border: 1px solid var(--line);
  border-bottom-left-radius: 8px;
}

.ai-message-bubble p {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.8;
}

.ai-message-extra {
  margin-top: 14px;
  display: grid;
  gap: 10px;
}

.ai-message-sources {
  margin-top: 14px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.ai-message-sources span {
  padding: 6px 10px;
  border-radius: 999px;
  background: #fff4ed;
  color: var(--brand-deep);
  font-size: 12px;
  font-weight: 600;
}

.ai-message-meta {
  margin-top: 12px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.ai-message-meta small,
.ai-composer-actions small {
  color: var(--muted);
  font-size: 12px;
}

.ai-composer {
  padding: 18px 24px 22px;
  border-top: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.98);
  display: grid;
  gap: 14px;
}

.ai-composer :deep(.el-textarea__inner) {
  border-radius: 20px;
  padding: 16px 18px;
  min-height: 110px !important;
  box-shadow: inset 0 0 0 1px rgba(17, 24, 39, 0.08);
}

@keyframes ai-history-shimmer {
  0% {
    background-position: 100% 0;
  }
  100% {
    background-position: -100% 0;
  }
}

@media (max-width: 1100px) {
  .ai-hero,
  .ai-layout {
    grid-template-columns: 1fr;
  }

  .ai-chat-shell {
    min-height: 680px;
  }

  .ai-history-list {
    max-height: 360px;
  }
}

@media (max-width: 720px) {
  .ai-message-shell {
    max-width: 100%;
  }

  .ai-history-item {
    grid-template-columns: minmax(0, 1fr);
  }

  .ai-history-delete {
    width: 100%;
    height: 38px;
  }
}
</style>
