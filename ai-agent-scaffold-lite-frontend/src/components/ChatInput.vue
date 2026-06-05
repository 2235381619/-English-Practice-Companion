<template>
  <div class="input-area">
    <div class="input-container" :class="{ 'is-disabled': disabled }">
      <textarea
        ref="textareaRef"
        v-model="inputText"
        class="chat-textarea"
        :placeholder="placeholder"
        :disabled="disabled"
        rows="1"
        @keydown.enter.exact="handleSend"
        @input="autoResize"
      />
      <button
        class="send-btn"
        :class="{ 'has-text': inputText.trim() && !disabled }"
        :disabled="!inputText.trim() || disabled"
        @click="handleSend"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="22" y1="2" x2="11" y2="13"/>
          <polygon points="22 2 15 22 11 13 2 9 22 2"/>
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  disabled: { type: Boolean, default: false },
  selectedAgent: { type: Object, default: null }
})

const emit = defineEmits(['send'])

const textareaRef = ref(null)
const inputText = ref('')

const placeholder = computed(() => {
  if (!props.selectedAgent) return '请先在左侧选择一个智能体...'
  if (props.disabled) return '正在回复中...'
  return '输入消息，按 Enter 发送...'
})

function handleSend() {
  const text = inputText.value.trim()
  if (!text || props.disabled) return
  emit('send', text)
  inputText.value = ''
  autoResize()
}

function autoResize() {
  const el = textareaRef.value
  if (el) {
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 200) + 'px'
  }
}
</script>

<style scoped>
.input-area {
  padding: 12px 20px 20px;
  border-top: 1px solid var(--color-border);
  background: var(--color-bg);
}

.input-container {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 8px 8px 8px 14px;
  background: var(--color-bg);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.input-container:focus-within {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.input-container.is-disabled {
  opacity: 0.6;
}

.chat-textarea {
  flex: 1;
  resize: none;
  font-size: 14px;
  line-height: 1.5;
  color: var(--color-text-primary);
  background: transparent;
  max-height: 200px;
  padding: 4px 0;
}

.chat-textarea::placeholder {
  color: var(--color-text-tertiary);
}

.chat-textarea:disabled {
  cursor: not-allowed;
}

.send-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary);
  transition: all 0.15s;
  flex-shrink: 0;
}

.send-btn.has-text {
  background: var(--color-accent);
  color: white;
}

.send-btn.has-text:hover {
  background: var(--color-accent-hover);
}

.send-btn:disabled {
  cursor: not-allowed;
}
</style>
