<script setup>
import { watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import { useUserStore } from '../stores/user'
import { useToastStore } from '../stores/toast'

const props = defineProps({
  visible: { type: Boolean, default: false },
})
const emit = defineEmits(['close'])
const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()
const toast = useToastStore()

watch(() => props.visible, (v) => {
  if (v && userStore.isLoggedIn) {
    cartStore.fetchItems().catch(() => {})
  }
})

function close() {
  emit('close')
}

function handleCheckout() {
  if (!userStore.isLoggedIn) {
    toast.show('请先登录再结算')
    close()
    return
  }
  close()
  router.push('/orders')
}
</script>

<template>
  <Teleport to="body">
    <div class="fixed inset-0 z-50" :class="{ 'pointer-events-none': !visible }">
      <div class="absolute inset-0 bg-ink/20 backdrop-blur-sm transition-opacity duration-300"
        :class="{ 'opacity-0': !visible, 'opacity-100': visible }" @click="close" />
      <div class="absolute right-0 top-0 h-full w-full max-w-md bg-white shadow-2xl transition-transform duration-300 pointer-events-auto"
        :class="{ 'translate-x-full': !visible, 'translate-x-0': visible }">
        <div class="flex flex-col h-full">
          <div class="flex items-center justify-between px-6 py-5 border-b border-hairline">
            <h3 class="text-[20px] font-light text-ink tracking-tight" style="letter-spacing: -0.2px;">购物车</h3>
            <button @click="close" class="w-9 h-9 rounded-lg hover:bg-canvas-soft flex items-center justify-center transition-colors">
              <svg class="w-5 h-5 text-ink-mute" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>
          <div class="flex-1 overflow-y-auto px-6 py-4">
            <div v-if="cartStore.items.length === 0" class="flex flex-col items-center justify-center h-full text-center">
              <div class="w-20 h-20 rounded-2xl bg-canvas-soft flex items-center justify-center mb-4">
                <svg class="w-10 h-10 text-ink-mute/40" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"/>
                </svg>
              </div>
              <p class="text-[15px] font-light text-ink-mute">购物车是空的</p>
              <p class="text-[13px] font-normal text-ink-mute/60 mt-1">快去挑选心仪的商品吧</p>
            </div>
            <div v-for="item in cartStore.items" :key="item.skuId" class="flex gap-4 py-4 border-b border-hairline last:border-0">
              <img :src="item.image" :alt="item.productName" class="w-20 h-20 rounded-lg object-cover object-top bg-canvas-soft flex-shrink-0">
              <div class="flex-1 min-w-0">
                <h4 class="text-[14px] font-light text-ink leading-snug mb-1 truncate">{{ item.productName }}</h4>
                <p v-if="item.specInfo" class="text-[11px] font-normal text-ink-mute mb-1">{{ item.specInfo }}</p>
                <p class="text-[13px] font-normal text-ink-mute mb-2">¥{{ item.price }}</p>
                <div class="flex items-center gap-3">
                  <button @click="cartStore.updateQuantity(item.skuId, item.quantity - 1)"
                    class="w-7 h-7 rounded-lg border border-hairline flex items-center justify-center hover:border-primary hover:text-primary transition-colors">
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4"/></svg>
                  </button>
                  <span class="text-[14px] font-tabular text-ink w-6 text-center">{{ item.quantity }}</span>
                  <button @click="cartStore.updateQuantity(item.skuId, item.quantity + 1)"
                    class="w-7 h-7 rounded-lg border border-hairline flex items-center justify-center hover:border-primary hover:text-primary transition-colors">
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/></svg>
                  </button>
                  <button @click="cartStore.removeItem(item.skuId)" class="ml-auto text-[11px] font-normal text-ink-mute hover:text-ruby transition-colors">删除</button>
                </div>
              </div>
            </div>
          </div>
          <div v-if="cartStore.items.length > 0" class="border-t border-hairline px-6 py-5">
            <div class="flex items-center justify-between mb-4">
              <span class="text-[15px] font-light text-ink-mute">合计</span>
              <span class="text-[22px] font-light text-ink font-tabular tracking-tight" style="letter-spacing: -0.22px;">¥{{ cartStore.totalAmount.toLocaleString() }}</span>
            </div>
            <button @click="handleCheckout" class="w-full py-3.5 bg-primary text-white text-[15px] font-normal rounded-pill hover:bg-primary-deep active:bg-primary-press transition-colors duration-200">结算</button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>
