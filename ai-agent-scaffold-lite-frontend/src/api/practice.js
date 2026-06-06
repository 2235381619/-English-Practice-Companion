const PRACTICE_BASE = '/api/v1/practice'

/**
 * 获取场景列表
 */
export async function listScenarios() {
  const res = await fetch(`${PRACTICE_BASE}/scenarios`)
  return res.json()
}

/**
 * 创建练习会话
 * @param {string} scenarioCode
 */
export async function createSession(scenarioCode) {
  const res = await fetch(`${PRACTICE_BASE}/session`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ scenarioCode })
  })
  return res.json()
}

/**
 * 获取会话信息
 * @param {string} sessionId
 */
export async function getSession(sessionId) {
  const res = await fetch(`${PRACTICE_BASE}/session/${sessionId}`)
  return res.json()
}

/**
 * 提交文本
 * @param {string} sessionId
 * @param {string} text
 */
export async function submitText(sessionId, text) {
  const params = new URLSearchParams({ sessionId, text })
  const res = await fetch(`${PRACTICE_BASE}/text`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: params
  })
  return res.json()
}

/**
 * 创建音频 WebSocket 连接
 *
 * 协议：(1) 连接后直接发送二进制 PCM 帧；(2) 发送文本 "END" 触发 ASR + 评测
 *
 * @param {string} sessionId
 * @param {object} callbacks - { onResult, onError, onOpen }
 * @returns {WebSocket}
 */
export function createAudioWs(sessionId, { onResult, onError, onOpen } = {}) {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsUrl = `${protocol}//${window.location.host}/practice/audio/${sessionId}`
  const ws = new WebSocket(wsUrl)
  ws.binaryType = 'arraybuffer'

  ws.onopen = () => onOpen?.()

  ws.onmessage = (event) => {
    if (typeof event.data === 'string') {
      try {
        const data = JSON.parse(event.data)
        if (data.error) {
          onError?.(data.error)
        } else {
          onResult?.(data)
        }
      } catch {
        onError?.('Parse error')
      }
    }
  }

  ws.onerror = () => onError?.('WebSocket connection error')
  return ws
}

/**
 * 发送 PCM 帧到 WebSocket
 * @param {WebSocket} ws
 * @param {Float32Array} float32Audio
 */
export function sendPcmFrame(ws, float32Audio) {
  if (!ws || ws.readyState !== WebSocket.OPEN) return
  const len = float32Audio.length
  const pcm16 = new Int16Array(len)
  for (let i = 0; i < len; i++) {
    const s = Math.max(-1, Math.min(1, float32Audio[i]))
    pcm16[i] = s < 0 ? s * 0x8000 : s * 0x7FFF
  }
  ws.send(pcm16.buffer)
}

/**
 * 获取课后报告
 * @param {string} sessionId
 */
export async function getReport(sessionId) {
  const res = await fetch(`${PRACTICE_BASE}/session/${sessionId}/report`)
  return res.json()
}

/**
 * 关闭会话
 * @param {string} sessionId
 */
export async function closeSession(sessionId) {
  const res = await fetch(`${PRACTICE_BASE}/session/${sessionId}`, {
    method: 'DELETE'
  })
  return res.json()
}