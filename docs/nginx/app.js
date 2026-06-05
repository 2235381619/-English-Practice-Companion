/**
 * AI Agent Scaffold Lite - 前端交互逻辑
 * 纯静态页面，通过 nginx 代理 /api/ 请求到后端
 */

// =============================================
// 配置
// =============================================
// 后端已开启 @CrossOrigin CORS，前端直接请求后端地址
const API_BASE = 'http://localhost:8091/api/v1';

// =============================================
// 状态
// =============================================

const state = {
  agents: [],
  selectedAgent: null,
  sessionId: '',
  userId: 'user_' + Date.now(),
  isStreaming: false,
  messages: []
};

// =============================================
// DOM 引用
// =============================================

const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => document.querySelectorAll(sel);

const els = {
  agentList: $('#agentList'),
  quickAgents: $('#quickAgents'),
  userId: $('#userId'),
  chatTitle: $('#chatTitle'),
  chatSubtitle: $('#chatSubtitle'),
  connectionStatus: $('#connectionStatus'),
  messagesArea: $('#messagesArea'),
  messageList: $('#messageList'),
  emptyState: $('#emptyState'),
  emptyDesc: $('#emptyDesc'),
  streamingIndicator: $('#streamingIndicator'),
  chatInput: $('#chatInput'),
  inputContainer: $('#inputContainer'),
  sendBtn: $('#sendBtn')
};

// =============================================
// 启动检测
// =============================================

// 直接请求后端地址（后端已开启 CORS），无需额外配置

// =============================================
// API 调用（统一错误处理）
// =============================================

async function apiGet(path) {
  const res = await fetch(API_BASE + path);
  if (!res.ok) throw new Error(`HTTP ${res.status} ${res.statusText}`);
  return res.json();
}

async function apiPost(path, body) {
  const res = await fetch(API_BASE + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  if (!res.ok) throw new Error(`HTTP ${res.status} ${res.statusText}`);
  return res.json();
}

async function loadAgents() {
  try {
    showStatus('加载智能体列表中...');
    const res = await apiGet('/query_aiagent_config_list');
    if (res.code === '0000' && res.data) {
      state.agents = res.data;
      renderAgentList();
      renderQuickAgents();
      hideStatus();
    } else {
      showStatus('加载失败: ' + (res.info || '响应格式异常'), true);
    }
  } catch (e) {
    console.error('加载智能体列表失败', e);
    showStatus('无法连接后端服务: ' + e.message + '。请确认后端已启动（端口 8091）且 nginx 代理配置正确。', true);
  }
}

async function createSession(agentId) {
  const res = await apiPost('/create_session', {
    agentId: agentId,
    userId: state.userId
  });
  if (res.code === '0000' && res.data) {
    return res.data.sessionId;
  }
  throw new Error(res.info || '创建会话失败');
}

// =============================================
// 流式对话（SSE）
// =============================================

function startStream(params, { onThinking, onResponse, onDone, onError }) {
  fetch(API_BASE + '/chat_stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(params)
  }).then(async (res) => {
    if (!res.ok) {
      onError(new Error(`HTTP ${res.status}`));
      return;
    }
    const reader = res.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    let pendingLength = null;
    let pendingType = null;

    function tryParse() {
      while (true) {
        if (pendingLength === null) {
          // 等待读取 header 行： T:123\n 或 R:456\n
          const nlIdx = buffer.indexOf('\n');
          if (nlIdx === -1) return;
          const header = buffer.slice(0, nlIdx);
          buffer = buffer.slice(nlIdx + 1);
          const colonIdx = header.indexOf(':');
          if (colonIdx === -1) return;
          pendingType = header.slice(0, colonIdx);
          pendingLength = parseInt(header.slice(colonIdx + 1), 10);
          if (isNaN(pendingLength)) { pendingLength = null; pendingType = null; return; }
        }

        // 等待 payload 完全到达
        if (buffer.length < pendingLength) return;

        // 取完整的 payload
        const payload = buffer.slice(0, pendingLength);
        buffer = buffer.slice(pendingLength);

        if (pendingType === 'T') {
          onThinking(payload);
        } else if (pendingType === 'R') {
          onResponse(payload);
        }

        pendingLength = null;
        pendingType = null;
      }
    }

    while (true) {
      const { done, value } = await reader.read();
      if (done) {
        onDone();
        break;
      }
      buffer += decoder.decode(value, { stream: true });
      tryParse();
    }
  }).catch((err) => {
    onError(err);
  });
}

// =============================================
// 状态提示
// =============================================

let statusEl = null;

function showStatus(msg, isError) {
  hideStatus();
  statusEl = document.createElement('div');
  statusEl.style.cssText = 'padding:12px 16px;margin:8px;border-radius:8px;font-size:13px;text-align:center;';
  if (isError) {
    statusEl.style.cssText += 'background:#fef2f2;color:#991b1b;border:1px solid #fecaca;';
  } else {
    statusEl.style.cssText += 'background:#f0f9ff;color:#1e40af;border:1px solid #bae6fd;';
  }
  statusEl.textContent = msg;

  // 插入到 agentList 顶部
  const firstChild = els.agentList.firstChild;
  if (firstChild) {
    els.agentList.insertBefore(statusEl, firstChild);
  } else {
    els.agentList.appendChild(statusEl);
  }
}

function hideStatus() {
  if (statusEl && statusEl.parentNode) {
    statusEl.parentNode.removeChild(statusEl);
    statusEl = null;
  }
}

// =============================================
// 渲染函数
// =============================================

function renderAgentList() {
  els.agentList.innerHTML = state.agents.map((agent) => `
    <div class="agent-item${state.selectedAgent?.agentId === agent.agentId ? ' active' : ''}"
         data-agent-id="${agent.agentId}">
      <div class="agent-avatar">${agent.agentName.charAt(0)}</div>
      <div class="agent-info">
        <div class="agent-name">${escapeHtml(agent.agentName)}</div>
        <div class="agent-desc">${escapeHtml(agent.agentDesc)}</div>
      </div>
    </div>
  `).join('');

  els.agentList.querySelectorAll('.agent-item').forEach((el) => {
    el.addEventListener('click', () => {
      const agent = state.agents.find((a) => a.agentId === el.dataset.agentId);
      if (agent) selectAgent(agent);
    });
  });
}

function renderQuickAgents() {
  if (els.quickAgents) {
    els.quickAgents.innerHTML = state.agents.map((agent) => `
      <button class="quick-agent-btn" data-agent-id="${agent.agentId}">
        <span class="quick-agent-avatar">${agent.agentName.charAt(0)}</span>
        <span class="quick-agent-name">${escapeHtml(agent.agentName)}</span>
      </button>
    `).join('');

    els.quickAgents.querySelectorAll('.quick-agent-btn').forEach((el) => {
      el.addEventListener('click', () => {
        const agent = state.agents.find((a) => a.agentId === el.dataset.agentId);
        if (agent) selectAgent(agent);
      });
    });
  }
}

function renderMessages() {
  els.messageList.innerHTML = state.messages.map((msg) => {
    let html = '';
    if (msg.role === 'assistant' && msg.thinkingContent) {
      html += `
        <details class="thinking-block">
          <summary class="thinking-summary">查看思考过程</summary>
          <div class="thinking-content">${renderContent(msg.thinkingContent)}</div>
        </details>`;
    }
    html += `
      <div class="message-wrapper ${msg.role}">
        <div class="message-content">
          <div class="message-text${msg.error ? ' error' : ''}">${renderContent(msg.content)}</div>
        </div>
      </div>`;
    return html;
  }).join('');
}

/** 将文本转换为 HTML（简单 Markdown 渲染） */
function renderContent(text) {
  if (!text) return '';
  return escapeHtml(text)
    .replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>');
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

// =============================================
// 核心交互
// =============================================

async function selectAgent(agent) {
  state.selectedAgent = agent;
  state.sessionId = '';
  state.messages = [];
  renderMessages();
  renderAgentList();

  // Update header
  els.chatTitle.textContent = agent.agentName;
  els.chatTitle.classList.remove('empty-title');
  els.chatSubtitle.textContent = agent.agentDesc;

  // Update empty state
  els.emptyDesc.textContent = `在下方输入消息，与「${agent.agentName}」开始对话`;
  els.emptyState.classList.remove('hidden');

  // Update connection status
  els.connectionStatus.className = 'connection-status';
  els.connectionStatus.querySelector('span:last-child').textContent = '连接中...';

  // Update input
  els.chatInput.placeholder = '输入消息，按 Enter 发送...';
  els.chatInput.disabled = false;
  els.inputContainer.classList.remove('is-disabled');

  try {
    state.sessionId = await createSession(agent.agentId);
    els.connectionStatus.className = 'connection-status connected';
    els.connectionStatus.querySelector('span:last-child').textContent = '已连接';
  } catch (e) {
    console.error('创建会话失败', e);
    els.connectionStatus.className = 'connection-status';
    els.connectionStatus.querySelector('span:last-child').textContent = '连接失败: ' + e.message;
  }
}

async function sendMessage(text) {
  if (!state.selectedAgent || !state.sessionId || state.isStreaming) return;

  // Add user message
  state.messages.push({ role: 'user', content: text });
  renderMessages();
  scrollToBottom();

  // Hide empty state
  els.emptyState.classList.add('hidden');

  // Add placeholder for assistant
  const assistantMsg = { role: 'assistant', thinkingContent: '', content: '' };
  state.messages.push(assistantMsg);
  renderMessages();
  state.isStreaming = true;
  els.streamingIndicator.classList.remove('hidden');
  updateSendBtn();
  scrollToBottom();

  const params = {
    agentId: state.selectedAgent.agentId,
    userId: state.userId,
    sessionId: state.sessionId,
    message: text
  };

  let streamFailed = false;

  startStream(params, {
    onThinking: (data) => {
      assistantMsg.thinkingContent += data;
      renderMessages();
      scrollToBottom();
    },
    onResponse: (data) => {
      assistantMsg.content += data;
      renderMessages();
      scrollToBottom();
    },
    onDone: () => {
      state.isStreaming = false;
      els.streamingIndicator.classList.add('hidden');
      updateSendBtn();
      scrollToBottom();
    },
    onError: (err) => {
      if (!streamFailed) {
        streamFailed = true;
        console.warn('流式对话失败，降级为普通对话', err);
        fallbackChat(params, assistantMsg);
      }
    }
  });
}

async function fallbackChat(params, assistantMsg) {
  try {
    const res = await apiPost('/chat', params);
    if (res.code === '0000' && res.data) {
      assistantMsg.content = res.data.content;
    } else {
      assistantMsg.content = '请求失败: ' + (res.info || '未知错误');
      assistantMsg.error = true;
    }
  } catch (e) {
    assistantMsg.content = '请求失败: ' + e.message;
    assistantMsg.error = true;
  }
  renderMessages();
  state.isStreaming = false;
  els.streamingIndicator.classList.add('hidden');
  updateSendBtn();
  scrollToBottom();
}

function scrollToBottom() {
  requestAnimationFrame(() => {
    els.messagesArea.scrollTop = els.messagesArea.scrollHeight;
  });
}

// =============================================
// UI 控制
// =============================================

function updateSendBtn() {
  const text = els.chatInput.value.trim();
  const disabled = !text || state.isStreaming || !state.selectedAgent;
  els.sendBtn.disabled = disabled;
  els.sendBtn.classList.toggle('has-text', !!text && !disabled);
}

// =============================================
// 事件绑定
// =============================================

els.chatInput.addEventListener('input', () => {
  updateSendBtn();
  els.chatInput.style.height = 'auto';
  els.chatInput.style.height = Math.min(els.chatInput.scrollHeight, 200) + 'px';
});

els.chatInput.addEventListener('keydown', (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    handleSend();
  }
});

els.sendBtn.addEventListener('click', handleSend);

function handleSend() {
  const text = els.chatInput.value.trim();
  if (!text || state.isStreaming || !state.selectedAgent) return;
  sendMessage(text);
  els.chatInput.value = '';
  els.chatInput.style.height = 'auto';
  updateSendBtn();
}

// =============================================
// 初始化
// =============================================

els.userId.textContent = state.userId;
updateSendBtn();
loadAgents();
