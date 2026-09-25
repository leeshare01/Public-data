<script setup>
import { ref } from 'vue'
import { useUIStore } from './stores/ui'
import { useCartStore } from './stores/cart'
import { useUserStore } from './stores/user'
import { useToastStore } from './stores/toast'
import NavBar from './components/NavBar.vue'
import Toast from './components/Toast.vue'
import AIChatPanel from './components/AIChatPanel.vue'
import CartSidebar from './components/CartSidebar.vue'
import AuthModal from './components/AuthModal.vue'
import ProductDetailModal from './components/ProductDetailModal.vue'
import FooterBar from './components/FooterBar.vue'

const ui = useUIStore()
const cartStore = useCartStore()
const userStore = useUserStore()
const toast = useToastStore()

// 收藏数量（简单实现，后续接入 API）
const favCount = ref(JSON.parse(localStorage.getItem('eshop_favorites') || '[]').length)

function openAuth(tab) {
  ui.openAuth(tab)
}

function openCart() {
  if (userStore.isLoggedIn) {
    cartStore.fetchItems().catch(() => {})
  }
  ui.openCart()
}
</script>

<template>
  <div class="min-h-screen bg-canvas">
    <NavBar @open-auth="openAuth" />

    <!-- 渐变背景 -->
    <div class="fixed inset-0 gradient-mesh pointer-events-none z-0" style="height: 45vh;" />

    <!-- 页面内容 -->
    <main class="relative z-10">
      <router-view />
    </main>

    <FooterBar />

    <!-- 悬浮按钮 -->
    <div class="fixed bottom-6 right-6 z-50 flex flex-col gap-3">
      <!-- 收藏（已隐藏） -->
      <!-- 购物车 -->
      <button @click="openCart" class="w-14 h-14 bg-primary text-white rounded-full shadow-float hover:shadow-card-hover hover:scale-105 active:scale-95 transition-all duration-300 flex items-center justify-center relative">
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"/>
        </svg>
        <span
          v-if="cartStore.totalCount > 0"
          class="absolute -top-1 -right-1 w-5 h-5 bg-ruby text-white text-[10px] font-normal rounded-full flex items-center justify-center"
        >{{ cartStore.totalCount }}</span>
      </button>
    </div>

    <!-- 全局弹窗 -->
    <AuthModal :visible="ui.showAuthModal" @close="ui.closeAuth()" />
    <CartSidebar :visible="ui.showCart" @close="ui.closeCart()" />
    <ProductDetailModal :visible="ui.showDetailModal" @close="ui.closeDetail()" />
    <AIChatPanel />
    <Toast />
  </div>
</template>
