<template>
  <div class="practice-view">
    <!-- Header -->
    <div class="header">
      <h2>English Practice</h2>
      <button v-if="sessionId" class="btn-close" @click="endSession">End Session</button>
    </div>

    <!-- Scenario selector (before session starts) -->
    <ScenarioSelector
      v-if="!sessionId"
      :scenarios="scenarios"
      :selected="selectedScenario"
      @select="startSession"
    />

    <!-- Main content (during session) -->
    <template v-if="sessionId">
      <div class="main-area">
        <div class="chat-area">
          <ConversationPanel :rounds="rounds" />
        </div>
        <div class="eval-sidebar" v-if="latestEval">
          <h4 class="sidebar-title">Latest Evaluation</h4>
          <EvaluationPanel :eval-result="latestEval" />
        </div>
      </div>

      <!-- Input area -->
      <div class="input-area">
        <input
          v-model="textInput"
          class="text-input"
          placeholder="Type your message..."
          @keyup.enter="sendText"
          :disabled="loading"
        />
        <button class="btn-mic" :class="{ recording }" @click="toggleMic" :title="recording ? 'Stop recording' : 'Start recording'">
          {{ recording ? '⏹' : '🎤' }}
        </button>
        <button class="btn-send" @click="sendText" :disabled="loading || !textInput.trim()">Send</button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import ScenarioSelector from '../components/ScenarioSelector.vue'
import ConversationPanel from '../components/ConversationPanel.vue'
import EvaluationPanel from '../components/EvaluationPanel.vue'
import { listScenarios, createSession, submitText, submitAudio, closeSession } from '../api/practice.js'

const scenarios = ref([])
const selectedScenario = ref('')
const sessionId = ref('')
const rounds = ref([])
const latestEval = ref(null)
const textInput = ref('')
const loading = ref(false)
const recording = ref(false)

let mediaRecorder = null
let audioChunks = []

onUnmounted(() => stopMic())

// Load scenarios on mount
listScenarios().then(res => {
  if (res.data?.scenarios) scenarios.value = res.data.scenarios
})

async function startSession(code) {
  selectedScenario.value = code
  loading.value = true
  const res = await createSession(code)
  loading.value = false
  if (res.data?.sessionId) {
    sessionId.value = res.data.sessionId
    rounds.value = []
    latestEval.value = null
  }
}

async function sendText() {
  const text = textInput.value.trim()
  if (!text || !sessionId.value) return
  textInput.value = ''
  loading.value = true
  const res = await submitText(sessionId.value, text)
  loading.value = false
  handleResponse(res.data)
}

function handleResponse(data) {
  if (!data) return
  const evalData = {
    asrText: data.asrText || '',
    aiReply: data.aiReply || '',
    score: data.score || 0,
    correctedText: data.correctedText,
    grammarIssues: data.grammarIssues || [],
    suggestions: data.suggestions || []
  }
  latestEval.value = evalData
  rounds.value.push({
    userInput: data.asrText || '',
    asrText: data.asrText || '',
    evaluation: evalData
  })
}

async function toggleMic() {
  if (recording.value) {
    stopRecording()
  } else {
    await startRecording()
  }
}

async function startRecording() {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    audioChunks = []
    mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm' })
    mediaRecorder.ondataavailable = e => { if (e.data.size > 0) audioChunks.push(e.data) }
    mediaRecorder.onstop = async () => {
      stream.getTracks().forEach(t => t.stop())
      const blob = new Blob(audioChunks, { type: 'audio/webm' })
      if (blob.size > 1000 && sessionId.value) {
        loading.value = true
        const res = await submitAudio(sessionId.value, blob)
        loading.value = false
        handleResponse(res.data)
      }
    }
    mediaRecorder.start()
    recording.value = true
  } catch (e) {
    console.warn('Mic error:', e)
  }
}

function stopRecording() {
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }
  recording.value = false
}

function stopMic() {
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stream?.getTracks().forEach(t => t.stop())
    mediaRecorder.stop()
  }
  recording.value = false
}

async function endSession() {
  await closeSession(sessionId.value)
  sessionId.value = ''
  rounds.value = []
  latestEval.value = null
  selectedScenario.value = ''
}
</script>

<style scoped>
.practice-view { display: flex; flex-direction: column; height: 100%; padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.header h2 { margin: 0; font-size: 18px; }
.btn-close { padding: 6px 14px; border: 1px solid #ddd; border-radius: 8px; background: #fff; cursor: pointer; font-size: 13px; }
.btn-close:hover { background: #fff0f0; border-color: #e57373; color: #c62828; }
.main-area { flex: 1; display: flex; gap: 12px; min-height: 0; }
.chat-area { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.eval-sidebar { width: 280px; flex-shrink: 0; overflow-y: auto; }
.sidebar-title { margin: 0 0 8px; font-size: 14px; color: #666; }
.input-area { display: flex; gap: 8px; padding-top: 12px; }
.text-input { flex: 1; padding: 10px 14px; border: 2px solid #e0e0e0; border-radius: 10px; font-size: 14px; outline: none; }
.text-input:focus { border-color: #4a90d9; }
.btn-mic { width: 42px; height: 42px; border-radius: 50%; border: 2px solid #e0e0e0; background: #fff; cursor: pointer; font-size: 18px; display: flex; align-items: center; justify-content: center; transition: all .2s; }
.btn-mic:hover { border-color: #4a90d9; }
.btn-mic.recording { border-color: #e53935; background: #ffebee; animation: pulse 1.5s infinite; }
@keyframes pulse { 0%, 100% { box-shadow: 0 0 0 0 rgba(229,57,53,.3); } 50% { box-shadow: 0 0 0 8px rgba(229,57,53,0); } }
.btn-send { padding: 10px 20px; border: none; border-radius: 10px; background: #4a90d9; color: #fff; font-size: 14px; cursor: pointer; }
.btn-send:disabled { opacity: .5; cursor: not-allowed; }
.btn-send:hover:not(:disabled) { background: #357abd; }
</style>


