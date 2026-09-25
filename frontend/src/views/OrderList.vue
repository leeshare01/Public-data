<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useOrderStore } from '../stores/order'

const router = useRouter()
const userStore = useUserStore()
const orderStore = useOrderStore()

const localOrders = ref([])

const statusMap = {
  0: { text: '待付款', cls: 'bg-amber/10 text-amber' },
  1: { text: '已付款', cls: 'bg-emerald/10 text-emerald' },
  2: { text: '已发货', cls: 'bg-emerald/10 text-emerald' },
  3: { text: '已完成', cls: 'bg-primary-bg-subdued text-primary-deep' },
  4: { text: '已取消', cls: 'bg-ink-mute/10 text-ink-mute' },
}

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    router.push('/')
    return
  }
  try {
    await orderStore.fetchOrders({ size: 50 })
  } catch {
    localOrders.value = JSON.parse(localStorage.getItem('eshop_orders') || '[]')
  }
})

const displayOrders = computed(() => orderStore.orders.length > 0 ? orderStore.orders : localOrders.value)
</script>

<template>
  <div class="page-container pt-8 pb-24">
    <h1 class="text-[32px] font-light text-ink mb-8">我的订单</h1>

    <div v-if="orderStore.loading" class="text-center py-20 text-ink-mute">加载中...</div>

    <div v-else-if="displayOrders.length === 0" class="text-center py-20">
      <div class="w-24 h-24 rounded-2xl bg-canvas-soft flex items-center justify-center mx-auto mb-4">
        <svg class="w-12 h-12 text-ink-mute/30" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
        </svg>
      </div>
      <p class="text-[16px] font-light text-ink-mute">暂无订单</p>
      <button @click="router.push('/')" class="mt-4 px-5 py-2 bg-primary text-white text-[13px] font-normal rounded-pill hover:bg-primary-deep transition-colors">去购物</button>
    </div>

    <div v-else class="space-y-4">
      <div v-for="order in displayOrders" :key="order.id"
        class="bg-white rounded-2xl shadow-card hover:shadow-card-hover transition-all duration-300 p-6 cursor-pointer"
        @click="router.push(`/order/${order.id}`)">
        <div class="flex items-center justify-between mb-4">
          <span class="text-[13px] font-normal text-ink-mute">订单号: {{ order.orderNo }}</span>
          <span class="text-[13px] font-normal px-3 py-1 rounded-pill" :class="statusMap[order.status]?.cls || 'bg-ink-mute/10 text-ink-mute'">{{ statusMap[order.status]?.text || '未知' }}</span>
        </div>
        <div v-for="item in order.items" :key="item.skuCode || item.productName" class="flex items-center gap-4 mb-3 last:mb-0">
          <div class="flex-1 min-w-0">
            <p class="text-[14px] font-light text-ink truncate">{{ item.productName }}</p>
            <p v-if="item.specValues" class="text-[12px] font-normal text-ink-mute">{{ item.specValues }}</p>
          </div>
          <div class="text-right flex-shrink-0">
            <p class="text-[12px] font-normal text-ink-mute font-tabular">×{{ item.quantity }}</p>
            <p class="text-[14px] font-normal text-ink font-tabular">¥{{ (item.subtotal || item.price * item.quantity).toLocaleString() }}</p>
          </div>
        </div>
        <div class="flex items-center justify-between mt-4 pt-4 border-t border-hairline">
          <span class="text-[14px] font-light text-ink-mute">共 {{ order.items?.length || 0 }} 件商品</span>
          <span class="text-[18px] font-light text-ink font-tabular">实付: ¥{{ (order.payAmount || order.totalAmount || 0).toLocaleString() }}</span>
        </div>
      </div>
    </div>
  </div>
</template>
