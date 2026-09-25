<script setup>
import { ref, onMounted } from 'vue'
import { productApi } from '../../api/product'
import request from '../../api/request'
import { useToastStore } from '../../stores/toast'

const toast = useToastStore()
const products = ref([])
const loading = ref(true)
const page = ref(1)
const pageSize = 20
const total = ref(0)
const statusFilter = ref('') // '' = 全部, 0 = 下架, 1 = 上架

async function loadProducts() {
  loading.value = true
  try {
    const params = { page: page.value, size: pageSize }
    if (statusFilter.value !== '') {
      params.status = statusFilter.value
    }
    const data = await productApi.getAdminProducts(params)
    products.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    products.value = []
    total.value = 0
    toast.show('加载商品列表失败')
  } finally {
    loading.value = false
  }
}

async function changeStatus(product) {
  try {
    const newStatus = product.status === 1 ? 0 : 1
    await request.put(`/product/${product.id}`, { status: newStatus })
    product.status = newStatus
    toast.show(`"${product.name}" 已${newStatus === 1 ? '上架' : '下架'}`)
  } catch (e) {
    toast.show('操作失败: ' + (e.message || '未知错误'))
  }
}

async function deleteProduct(product) {
  if (!confirm(`确定要删除商品 "${product.name}" 吗？该操作不可恢复。`)) {
    return
  }
  try {
    await productApi.deleteProduct(product.id)
    toast.show(`"${product.name}" 已删除`)
    loadProducts()
  } catch (e) {
    toast.show('删除失败: ' + (e.message || '未知错误'))
  }
}

function switchFilter(status) {
  statusFilter.value = status
  page.value = 1
  loadProducts()
}

onMounted(loadProducts)
</script>

<template>
  <div class="page-container pt-8 pb-24">
    <div class="flex items-center justify-between mb-8">
      <div>
        <router-link to="/admin" class="text-[13px] text-ink-mute hover:text-primary transition-colors">← 管理后台</router-link>
        <h1 class="text-[32px] font-light text-ink mt-1">商品管理</h1>
      </div>
      <div class="flex gap-2 items-center">
        <router-link to="/admin/products/add"
          class="px-4 py-1.5 bg-primary text-white text-[12px] font-normal rounded-pill hover:bg-primary-deep active:bg-primary-press transition-colors">
          + 添加商品
        </router-link>
        <button v-for="(label, val) in { '': '全部', 1: '上架', 0: '下架' }" :key="val"
          @click="switchFilter(val)"
          class="px-3 py-1.5 text-[12px] rounded-pill border transition-colors"
          :class="statusFilter === val ? 'bg-primary text-white border-primary' : 'border-hairline text-ink-mute hover:border-primary'">
          {{ label }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="text-center py-10 text-ink-mute">加载中...</div>

    <div v-else class="bg-white rounded-2xl overflow-hidden shadow-card">
      <table class="w-full text-left">
        <thead>
          <tr class="border-b border-hairline text-[13px] font-normal text-ink-mute">
            <th class="px-6 py-4">商品</th>
            <th class="px-6 py-4">价格</th>
            <th class="px-6 py-4">销量</th>
            <th class="px-6 py-4">状态</th>
            <th class="px-6 py-4">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in products" :key="p.id" class="border-b border-hairline/50 hover:bg-canvas-soft/50 transition-colors">
            <td class="px-6 py-4">
              <div class="flex items-center gap-3">
                <img :src="p.mainImage" class="w-12 h-12 rounded-lg object-cover bg-canvas-soft" loading="lazy">
                <div>
                  <router-link :to="`/admin/products/${p.id}/edit`" class="text-[14px] font-normal text-ink hover:text-primary transition-colors">{{ p.name }}</router-link>
                  <p class="text-[11px] text-ink-mute">{{ p.categoryName }}</p>
                </div>
              </div>
            </td>
            <td class="px-6 py-4 text-[14px] font-tabular text-ink">¥{{ p.price }}</td>
            <td class="px-6 py-4 text-[14px] text-ink-mute">{{ p.sales || 0 }}</td>
            <td class="px-6 py-4">
              <span class="px-2.5 py-1 text-[11px] rounded-pill" :class="p.status === 1 ? 'bg-emerald/10 text-emerald' : 'bg-ink-mute/10 text-ink-mute'">
                {{ p.status === 1 ? '上架' : '下架' }}
              </span>
            </td>
            <td class="px-6 py-4">
              <div class="flex items-center gap-3">
                <router-link :to="`/admin/products/${p.id}/edit`" class="text-[12px] text-ink-mute hover:text-primary transition-colors">编辑</router-link>
                <button @click="changeStatus(p)" class="text-[12px] font-normal transition-colors"
                  :class="p.status === 1 ? 'text-amber hover:text-amber/80' : 'text-primary hover:text-primary-deep'">
                  {{ p.status === 1 ? '下架' : '上架' }}
                </button>
                <button @click="deleteProduct(p)" class="text-[12px] text-red hover:text-red/70 transition-colors">删除</button>
              </div>
            </td>
          </tr>
          <tr v-if="products.length === 0">
            <td colspan="5" class="px-6 py-12 text-center text-ink-mute">暂无商品</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
