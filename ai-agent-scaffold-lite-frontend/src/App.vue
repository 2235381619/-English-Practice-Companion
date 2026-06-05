<template>
  <div class="app-container">
    <ChatView />
  </div>
</template>

<script setup>
import ChatView from './views/ChatView.vue'
</script>

<style scoped>
.app-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}
</style>
