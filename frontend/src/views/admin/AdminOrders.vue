<script setup>
import { ref, onMounted } from 'vue'
import { useToastStore } from '../../stores/toast'
import request from '../../api/request'

const toast = useToastStore()

const orders = ref([])
const loading = ref(true)
const statusFilter = ref('')
const trackingInput = ref({}) // orderId -> trackingNo

const statusMap = { 0: '待付款', 1: '已付款', 2: '已发货', 3: '已完成', 4: '已取消' }
const statusClass = {
  0: 'bg-amber/10 text-amber',
  1: 'bg-primary-bg-subdued text-primary',
  2: 'bg-emerald/10 text-emerald',
  3: 'bg-ink-mute/10 text-ink-mute',
  4: 'bg-ruby/10 text-ruby',
}

async function loadOrders() {
  loading.value = true
  try {
    const params = { page: 1, size: 50 }
    if (statusFilter.value) params.status = statusFilter.value
    const data = await request.get('/order/admin/orders', { params })
    orders.value = data.records || []
  } catch (e) {
    toast.show('加载订单失败')
  } finally {
    loading.value = false
  }
}

async function deliver(order) {
  try {
    await request.put(`/order/admin/orders/${order.id}/deliver`)
    toast.show(`订单 ${order.orderNo} 已标记发货`)
    loadOrders()
  } catch (e) {
    toast.show('发货失败: ' + (e.message || '未知错误'))
  }
}

async function payForUser(order) {
  try {
    await request.put(`/order/orders/${order.id}/pay`)
    toast.show(`订单 ${order.orderNo} 已模拟付款`)
    loadOrders()
  } catch (e) {
    toast.show('支付失败: ' + (e.message || '未知错误'))
  }
}

onMounted(loadOrders)
</script>

<template>
  <div class="page-container pt-8 pb-24">
    <div class="flex items-center justify-between mb-8">
      <div>
        <router-link to="/admin" class="text-[13px] text-ink-mute hover:text-primary transition-colors">← 管理后台</router-link>
        <h1 class="text-[32px] font-light text-ink mt-1">订单管理</h1>
      </div>
      <div class="flex gap-2">
        <button v-for="(label, val) in { '': '全部', 0: '待付款', 1: '已付款', 2: '已发货', 3: '已完成' }" :key="val"
          @click="statusFilter = val; loadOrders()"
          class="px-3 py-1.5 text-[12px] rounded-pill border transition-colors"
          :class="statusFilter === val ? 'bg-primary text-white border-primary' : 'border-hairline text-ink-mute hover:border-primary'">
          {{ label }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="text-center py-10 text-ink-mute">加载中...</div>

    <div v-else class="space-y-4">
      <div v-for="order in orders" :key="order.id" class="bg-white rounded-2xl p-6 shadow-card">
        <!-- 头部：订单号 + 状态 + 时间 -->
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-3">
            <span class="text-[13px] font-normal text-ink-mute font-tabular">#{{ order.orderNo || order.id }}</span>
            <span class="text-[12px] font-normal text-ink-mute">{{ order.createTime || '' }}</span>
          </div>
          <span class="px-3 py-1 text-[12px] rounded-pill font-normal" :class="statusClass[order.status] || 'bg-ink-mute/10 text-ink-mute'">
            {{ statusMap[order.status] || '未知' }}
          </span>
        </div>

        <!-- 用户信息 -->
        <div class="text-[12px] text-ink-mute mb-3">
          用户ID: {{ order.userId }} | 收货人: {{ order.consigneeName || '-' }}
          <span v-if="order.consigneePhone"> | {{ order.consigneePhone }}</span>
          <span v-if="order.consigneeAddress"> | {{ order.consigneeAddress }}</span>
        </div>

        <!-- 商品明细 -->
        <div v-if="order.items && order.items.length" class="space-y-2 mb-4">
          <div v-for="item in order.items" :key="item.id || item.productId"
            class="flex items-center gap-3 py-2 px-3 bg-canvas-soft/50 rounded-lg">
            <span class="text-[14px] text-ink flex-1 min-w-0 truncate">{{ item.productName }}</span>
            <span v-if="item.specValues" class="text-[12px] text-ink-mute">{{ item.specValues }}</span>
            <span class="text-[13px] text-ink-mute font-tabular">¥{{ item.price }} × {{ item.quantity }}</span>
            <span class="text-[14px] text-ink font-tabular">¥{{ (item.subtotal || item.price * item.quantity).toFixed(2) }}</span>
          </div>
        </div>
        <div v-else class="text-[13px] text-ink-mute mb-4">商品信息加载中...</div>

        <!-- 底部：合计 + 操作 -->
        <div class="flex items-center justify-between pt-4 border-t border-hairline">
          <div>
            <span class="text-[13px] text-ink-mute">共 {{ order.items?.length || 0 }} 件 · </span>
            <span class="text-[18px] font-light text-ink font-tabular">¥{{ (order.payAmount || order.totalAmount || 0).toFixed(2) }}</span>
          </div>
          <div class="flex gap-2 items-center">
            <!-- 待付款 → 模拟付款 -->
            <button v-if="order.status === 0" @click="payForUser(order)"
              class="px-4 py-1.5 bg-primary text-white text-[12px] rounded-pill hover:bg-primary-deep transition-colors">
              模拟付款
            </button>
            <!-- 已付款 → 发货 -->
            <button v-if="order.status === 1" @click="deliver(order)"
              class="px-4 py-1.5 bg-emerald text-white text-[12px] rounded-pill hover:bg-emerald/80 transition-colors">
              标记发货
            </button>
            <!-- 已发货 / 已完成 -->
            <span v-if="order.status >= 2" class="text-[12px] text-ink-mute">
              {{ order.status === 2 ? '🚚 已发货' : '✅ 已完成' }}
            </span>
          </div>
        </div>
      </div>

      <div v-if="orders.length === 0" class="text-center py-20 text-ink-mute">
        <p class="text-[16px] font-light">暂无订单</p>
      </div>
    </div>
  </div>
</template>
