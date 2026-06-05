<template>
  <div class="message-wrapper" :class="role">
    <div class="message-avatar" :class="role">
      <svg v-if="role === 'assistant'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="10"/>
        <path d="M12 6v6l4 2"/>
      </svg>
      <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
        <circle cx="12" cy="7" r="4"/>
      </svg>
    </div>
    <div class="message-content">
      <div class="message-role-label">{{ role === 'user' ? '你' : name }}</div>
      <div class="message-text" v-html="renderedContent"></div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  role: { type: String, required: true },
  content: { type: String, default: '' },
  name: { type: String, default: 'AI' }
})

const renderedContent = computed(() => {
  // Simple markdown-like rendering: convert newlines to <br>, wrap code in backticks
  return props.content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
})
</script>

<style scoped>
.message-wrapper {
  display: flex;
  gap: 12px;
  padding: 16px 0;
}

.message-wrapper.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}

.message-avatar.assistant {
  background: var(--color-accent-bg);
  color: var(--color-accent);
}

.message-avatar.user {
  background: var(--color-bg-hover);
  color: var(--color-text-secondary);
}

.message-content {
  max-width: 75%;
  min-width: 0;
}

.message-wrapper.user .message-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.message-role-label {
  font-size: 11px;
  font-weight: 500;
  color: var(--color-text-tertiary);
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.message-text {
  font-size: 14px;
  line-height: 1.7;
  color: var(--color-text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

.message-wrapper.user .message-text {
  background: var(--color-accent-bg);
  color: var(--color-text-primary);
  padding: 10px 14px;
  border-radius: var(--radius-lg);
  border-bottom-right-radius: 4px;
}

.message-wrapper.assistant .message-text {
  padding-top: 2px;
}
</style>
