<script setup>
import { ref } from 'vue'
import { useAiStore } from '../stores/ai'

const aiStore = useAiStore()
const visible = ref(false)
const inputText = ref('')

function toggle() {
  visible.value = !visible.value
  if (visible.value) {
    setTimeout(() => inputRef.value?.focus(), 400)
  }
}

const inputRef = ref(null)

function send() {
  aiStore.sendMessage(inputText.value)
  inputText.value = ''
}

function quickAsk(text) {
  if (!visible.value) visible.value = true
  setTimeout(() => {
    inputText.value = text
    send()
  }, 400)
}

function renderContent(text) {
  return text
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}

defineExpose({ toggle })
</script>

<template>
  <!-- 悬浮按钮 -->
  <button
    @click="toggle"
    class="fixed bottom-6 left-6 z-50 w-14 h-14 rounded-full shadow-float hover:shadow-card-hover hover:scale-105 active:scale-95 transition-all duration-300 flex items-center justify-center group"
    style="background: linear-gradient(135deg, #533afd, #7c5cfc);"
  >
    <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.455 2.456L21.75 6l-1.036.259a3.375 3.375 0 00-2.455 2.456z"/>
    </svg>
    <span class="absolute left-16 top-1/2 -translate-y-1/2 px-3 py-1.5 bg-ink text-white text-[12px] font-normal rounded-lg whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none">
      AI 智能导购
    </span>
  </button>

  <!-- 聊天面板 -->
  <div
    class="fixed bottom-24 left-6 z-50 w-[380px] h-[560px] bg-white rounded-2xl shadow-2xl border border-hairline flex flex-col origin-bottom-left transition-all duration-300"
    :class="visible ? 'opacity-100 scale-100 pointer-events-auto' : 'opacity-0 scale-95 pointer-events-none'"
  >
    <!-- Header -->
    <div class="flex items-center justify-between px-5 py-4 border-b border-hairline rounded-t-2xl"
      style="background: linear-gradient(135deg, #533afd, #7c5cfc);">
      <div class="flex items-center gap-3">
        <div class="w-8 h-8 rounded-lg bg-white/20 flex items-center justify-center backdrop-blur-sm">
          <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.455 2.456L21.75 6l-1.036.259a3.375 3.375 0 00-2.455 2.456z"/>
          </svg>
        </div>
        <div>
          <h3 class="text-[15px] font-normal text-white">AI 智能导购</h3>
          <p class="text-[11px] text-white/70 font-light">DeepSeek + RAG 知识库</p>
        </div>
      </div>
      <button @click="toggle" class="w-7 h-7 rounded-lg hover:bg-white/10 flex items-center justify-center transition-colors">
        <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
        </svg>
      </button>
    </div>

    <!-- 消息列表 -->
    <div class="flex-1 overflow-y-auto px-5 py-4 space-y-4 scrollbar-hide" style="background: #f8fafc;">
      <!-- 欢迎语 -->
      <div v-if="aiStore.messages.length === 0" class="flex items-start gap-3">
        <div class="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center flex-shrink-0 mt-0.5">
          <svg class="w-4 h-4 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z"/>
          </svg>
        </div>
        <div class="flex-1">
          <div class="bg-white rounded-2xl rounded-tl-none px-4 py-3 shadow-sm border border-hairline/50">
            <p class="text-[13px] font-light text-ink-secondary leading-relaxed">
              你好！我是 <strong>AI 智能导购</strong>，基于 DeepSeek 大模型和 RAG 知识库。<br><br>
              我可以帮你：<br>
              🔍 <strong>找商品</strong> — 告诉我需求，智能筛选<br>
              💬 <strong>解答问题</strong> — 商品参数、对比都可以问<br>
              🎯 <strong>精准推荐</strong> — 根据喜好量身推荐
            </p>
          </div>
          <p class="text-[11px] font-light text-ink-mute/60 mt-1.5 px-1">AI 导购 · 刚刚</p>
        </div>
      </div>

      <!-- 消息 -->
      <div v-for="(msg, idx) in aiStore.messages" :key="idx"
        class="flex items-start gap-3"
        :class="{ 'flex-row-reverse': msg.role === 'user' }"
        style="animation: cardEnter 0.3s ease-out forwards;"
      >
        <div v-if="msg.role === 'user'"
          class="w-8 h-8 rounded-lg bg-primary flex items-center justify-center flex-shrink-0 mt-0.5">
          <span class="text-white text-[12px] font-normal">{{ (msg.nickname || '你').charAt(0) }}</span>
        </div>
        <div v-else
          class="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center flex-shrink-0 mt-0.5">
          <svg class="w-4 h-4 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z"/>
          </svg>
        </div>
        <div class="flex-1" :class="{ 'text-right': msg.role === 'user' }" style="max-width: 80%;">
          <div class="inline-block text-left px-4 py-3"
            :class="msg.role === 'user'
              ? 'bg-primary text-white rounded-2xl rounded-tr-none'
              : 'bg-white rounded-2xl rounded-tl-none shadow-sm border border-hairline/50'"
          >
            <p class="text-[13px] font-light leading-relaxed"
              :class="msg.role === 'user' ? 'text-white' : 'text-ink-secondary'"
              v-html="renderContent(msg.content)"
            />
            <!-- 推荐商品 -->
            <div v-if="msg.products && msg.products.length" class="flex gap-2 mt-3 flex-wrap">
              <div v-for="p in msg.products" :key="p.productId || p.id"
                class="flex items-center gap-2 px-3 py-2 bg-canvas-soft rounded-lg cursor-pointer hover:bg-primary-bg-subdued transition-colors flex-shrink-0">
                <img :src="p.image" class="w-8 h-8 rounded object-cover">
                <div>
                  <p class="text-[11px] font-normal text-ink leading-tight">{{ p.name }}</p>
                  <p class="text-[10px] font-light text-primary font-tabular">¥{{ p.price }}</p>
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

    <!-- 快捷提问 -->
    <div v-if="aiStore.messages.length === 0" class="px-5 pb-3 flex gap-2 overflow-x-auto scrollbar-hide">
      <button @click="quickAsk('推荐适合学生的平价耳机')" class="flex-shrink-0 px-3 py-1.5 bg-primary-bg-subdued text-primary-deep text-[11px] font-normal rounded-pill hover:bg-primary-soft hover:text-white transition-colors whitespace-nowrap">
        🎧 平价耳机推荐
      </button>
      <button @click="quickAsk('想买一款智能手表')" class="flex-shrink-0 px-3 py-1.5 bg-primary-bg-subdued text-primary-deep text-[11px] font-normal rounded-pill hover:bg-primary-soft hover:text-white transition-colors whitespace-nowrap">
        ⌚ 智能手表
      </button>
      <button @click="quickAsk('有没有性价比高的蓝牙音箱')" class="flex-shrink-0 px-3 py-1.5 bg-primary-bg-subdued text-primary-deep text-[11px] font-normal rounded-pill hover:bg-primary-soft hover:text-white transition-colors whitespace-nowrap">
        🔉 蓝牙音箱
      </button>
    </div>

    <!-- 输入区域 -->
    <div class="px-4 py-3 border-t border-hairline bg-white rounded-b-2xl">
      <div class="flex items-center gap-2">
        <input
          ref="inputRef"
          v-model="inputText"
          type="text"
          placeholder="说出你的购物需求..."
          class="flex-1 px-4 py-2.5 bg-canvas-soft border border-hairline rounded-pill text-[13px] font-light text-ink placeholder-ink-mute/60 focus:outline-none focus:border-primary transition-colors"
          @keyup.enter="send"
          :disabled="aiStore.loading"
        >
        <button @click="send" :disabled="aiStore.loading || !inputText.trim()"
          class="w-10 h-10 bg-primary text-white rounded-full flex items-center justify-center hover:bg-primary-deep active:bg-primary-press transition-colors flex-shrink-0 disabled:opacity-50 disabled:cursor-not-allowed">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 12h14M12 5l7 7-7 7"/>
          </svg>
        </button>
      </div>
      <p class="text-[10px] font-light text-ink-mute/50 mt-1.5 text-center">基于 DeepSeek · RAG 增强检索</p>
    </div>
  </div>
</template>
