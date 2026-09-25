<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAiStore } from '../stores/ai'
import { aiApi } from '../api/ai'

const router = useRouter()
const aiStore = useAiStore()
const inputText = ref('')
const conversations = ref([])
const showHistory = ref(false)
const historyLoading = ref(false)

function send() {
  if (!inputText.value.trim() || aiStore.loading) return
  aiStore.sendMessage(inputText.value)
  inputText.value = ''
}

function stop() {
  aiStore.stopGeneration()
}

function clear() {
  aiStore.clearMessages()
}

function newConversation() {
  aiStore.clearMessages()
}

function renderContent(text) {
  return text
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}

async function loadConversations() {
  historyLoading.value = true
  try {
    const data = await aiApi.getConversations({ page: 1, size: 20 })
    conversations.value = data.records || data || []
  } catch {
    conversations.value = []
  } finally {
    historyLoading.value = false
  }
}

async function loadConversation(convId) {
  try {
    const data = await aiApi.getMessages(convId)
    aiStore.conversationId = convId
    aiStore.messages = data.map(m => ({
      role: m.role,
      content: m.content,
      contentType: m.contentType || 'text',
    }))
    showHistory.value = false
  } catch {
    // ignore
  }
}

async function deleteConversation(convId) {
  try {
    await aiApi.deleteConversation(convId)
    conversations.value = conversations.value.filter(c => c.id !== convId)
  } catch {
    // ignore
  }
}

onMounted(() => {
  if (aiStore.messages.length === 0) {
    aiStore.clearMessages()
  }
})
</script>

<template>
  <div class="page-container pt-8 pb-24" style="max-width: 800px;">
    <!-- 头部 -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-[32px] font-light text-ink mb-1">AI 智能导购</h1>
        <p class="text-[14px] font-light text-ink-mute">基于 DeepSeek 大模型 · RAG 增强检索</p>
      </div>
      <div class="flex gap-2">
        <button @click="showHistory = !showHistory; if(showHistory) loadConversations()"
          class="px-4 py-2 border border-hairline text-ink-mute text-[13px] rounded-pill hover:border-primary hover:text-primary transition-colors">
          {{ showHistory ? '关闭历史' : '历史记录' }}
        </button>
        <button @click="newConversation"
          class="px-4 py-2 bg-primary text-white text-[13px] rounded-pill hover:bg-primary-deep transition-colors">
          + 新对话
        </button>
      </div>
    </div>

    <div class="flex gap-6">
      <!-- 历史记录侧栏 -->
      <div v-if="showHistory" class="w-64 flex-shrink-0">
        <div class="bg-white rounded-2xl shadow-card p-4 sticky top-8" style="max-height: 70vh; overflow-y: auto;">
          <h3 class="text-[14px] font-normal text-ink mb-3">对话历史</h3>
          <div v-if="historyLoading" class="text-[12px] text-ink-mute py-4 text-center">加载中...</div>
          <div v-else-if="conversations.length === 0" class="text-[12px] text-ink-mute py-4 text-center">暂无历史对话</div>
          <div v-else class="space-y-1">
            <div v-for="conv in conversations" :key="conv.id"
              class="group flex items-center gap-2 px-3 py-2 rounded-lg cursor-pointer hover:bg-canvas-soft transition-colors"
              :class="{ 'bg-primary-bg-subdued': conv.id === aiStore.conversationId }"
              @click="loadConversation(conv.id)">
              <span class="flex-1 text-[12px] text-ink truncate">{{ conv.title || conv.firstMessage || `对话 ${conv.id}` }}</span>
              <button @click.stop="deleteConversation(conv.id)"
                class="opacity-0 group-hover:opacity-100 text-ink-mute hover:text-ruby transition-all text-[14px]">✕</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 聊天区域 -->
      <div class="flex-1">
        <div class="bg-white rounded-2xl shadow-card flex flex-col" style="min-height: 500px; max-height: 70vh;">
          <!-- 消息列表 -->
          <div class="flex-1 overflow-y-auto px-6 py-5 space-y-4" style="background: #f8fafc;">
            <!-- 欢迎语 -->
            <div v-if="aiStore.messages.length === 0" class="text-center py-12">
              <div class="w-16 h-16 rounded-2xl bg-primary/10 flex items-center justify-center mx-auto mb-4">
                <svg class="w-8 h-8 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.455 2.456L21.75 6l-1.036.259a3.375 3.375 0 00-2.455 2.456z"/>
                </svg>
              </div>
              <p class="text-[16px] font-light text-ink mb-2">有什么可以帮助你的？</p>
              <p class="text-[13px] font-light text-ink-mute">告诉我购物需求，我来为你智能推荐</p>
              <div class="flex flex-wrap justify-center gap-2 mt-6">
                <button @click="inputText = '推荐适合学生的平价耳机'; send()"
                  class="px-4 py-2 bg-primary-bg-subdued text-primary-deep text-[12px] rounded-pill hover:bg-primary-soft transition-colors">
                  🎧 平价耳机推荐
                </button>
                <button @click="inputText = '想买一款智能手表'; send()"
                  class="px-4 py-2 bg-primary-bg-subdued text-primary-deep text-[12px] rounded-pill hover:bg-primary-soft transition-colors">
                  ⌚ 智能手表
                </button>
                <button @click="inputText = '有没有性价比高的蓝牙音箱'; send()"
                  class="px-4 py-2 bg-primary-bg-subdued text-primary-deep text-[12px] rounded-pill hover:bg-primary-soft transition-colors">
                  🔉 蓝牙音箱
                </button>
                <button @click="inputText = '帮我推荐一款手机'; send()"
                  class="px-4 py-2 bg-primary-bg-subdued text-primary-deep text-[12px] rounded-pill hover:bg-primary-soft transition-colors">
                  📱 推荐手机
                </button>
              </div>
            </div>

            <!-- 消息 -->
            <div v-for="(msg, idx) in aiStore.messages" :key="idx"
              class="flex items-start gap-3"
              :class="{ 'flex-row-reverse': msg.role === 'user' }">
              <div v-if="msg.role === 'user'"
                class="w-8 h-8 rounded-lg bg-primary flex items-center justify-center flex-shrink-0 mt-0.5">
                <span class="text-white text-[12px] font-normal">你</span>
              </div>
              <div v-else
                class="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center flex-shrink-0 mt-0.5">
                <svg class="w-4 h-4 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z"/>
                </svg>
              </div>
              <div class="flex-1" :class="{ 'text-right': msg.role === 'user' }" style="max-width: 80%;">
                <div class="inline-block text-left px-5 py-3"
                  :class="msg.role === 'user'
                    ? 'bg-primary text-white rounded-2xl rounded-tr-none'
                    : 'bg-white rounded-2xl rounded-tl-none shadow-sm border border-hairline/50'">
                  <p class="text-[14px] font-light leading-relaxed"
                    :class="msg.role === 'user' ? 'text-white' : 'text-ink-secondary'"
                    v-html="renderContent(msg.content)" />
                  <!-- 推荐商品卡片 -->
                  <div v-if="msg.recommendations && msg.recommendations.length" class="mt-4 space-y-2">
                    <p class="text-[11px] font-normal" :class="msg.role === 'user' ? 'text-white/70' : 'text-ink-mute'">推荐商品</p>
                    <div class="flex flex-wrap gap-2">
                      <div v-for="p in msg.recommendations.slice(0, 5)" :key="p.id"
                        @click="router.push(`/product/${p.id}`)"
                        class="flex items-center gap-2 px-3 py-2 rounded-xl cursor-pointer transition-colors"
                        :class="msg.role === 'user' ? 'bg-white/10 hover:bg-white/20' : 'bg-canvas-soft hover:bg-primary-bg-subdued'">
                        <img :src="p.mainImage" class="w-10 h-10 rounded-lg object-cover bg-white">
                        <div>
                          <p class="text-[11px] font-normal leading-tight" :class="msg.role === 'user' ? 'text-white' : 'text-ink'">{{ p.name }}</p>
                          <p class="text-[10px] font-tabular mt-0.5" :class="msg.role === 'user' ? 'text-white/70' : 'text-primary'">¥{{ p.price }}</p>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 正在输入 -->
            <div v-if="aiStore.loading" class="flex items-start gap-3">
              <div class="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center flex-shrink-0 mt-0.5">
                <svg class="w-4 h-4 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z"/>
                </svg>
              </div>
              <div class="bg-white rounded-2xl rounded-tl-none px-4 py-3 shadow-sm border border-hairline/50">
                <div class="flex gap-1.5">
                  <span class="w-2 h-2 bg-primary/40 rounded-full animate-bounce" style="animation-delay: 0ms;"></span>
                  <span class="w-2 h-2 bg-primary/40 rounded-full animate-bounce" style="animation-delay: 150ms;"></span>
                  <span class="w-2 h-2 bg-primary/40 rounded-full animate-bounce" style="animation-delay: 300ms;"></span>
                </div>
              </div>
            </div>
          </div>

          <!-- 底部输入 -->
          <div class="px-6 py-4 border-t border-hairline bg-white rounded-b-2xl">
            <div class="flex items-center gap-3">
              <input v-model="inputText" type="text"
                placeholder="说出你的购物需求..."
                class="flex-1 px-5 py-3 bg-canvas-soft border border-hairline rounded-pill text-[14px] font-light text-ink placeholder-ink-mute/60 focus:outline-none focus:border-primary transition-colors"
                @keyup.enter="send" :disabled="aiStore.loading">
              <button v-if="aiStore.loading" @click="stop"
                class="px-5 py-3 bg-ink-mute/20 text-ink-mute rounded-pill text-[13px] hover:bg-ink-mute/30 transition-colors">
                停止
              </button>
              <button @click="send" :disabled="aiStore.loading || !inputText.trim()"
                class="w-11 h-11 bg-primary text-white rounded-full flex items-center justify-center hover:bg-primary-deep active:bg-primary-press transition-colors flex-shrink-0 disabled:opacity-50 disabled:cursor-not-allowed">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 12h14M12 5l7 7-7 7"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
