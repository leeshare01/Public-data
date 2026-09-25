<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useOrderStore } from '../stores/order'
import { useToastStore } from '../stores/toast'
import { orderApi } from '../api/order'

const route = useRoute()
const router = useRouter()
const orderStore = useOrderStore()
const toast = useToastStore()

const localOrder = ref(null)
const paying = ref(false)
const confirming = ref(false)

const statusMap = {
  0: { text: '待付款', color: 'bg-amber/10 text-amber' },
  1: { text: '已付款', color: 'bg-emerald/10 text-emerald' },
  2: { text: '已发货', color: 'bg-emerald/10 text-emerald' },
  3: { text: '已完成', color: 'bg-primary-bg-subdued text-primary-deep' },
  4: { text: '已取消', color: 'bg-ink-mute/10 text-ink-mute' },
}

const order = computed(() => orderStore.currentOrder || localOrder.value)

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    await orderStore.fetchOrderDetail(id)
  } catch {
    const saved = JSON.parse(localStorage.getItem('eshop_orders') || '[]')
    localOrder.value = saved.find(o => o.id === id)
  }
})

async function payOrder() {
  if (!order.value || paying.value) return
  paying.value = true
  try {
    await orderApi.pay(order.value.id)
    order.value.status = 1
    toast.show('✅ 模拟支付成功！')
  } catch (e) {
    toast.show('支付失败: ' + (e.message || '请重试'))
  } finally {
    paying.value = false
  }
}

async function confirmReceive() {
  if (!order.value || confirming.value) return
  confirming.value = true
  try {
    await orderApi.confirmReceive(order.value.id)
    order.value.status = 3
    toast.show('✅ 已确认收货')
  } catch (e) {
    toast.show('确认收货失败: ' + (e.message || '请重试'))
  } finally {
    confirming.value = false
  }
}

async function cancelOrder() {
  if (!order.value) return
  try {
    await orderStore.cancelOrder(order.value.id, '用户取消')
    toast.show('订单已取消')
  } catch {
    // fallback for localStorage mode
    const saved = JSON.parse(localStorage.getItem('eshop_orders') || '[]')
    const idx = saved.findIndex(o => o.id === order.value.id)
    if (idx >= 0) {
      saved[idx].status = 4
      localStorage.setItem('eshop_orders', JSON.stringify(saved))
    }
    order.value.status = 4
    toast.show('订单已取消')
  }
}
</script>

<template>
  <div class="page-container pt-8 pb-24">
    <button @click="router.push('/orders')" class="flex items-center gap-1 text-[14px] font-normal text-ink-mute hover:text-primary transition-colors mb-6">
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
      返回订单列表
    </button>

    <div v-if="orderStore.loading" class="text-center py-20 text-ink-mute">加载中...</div>
    <div v-else-if="!order" class="text-center py-20"><p class="text-[16px] font-light text-ink-mute">订单不存在</p></div>

    <div v-else>
      <div class="bg-white rounded-2xl shadow-card p-6 mb-6">
        <div class="flex items-center justify-between mb-2">
          <h1 class="text-[22px] font-light text-ink">订单详情</h1>
          <span class="text-[13px] font-normal px-3 py-1 rounded-pill" :class="statusMap[order.status]?.color">{{ statusMap[order.status]?.text }}</span>
        </div>
        <p class="text-[13px] font-normal text-ink-mute">订单号: {{ order.orderNo }}</p>
        <p class="text-[13px] font-normal text-ink-mute">下单时间: {{ order.createTime }}</p>
      </div>

      <div class="bg-white rounded-2xl shadow-card p-6 mb-6">
        <h3 class="text-[16px] font-light text-ink mb-4">商品信息</h3>
        <div v-for="item in order.items" :key="item.skuCode || item.productName" class="flex items-center gap-4 py-3 border-b border-hairline last:border-0">
          <div class="flex-1 min-w-0">
            <p class="text-[15px] font-light text-ink mb-1">{{ item.productName }}</p>
            <p v-if="item.specValues" class="text-[12px] font-normal text-ink-mute mb-1">{{ item.specValues }}</p>
            <p class="text-[14px] font-normal text-ink font-tabular">¥{{ item.price }} × {{ item.quantity }}</p>
          </div>
          <span class="text-[16px] font-light text-ink font-tabular">¥{{ (item.subtotal || item.price * item.quantity).toLocaleString() }}</span>
        </div>
      </div>

      <div class="bg-white rounded-2xl shadow-card p-6 mb-6">
        <div class="space-y-2">
          <div class="flex items-center justify-between text-[14px]">
            <span class="text-ink-mute">商品总额</span>
            <span class="text-ink font-tabular">¥{{ (order.totalAmount || 0).toLocaleString() }}</span>
          </div>
          <div class="flex items-center justify-between text-[14px]">
            <span class="text-ink-mute">实付金额</span>
            <span class="text-[20px] font-light text-ink font-tabular">¥{{ (order.payAmount || order.totalAmount || 0).toLocaleString() }}</span>
          </div>
        </div>
      </div>

      <div class="flex gap-3 flex-wrap">
        <!-- 待付款 → 模拟支付 + 取消 -->
        <template v-if="order.status === 0">
          <button @click="payOrder" :disabled="paying"
            class="px-6 py-3 bg-primary text-white text-[14px] font-normal rounded-pill hover:bg-primary-deep transition-colors"
            :class="paying ? 'opacity-60 cursor-not-allowed' : ''">
            {{ paying ? '支付中…' : '💳 模拟付款' }}
          </button>
          <button @click="cancelOrder" class="px-6 py-3 border border-hairline text-ink-mute text-[14px] font-normal rounded-pill hover:border-ruby hover:text-ruby transition-colors">取消订单</button>
        </template>
        <!-- 已发货 → 确认收货 -->
        <button v-if="order.status === 2" @click="confirmReceive" :disabled="confirming"
          class="px-6 py-3 bg-emerald text-white text-[14px] font-normal rounded-pill hover:bg-emerald/80 transition-colors"
          :class="confirming ? 'opacity-60 cursor-not-allowed' : ''">
          {{ confirming ? '确认中…' : '📦 确认收货' }}
        </button>
      </div>
    </div>
  </div>
</template>
