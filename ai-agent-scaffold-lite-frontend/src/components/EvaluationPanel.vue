<template>
  <div v-if="evalResult" class="evaluation-panel">
    <div class="score-row">
      <span class="score-label">Score</span>
      <span class="score-value" :class="scoreClass">{{ evalResult.score }}/10</span>
    </div>
    <div v-if="evalResult.grammarIssues?.length" class="section">
      <div class="section-title">Grammar Issues</div>
      <div v-for="(issue, i) in evalResult.grammarIssues" :key="i" class="issue-item">{{ issue }}</div>
    </div>
    <div v-if="evalResult.suggestions?.length" class="section">
      <div class="section-title">Suggestions</div>
      <div v-for="(s, i) in evalResult.suggestions" :key="i" class="suggestion-item">{{ s }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ evalResult: { type: Object, default: null } })
const scoreClass = computed(() => {
  if (!props.evalResult) return ''
  const s = props.evalResult.score
  if (s >= 8) return 'high'
  if (s >= 5) return 'mid'
  return 'low'
})
</script>

<style scoped>
.evaluation-panel { padding: 12px; border-radius: 10px; background: #fafafa; border: 1px solid #eee; display: flex; flex-direction: column; gap: 10px; }
.score-row { display: flex; justify-content: space-between; align-items: center; }
.score-label { font-size: 13px; color: #666; }
.score-value { font-size: 20px; font-weight: bold; }
.score-value.high { color: #2e7d32; }
.score-value.mid { color: #f57f17; }
.score-value.low { color: #c62828; }
.section-title { font-size: 13px; font-weight: 600; color: #555; margin-bottom: 4px; }
.issue-item { font-size: 12px; color: #c62828; padding: 2px 0; }
.suggestion-item { font-size: 12px; color: #2e7d32; padding: 2px 0; }
</style>
