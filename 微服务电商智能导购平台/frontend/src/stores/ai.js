import { defineStore } from 'pinia'
import { ref } from 'vue'
import { aiApi } from '../api/ai'

export const useAiStore = defineStore('ai', () => {
  const messages = ref([])
  const conversationId = ref(0)
  const loading = ref(false)
  const abortController = ref(null)

  async function sendMessage(text) {
    if (!text.trim() || loading.value) return

    // 添加用户消息
    messages.value.push({ role: 'user', content: text, contentType: 'text' })
    loading.value = true

    // 准备助手消息占位
    const assistantMsg = { role: 'assistant', content: '', contentType: 'text', products: [] }
    messages.value.push(assistantMsg)

    try {
      const controller = aiApi.chatStream(
        { conversationId: conversationId.value, message: text },
        // onMessage
        (chunk) => {
          if (chunk.type === 'text') {
            assistantMsg.content += chunk.content
          } else if (chunk.type === 'product') {
            assistantMsg.products = assistantMsg.products || []
            assistantMsg.products.push(chunk.data)
          } else if (chunk.type === 'reasoning') {
            // 可选的推理过程，暂不展示
          }
        },
        // onDone
        (done) => {
          if (done.conversationId) conversationId.value = done.conversationId
          if (done.recommendations) {
            assistantMsg.recommendations = done.recommendations
          }
          loading.value = false
        },
        // onError
        (err) => {
          assistantMsg.content = '抱歉，我现在暂时无法回答。请稍后再试。'
          loading.value = false
        },
      )
      abortController.value = controller
    } catch (err) {
      assistantMsg.content = '抱歉，我现在暂时无法回答。请稍后再试。'
      loading.value = false
    }
  }

  function stopGeneration() {
    if (abortController.value) {
      abortController.value.abort()
      abortController.value = null
      loading.value = false
    }
  }

  function clearMessages() {
    messages.value = []
    conversationId.value = 0
  }

  return {
    messages, conversationId, loading,
    sendMessage, stopGeneration, clearMessages,
  }
})
