<template>
  <div class="scenario-selector">
    <h3 class="section-title">Choose a Scenario</h3>
    <div class="scenario-list">
      <div
        v-for="s in scenarios"
        :key="s.code"
        class="scenario-card"
        :class="{ active: selected === s.code }"
        @click="$emit('select', s.code)"
      >
        <span class="scenario-icon">{{ icons[s.code] }}</span>
        <span class="scenario-name">{{ s.name }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  scenarios: { type: Array, default: () => [] },
  selected: { type: String, default: '' }
})
defineEmits(['select'])

const icons = { interview: '💼', restaurant: '🍽️', meeting: '📋' }
</script>

<style scoped>
.scenario-selector { margin-bottom: 16px; }
.section-title { margin: 0 0 12px; font-size: 14px; color: #666; }
.scenario-list { display: flex; gap: 10px; }
.scenario-card {
  flex: 1; display: flex; flex-direction: column; align-items: center;
  gap: 6px; padding: 14px 12px; border-radius: 10px;
  border: 2px solid #e8e8e8; cursor: pointer;
  transition: all .2s; font-size: 13px;
}
.scenario-card:hover { border-color: #4a90d9; background: #f0f6ff; }
.scenario-card.active { border-color: #4a90d9; background: #e6f0ff; }
.scenario-icon { font-size: 28px; }
.scenario-name { font-weight: 500; color: #333; }
</style>
