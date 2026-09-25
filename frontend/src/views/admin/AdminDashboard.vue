<script setup>
import { ref, onMounted } from 'vue'
import { productApi } from '../../api/product'
import { userApi } from '../../api/user'
import request from '../../api/request'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const productCount = ref(0)
const pendingOrderCount = ref(0)
const userCount = ref(0)
const loading = ref(true)

onMounted(async () => {
  try {
    const [productData, orderData, userCountData] = await Promise.allSettled([
      productApi.getProducts({ page: 1, size: 1 }),
      request.get('/order/admin/orders', { params: { page: 1, size: 1, status: 1 } }),
      userApi.getUserCount(),
    ])
    if (productData.status === 'fulfilled') {
      productCount.value = productData.value.total || 0
    }
    if (orderData.status === 'fulfilled') {
      pendingOrderCount.value = orderData.value.total || 0
    }
    if (userCountData.status === 'fulfilled') {
      userCount.value = userCountData.value || 0
    }
  } catch {} finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page-container pt-8 pb-24">
    <h1 class="text-[32px] font-light text-ink mb-2">管理后台</h1>
    <p class="text-[14px] font-light text-ink-mute mb-8">欢迎回来，{{ userStore.user?.nickname || userStore.user?.username }}</p>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-10">
      <div class="bg-white rounded-2xl p-6 shadow-card">
        <p class="text-[13px] font-normal text-ink-mute mb-1">商品总数</p>
        <p class="text-[36px] font-light text-ink font-tabular">{{ loading ? '...' : productCount }}</p>
      </div>
      <div class="bg-white rounded-2xl p-6 shadow-card">
        <p class="text-[13px] font-normal text-ink-mute mb-1">待处理订单</p>
        <p class="text-[36px] font-light text-ink font-tabular">{{ loading ? '...' : pendingOrderCount }}</p>
      </div>
      <div class="bg-white rounded-2xl p-6 shadow-card">
        <p class="text-[13px] font-normal text-ink-mute mb-1">注册用户</p>
        <p class="text-[36px] font-light text-ink font-tabular">{{ loading ? '...' : userCount }}</p>
      </div>
    </div>

    <!-- 管理入口 -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <router-link to="/admin/products" class="bg-white rounded-2xl p-8 shadow-card hover:shadow-card-hover transition-shadow group">
        <p class="text-[20px] font-light text-ink group-hover:text-primary transition-colors mb-2">📦 商品管理</p>
        <p class="text-[14px] font-light text-ink-mute">查看、新增、编辑、上下架商品</p>
      </router-link>
      <router-link to="/admin/orders" class="bg-white rounded-2xl p-8 shadow-card hover:shadow-card-hover transition-shadow group">
        <p class="text-[20px] font-light text-ink group-hover:text-primary transition-colors mb-2">📋 订单管理</p>
        <p class="text-[14px] font-light text-ink-mute">查看所有订单、处理发货</p>
      </router-link>
    </div>
  </div>
</template>
