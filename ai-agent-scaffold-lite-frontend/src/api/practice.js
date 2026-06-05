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
 * 提交音频文件
 * @param {string} sessionId
 * @param {Blob|File} audioBlob
 */
export async function submitAudio(sessionId, audioBlob) {
  const formData = new FormData()
  formData.append('sessionId', sessionId)
  formData.append('file', audioBlob, 'audio.wav')
  const res = await fetch(`${PRACTICE_BASE}/audio`, {
    method: 'POST',
    body: formData
  })
  return res.json()
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
