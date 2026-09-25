<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useCartStore } from '../stores/cart'
import { useToastStore } from '../stores/toast'

const emit = defineEmits(['open-auth'])
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const cartStore = useCartStore()
const toast = useToastStore()

const navSearchQuery = ref('')

function handleNavSearch() {
  const q = navSearchQuery.value.trim()
  if (!q) return
  // 跳转到首页并带上搜索关键词
  router.push(q ? `/?search=${encodeURIComponent(q)}` : '/')
  navSearchQuery.value = ''
}

function handleLogout() {
  userStore.logout()
  toast.show('已退出登录')
  router.push('/')
}
</script>

<template>
  <nav class="relative z-10 px-6 lg:px-12 py-5 flex items-center justify-between page-container">
    <!-- Logo -->
    <router-link to="/" class="flex items-center gap-2">
      <div class="w-8 h-8 rounded-lg bg-primary flex items-center justify-center">
        <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"/>
        </svg>
      </div>
      <span class="text-xl font-light tracking-tight text-ink" style="letter-spacing: -0.64px;">商城</span>
    </router-link>

    <!-- 导航链接 -->
    <div class="hidden md:flex items-center gap-8 text-[15px] font-light">
      <router-link to="/" class="text-ink-mute hover:text-primary transition-colors duration-200"
        :class="{ 'text-primary font-normal': route.path === '/' }">
        首页
      </router-link>
      <router-link to="/chat" class="text-ink-mute hover:text-primary transition-colors duration-200"
        :class="{ 'text-primary font-normal': route.path === '/chat' }">
        AI 导购
      </router-link>
      <router-link to="/cart" class="text-ink-mute hover:text-primary transition-colors duration-200"
        :class="{ 'text-primary font-normal': route.path === '/cart' }">
        购物车
        <span v-if="cartStore.totalCount > 0" class="ml-1 px-1.5 py-0.5 bg-ruby/10 text-ruby text-[10px] rounded-full">{{ cartStore.totalCount }}</span>
      </router-link>
      <router-link to="/orders" class="text-ink-mute hover:text-primary transition-colors duration-200"
        :class="{ 'text-primary font-normal': route.path.startsWith('/order') }">
        订单
      </router-link>
      <router-link to="/favorites" v-if="userStore.isLoggedIn" class="text-ink-mute hover:text-primary transition-colors duration-200"
        :class="{ 'text-primary font-normal': route.path === '/favorites' }">
        收藏
      </router-link>
      <router-link to="/profile" v-if="userStore.isLoggedIn" class="text-ink-mute hover:text-primary transition-colors duration-200"
        :class="{ 'text-primary font-normal': route.path === '/profile' }">
        个人中心
      </router-link>
      <router-link to="/admin" v-if="userStore.isAdmin" class="text-ink-mute hover:text-primary transition-colors duration-200"
        :class="{ 'text-primary font-normal': route.path.startsWith('/admin') }">
        管理后台
      </router-link>
    </div>

    <!-- 导航栏搜索框 -->
    <div class="hidden md:block flex-1 max-w-xs mx-4">
      <div class="relative flex items-center">
        <svg class="absolute left-3 w-4 h-4 text-ink-mute/50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
        </svg>
        <input v-model="navSearchQuery" type="text" placeholder="搜索商品..."
          class="w-full pl-9 pr-3 py-2 bg-canvas-soft rounded-lg border-0 text-[13px] text-ink placeholder-ink-mute/50 focus:outline-none focus:ring-1 focus:ring-primary/30 transition-all"
          @keyup.enter="handleNavSearch">
      </div>
    </div>

    <!-- 用户操作 -->
    <div class="flex items-center gap-4">
      <template v-if="userStore.isLoggedIn">
        <span class="text-[14px] font-normal text-ink">{{ userStore.user?.nickname || userStore.user?.username }}</span>
        <button @click="handleLogout" class="text-[12px] font-normal text-ink-mute hover:text-ruby transition-colors">退出</button>
      </template>
      <template v-else>
        <button @click="emit('open-auth', 'login')" class="px-4 py-2 text-[14px] font-normal text-primary hover:bg-primary-bg-subdued rounded-pill transition-colors duration-200">
          登录
        </button>
        <button @click="emit('open-auth', 'register')" class="px-5 py-2.5 bg-primary text-white text-[14px] font-normal rounded-pill hover:bg-primary-deep active:bg-primary-press transition-colors duration-200 shadow-card">
          注册
        </button>
      </template>
    </div>
  </nav>
</template>
