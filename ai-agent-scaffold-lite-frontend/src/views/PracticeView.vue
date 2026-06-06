<template>
  <div class="practice-view">
    <div class="header">
      <h2>English Practice</h2>
      <button v-if="sessionId" class="btn-close" @click="endSession">End Session</button>
    </div>

    <ScenarioSelector
      v-if="!sessionId"
      :scenarios="scenarios"
      :selected="selectedScenario"
      @select="startSession"
    />

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
import { listScenarios, createSession, submitText, closeSession, createAudioWs } from '../api/practice.js'

const scenarios = ref([])
const selectedScenario = ref('')
const sessionId = ref('')
const rounds = ref([])
const latestEval = ref(null)
const textInput = ref('')
const loading = ref(false)
const recording = ref(false)
let recordState = null
let audioWs = null

onUnmounted(() => stopMic())

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

// ────────── 录音（参考 bailing 模式）──────────
async function startRecording() {
  try {
    // 1. 先打开 WebSocket（连接是异步的，不阻塞）
    const ws = createAudioWs(sessionId.value, {
      onResult: (data) => {
        loading.value = false
        handleResponse(data)
        if (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING) {
          ws.close()
        }
        audioWs = null
      },
      onError: (err) => {
        loading.value = false
        console.warn('WS error:', err)
      }
    })
    audioWs = ws

    // 2. 获取麦克风
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })

    // 3. 创建 AudioContext（浏览器原生采样率，后续手动降采样到 16kHz）
    const audioCtx = new (window.AudioContext || window.webkitAudioContext)()
    const source = audioCtx.createMediaStreamSource(stream)
    const processor = audioCtx.createScriptProcessor(4096, 1, 1)

    const inputSampleRate = audioCtx.sampleRate // 通常是 48000
    const outputSampleRate = 16000

    // 降采样（平均值法，同 bailing）
    function downsample(buffer, fromRate, toRate) {
      if (fromRate === toRate) return buffer
      const ratio = fromRate / toRate
      const newLen = Math.round(buffer.length / ratio)
      const result = new Float32Array(newLen)
      let offsetResult = 0
      let offsetBuffer = 0
      while (offsetResult < result.length) {
        const nextOffset = Math.round((offsetResult + 1) * ratio)
        let accum = 0, count = 0
        for (let i = offsetBuffer; i < nextOffset && i < buffer.length; i++) {
          accum += buffer[i]
          count++
        }
        result[offsetResult] = count > 0 ? accum / count : 0
        offsetResult++
        offsetBuffer = nextOffset
      }
      return result
    }

    // Float32 → Int16 PCM
    function floatTo16bitPCM(input) {
      const out = new Int16Array(input.length)
      for (let i = 0; i < input.length; i++) {
        const s = Math.max(-1, Math.min(1, input[i]))
        out[i] = s < 0 ? s * 0x8000 : s * 0x7FFF
      }
      return out
    }

    // 4. 持续处理音频：降采样 → Int16 → 二进制发送
    processor.onaudioprocess = (e) => {
      const input = e.inputBuffer.getChannelData(0)
      const downsampled = downsample(input, inputSampleRate, outputSampleRate)
      const pcm16 = floatTo16bitPCM(downsampled)
      if (audioWs && audioWs.readyState === WebSocket.OPEN) {
        audioWs.send(pcm16.buffer)
      }
    }

    source.connect(processor)
    processor.connect(audioCtx.destination)

    recordState = {
      audioCtx, source, processor, stream,
      close() {
        try { source.disconnect() } catch (e) {}
        try { processor.disconnect() } catch (e) {}
        audioCtx.close()
        stream.getTracks().forEach(t => t.stop())
      }
    }
    recording.value = true
  } catch (e) {
    console.warn('Mic error:', e)
  }
}

function stopRecording() {
  if (!recordState) return
  recordState.close()
  recordState = null
  recording.value = false

  // 发送 END 触发后端处理
  if (audioWs && audioWs.readyState === WebSocket.OPEN) {
    loading.value = true
    audioWs.send('END')
  }
}

function stopMic() {
  if (recordState) { recordState.close(); recordState = null }
  if (audioWs) {
    if (audioWs.readyState === WebSocket.OPEN || audioWs.readyState === WebSocket.CONNECTING) {
      audioWs.close()
    }
    audioWs = null
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