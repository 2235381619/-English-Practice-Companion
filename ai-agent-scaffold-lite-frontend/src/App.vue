<template>
  <div class="app-container">
    <nav class="nav-bar">
      <button :class="{ active: activeTab === 'chat' }" @click="activeTab = 'chat'">Chat</button>
      <button :class="{ active: activeTab === 'practice' }" @click="activeTab = 'practice'">Practice</button>
    </nav>
    <ChatView v-if="activeTab === 'chat'" />
    <PracticeView v-else />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import ChatView from './views/ChatView.vue'
import PracticeView from './views/PracticeView.vue'

const activeTab = ref('practice')
</script>

<style scoped>
.app-container { height: 100vh; display: flex; flex-direction: column; }
.nav-bar { display: flex; border-bottom: 1px solid #e0e0e0; background: #fafafa; flex-shrink: 0; }
.nav-bar button {
  flex: 1; padding: 12px; border: none; background: none;
  font-size: 14px; font-weight: 500; cursor: pointer; color: #666;
  border-bottom: 2px solid transparent; transition: all .2s;
}
.nav-bar button:hover { color: #333; }
.nav-bar button.active { color: #4a90d9; border-bottom-color: #4a90d9; background: #fff; }
</style>
