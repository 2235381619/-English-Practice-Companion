const BASE_URL = '/api/v1'

/**
 * 查询智能体配置列表
 */
export async function queryAgentConfigList() {
  const res = await fetch(`${BASE_URL}/query_aiagent_config_list`)
  return res.json()
}

/**
 * 创建会话
 * @param {string} agentId
 * @param {string} userId
 */
export async function createSession(agentId, userId) {
  const res = await fetch(`${BASE_URL}/create_session?agentId=${encodeURIComponent(agentId)}&userId=${encodeURIComponent(userId)}`, {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' }
  })
  return res.json()
}

/**
 * 非流式对话
 * @param {{ agentId: string, userId: string, sessionId: string, message: string }} params
 */
export async function chat(params) {
  const res = await fetch(`${BASE_URL}/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(params)
  })
  return res.json()
}

/**
 * 流式对话
 */
export async function chatStream(params, { onMessage, onDone, onError }) {
  try {
    const res = await fetch(`${BASE_URL}/chat_stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(params)
    })

    if (!res.ok) {
      onError?.(new Error(`HTTP ${res.status}`))
      return
    }

    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        onDone?.()
        break
      }

      buffer += decoder.decode(value, { stream: true })
      const parts = buffer.split('\n\n')
      buffer = parts.pop() || ''

      for (const part of parts) {
        const clean = part.replace(/^data:/, '').trim()
        if (clean) {
          onMessage?.(clean)
        }
      }
    }
  } catch (err) {
    onError?.(err)
  }
}
