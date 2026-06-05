<template>
  <div class="conversation-panel" ref="panelRef">
    <div v-if="!rounds.length" class="empty-hint">
      Start speaking to begin the conversation
    </div>
    <div v-for="(r, i) in rounds" :key="i" class="round">
      <div class="msg user">
        <div class="bubble">{{ r.asrText || r.userInput }}</div>
      </div>
      <div v-if="r.evaluation" class="msg tutor">
        <div class="bubble tutor-bubble">
          <div class="reply-text">{{ r.evaluation.aiReply }}</div>
          <div v-if="r.evaluation.correctedText && r.evaluation.correctedText !== r.asrText" class="correction">
            <span class="label">✓ </span>{{ r.evaluation.correctedText }}
          </div>
          <div v-if="r.evaluation.grammarIssues?.length" class="issues">
            <div v-for="(issue, j) in r.evaluation.grammarIssues" :key="j" class="issue">• {{ issue }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
const props = defineProps({ rounds: { type: Array, default: () => [] } })
const panelRef = ref(null)
watch(() => props.rounds.length, async () => {
  await nextTick()
  if (panelRef.value) panelRef.value.scrollTop = panelRef.value.scrollHeight
})
</script>

<style scoped>
.conversation-panel { flex: 1; overflow-y: auto; padding: 12px 0; display: flex; flex-direction: column; gap: 16px; }
.empty-hint { text-align: center; color: #999; padding: 40px 0; font-size: 14px; }
.round { display: flex; flex-direction: column; gap: 8px; }
.msg { display: flex; }
.msg.user { justify-content: flex-end; }
.msg.tutor { justify-content: flex-start; }
.bubble { max-width: 80%; padding: 10px 14px; border-radius: 12px; font-size: 14px; line-height: 1.5; }
.user .bubble { background: #4a90d9; color: #fff; border-bottom-right-radius: 4px; }
.tutor-bubble { background: #f0f0f0; color: #333; border-bottom-left-radius: 4px; }
.reply-text { margin-bottom: 4px; }
.correction { font-size: 13px; color: #2e7d32; padding: 4px 0; border-top: 1px solid #ddd; margin-top: 6px; }
.label { font-weight: bold; }
.issues { font-size: 12px; color: #c62828; margin-top: 4px; }
.issue { padding: 1px 0; }
</style>
