<template>
  <div class="chat-layout">
    <!-- Sidebar -->
    <AgentList
      :agents="agents"
      :selectedAgent="selectedAgent"
      :userId="userId"
      @select="handleSelectAgent"
    />

    <!-- Main Chat Area -->
    <main class="main-area">
      <!-- Header -->
      <header class="chat-header">
        <div class="chat-header-left">
          <h1 v-if="selectedAgent" class="chat-title">{{ selectedAgent.agentName }}</h1>
          <h1 v-else class="chat-title empty-title">AI Agent 智能体交互平台</h1>
          <span v-if="selectedAgent" class="chat-subtitle">{{ selectedAgent.agentDesc }}</span>
        </div>
        <div class="chat-header-right">
          <div class="connection-status" :class="{ connected: sessionId }">
            <span class="status-dot"></span>
            <span>{{ sessionId ? '已连接' : '未连接' }}</span>
          </div>
        </div>
      </header>

      <!-- Messages -->
      <div class="messages-area" ref="messagesRef">
        <!-- Empty state -->
        <div v-if="messages.length === 0" class="empty-state">
          <div class="empty-icon">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 6v6l4 2"/>
            </svg>
          </div>
          <h2 class="empty-title">开始对话</h2>
          <p class="empty-desc" v-if="selectedAgent">
            在下方输入消息，与「{{ selectedAgent.agentName }}」开始对话
          </p>
          <p class="empty-desc" v-else>
            从左侧选择一个智能体开始对话
          </p>

          <div v-if="agents.length > 0 && !selectedAgent" class="quick-agents">
            <button
              v-for="agent in agents"
              :key="agent.agentId"
              class="quick-agent-btn"
              @click="handleSelectAgent(agent)"
            >
              <span class="quick-agent-avatar">{{ agent.agentName.charAt(0) }}</span>
              <span class="quick-agent-name">{{ agent.agentName }}</span>
            </button>
          </div>
        </div>

        <!-- Message list -->
        <div v-for="(msg, idx) in messages" :key="idx">
          <ChatMessage
            :role="msg.role"
            :content="msg.content"
            :name="selectedAgent?.agentName || 'AI'"
          />
        </div>

        <!-- Streaming indicator -->
        <div v-if="isStreaming" class="streaming-indicator">
          <div class="streaming-dot"></div>
          <div class="streaming-dot"></div>
          <div class="streaming-dot"></div>
        </div>
      </div>

      <!-- Input -->
      <ChatInput
        :disabled="isStreaming || !selectedAgent"
        :selectedAgent="selectedAgent"
        @send="handleSend"
      />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import AgentList from '../components/AgentList.vue'
import ChatMessage from '../components/ChatMessage.vue'
import ChatInput from '../components/ChatInput.vue'
import { queryAgentConfigList, createSession, chat, chatStream } from '../api/index.js'

const agents = ref([])
const selectedAgent = ref(null)
const sessionId = ref('')
const userId = ref('user_' + Date.now())
const messages = ref([])
const isStreaming = ref(false)
const messagesRef = ref(null)

// Load agent list on mount
onMounted(async () => {
  try {
    const res = await queryAgentConfigList()
    if (res.code === '0000' && res.data) {
      agents.value = res.data
    }
  } catch (e) {
    console.error('Failed to load agents', e)
  }
})

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

async function handleSelectAgent(agent) {
  selectedAgent.value = agent
  sessionId.value = ''
  messages.value = []

  try {
    const res = await createSession(agent.agentId, userId.value)
    if (res.code === '0000' && res.data) {
      sessionId.value = res.data.sessionId
    }
  } catch (e) {
    console.error('Failed to create session', e)
  }
}

async function handleSend(text) {
  if (!selectedAgent.value || !sessionId.value || isStreaming.value) return

  // Add user message
  messages.value.push({ role: 'user', content: text })
  scrollToBottom()

  // Add placeholder for assistant
  const assistantMsg = { role: 'assistant', content: '' }
  messages.value.push(assistantMsg)
  isStreaming.value = true

  try {
    const params = {
      agentId: selectedAgent.value.agentId,
      userId: userId.value,
      sessionId: sessionId.value,
      message: text
    }

    await chatStream(params, {
      onMessage: (data) => {
        assistantMsg.content += data
        scrollToBottom()
      },
      onDone: () => {
        isStreaming.value = false
        scrollToBottom()
      },
      onError: (err) => {
        console.error('Stream error', err)
        assistantMsg.content = '**Error:** ' + err.message
        isStreaming.value = false
        scrollToBottom()
      }
    })
  } catch (e) {
    // Fallback to non-streaming chat
    try {
      const res = await chat(params)
      if (res.code === '0000' && res.data) {
        assistantMsg.content = res.data.content
      } else {
        assistantMsg.content = '**Error:** ' + (res.info || 'Unknown error')
      }
    } catch (e2) {
      assistantMsg.content = '**Error:** Request failed - ' + e2.message
    }
    isStreaming.value = false
    scrollToBottom()
  }
}
</script>

<style scoped>
.chat-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* Main Area */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--color-bg);
}

/* Header */
.chat-header {
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.chat-header-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
  min-width: 0;
}

.chat-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-title.empty-title {
  color: var(--color-text-secondary);
  font-weight: 500;
}

.chat-subtitle {
  font-size: 12px;
  color: var(--color-text-tertiary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-header-right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: var(--color-text-tertiary);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-text-tertiary);
}

.connection-status.connected .status-dot {
  background: var(--color-success);
}

.connection-status.connected {
  color: var(--color-success);
}

/* Messages Area */
.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 8px 24px 0;
}

/* Empty State */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  padding: 40px;
}

.empty-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: var(--color-bg-hover);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary);
  margin-bottom: 16px;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 6px;
}

.empty-desc {
  font-size: 14px;
  color: var(--color-text-tertiary);
  max-width: 400px;
}

.quick-agents {
  display: flex;
  gap: 8px;
  margin-top: 20px;
  flex-wrap: wrap;
  justify-content: center;
}

.quick-agent-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--color-text-primary);
  transition: all 0.15s;
}

.quick-agent-btn:hover {
  border-color: var(--color-accent);
  background: var(--color-accent-bg);
  color: var(--color-accent);
}

.quick-agent-avatar {
  width: 24px;
  height: 24px;
  border-radius: 5px;
  background: var(--color-bg-hover);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 11px;
}

.quick-agent-btn:hover .quick-agent-avatar {
  background: var(--color-accent);
  color: white;
}

/* Streaming Indicator */
.streaming-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 0 20px;
  margin-left: 40px;
}

.streaming-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-accent);
  animation: streaming-pulse 1.4s ease-in-out infinite;
}

.streaming-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.streaming-dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes streaming-pulse {
  0%, 80%, 100% {
    opacity: 0.3;
    transform: scale(0.8);
  }
  40% {
    opacity: 1;
    transform: scale(1.1);
  }
}
</style>
