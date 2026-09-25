import request from './request'

export const aiApi = {
  /** 发送导购消息（非流式） */
  chat(data) {
    return request.post('/ai/chat', data)
  },

  /** 发送导购消息（流式 SSE） */
  chatStream(data, onMessage, onDone, onError) {
    const token = localStorage.getItem('eshop_token')
    const controller = new AbortController()

    fetch('/api/ai/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(data),
      signal: controller.signal,
    }).then(async (response) => {
      if (!response.ok) {
        onError?.(new Error(`HTTP ${response.status}`))
        return
      }
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })

        // 解析 SSE 帧: "data: {...}\n\n"
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''
        for (const line of lines) {
          if (line.startsWith('data:')) {
            try {
              // SSE data 行可能是 "data:{...}"(无空格) 或 "data: {...}"(有空格)
              const parsed = JSON.parse(line.slice(5).trimStart())
              if (parsed.type === 'done') {
                onDone?.(parsed)
              } else {
                onMessage?.(parsed)
              }
            } catch { /* 忽略解析失败的帧 */ }
          }
        }
      }
    }).catch((err) => {
      if (err.name !== 'AbortError') onError?.(err)
    })

    return controller // 返回 AbortController 以便中断
  },

  /** 获取对话历史列表 */
  getConversations(params) {
    return request.get('/ai/conversations', { params })
  },

  /** 获取对话消息 */
  getMessages(conversationId, params) {
    return request.get(`/ai/conversations/${conversationId}/messages`, { params })
  },

  /** 删除对话 */
  deleteConversation(conversationId) {
    return request.delete(`/ai/conversations/${conversationId}`)
  },

  /** 健康检测 */
  health() {
    return request.get('/ai/health')
  },
}
