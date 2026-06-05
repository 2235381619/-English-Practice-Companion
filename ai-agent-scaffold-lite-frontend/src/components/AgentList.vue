<template>
  <aside class="sidebar">
    <div class="sidebar-header">
      <div class="logo">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"/>
          <path d="M12 6v6l4 2"/>
        </svg>
        <span>AI Agents</span>
      </div>
    </div>

    <div class="sidebar-section-label">智能体列表</div>

    <div class="agent-list">
      <div
        v-for="agent in agents"
        :key="agent.agentId"
        class="agent-item"
        :class="{ active: selectedAgent?.agentId === agent.agentId }"
        @click="$emit('select', agent)"
      >
        <div class="agent-avatar">
          {{ agent.agentName.charAt(0) }}
        </div>
        <div class="agent-info">
          <div class="agent-name">{{ agent.agentName }}</div>
          <div class="agent-desc">{{ agent.agentDesc }}</div>
        </div>
      </div>
    </div>

    <div class="sidebar-footer">
      <div v-if="userId" class="user-badge">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
          <circle cx="12" cy="7" r="4"/>
        </svg>
        <span>{{ userId }}</span>
      </div>
    </div>
  </aside>
</template>

<script setup>
defineProps({
  agents: { type: Array, default: () => [] },
  selectedAgent: { type: Object, default: null },
  userId: { type: String, default: '' }
})

defineEmits(['select'])
</script>

<style scoped>
.sidebar {
  width: var(--sidebar-width);
  min-width: var(--sidebar-width);
  height: 100%;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--color-border);
  background: var(--color-bg);
}

.sidebar-header {
  height: var(--header-height);
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-bottom: 1px solid var(--color-border);
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
  color: var(--color-text-primary);
}

.logo svg {
  color: var(--color-accent);
}

.sidebar-section-label {
  padding: 16px 16px 8px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-text-tertiary);
}

.agent-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 8px;
}

.agent-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 10px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.15s;
  margin-bottom: 2px;
}

.agent-item:hover {
  background: var(--color-bg-hover);
}

.agent-item.active {
  background: var(--color-accent-bg);
}

.agent-item.active .agent-name {
  color: var(--color-accent);
}

.agent-avatar {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: var(--color-bg-hover);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 13px;
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.agent-item.active .agent-avatar {
  background: var(--color-accent);
  color: white;
}

.agent-info {
  min-width: 0;
  flex: 1;
}

.agent-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.agent-desc {
  font-size: 11px;
  color: var(--color-text-tertiary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 1px;
}

.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--color-border);
}

.user-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.user-badge svg {
  opacity: 0.5;
}
</style>
