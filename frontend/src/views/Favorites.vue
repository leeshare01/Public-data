<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useFavoriteStore } from '../stores/favorite'
import { productApi } from '../api/product'
import { useToastStore } from '../stores/toast'

const router = useRouter()
const userStore = useUserStore()
const favoriteStore = useFavoriteStore()
const toast = useToastStore()

const products = ref([])
const loading = ref(false)
const loaded = ref(false)

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    router.push('/')
    return
  }
  await loadFavorites()
})

async function loadFavorites() {
  const ids = favoriteStore.getIds()
  if (ids.length === 0) {
    loaded.value = true
    return
  }

  loading.value = true
  try {
    const list = await productApi.batchGetProducts(ids)
    // 保持收藏顺序
    const map = {}
    if (list && Array.isArray(list)) {
      list.forEach(p => { map[p.id] = p })
    }
    products.value = ids.map(id => map[id]).filter(Boolean)
  } catch {
    products.value = []
  } finally {
    loading.value = false
    loaded.value = true
  }
}

function goProduct(id) {
  router.push(`/product/${id}`)
}

async function toggleFavorite(productId) {
  const now = favoriteStore.toggle(productId)
  toast.show(now ? '已收藏' : '已取消收藏')
  if (!now) {
    // 取消收藏后从列表移除
    products.value = products.value.filter(p => p.id !== productId)
  }
}

function formatPrice(price) {
  if (price == null) return '-'
  return '¥' + Number(price).toLocaleString()
}
</script>

<template>
  <div class="page-container pt-8 pb-24">
    <h1 class="text-[32px] font-light text-ink mb-8">我的收藏</h1>

    <div v-if="loading" class="text-center py-20 text-ink-mute">加载中...</div>

    <div v-else-if="!loaded" class="text-center py-20 text-ink-mute">加载中...</div>

    <div v-else-if="products.length === 0" class="text-center py-20">
      <div class="w-24 h-24 rounded-2xl bg-canvas-soft flex items-center justify-center mx-auto mb-4">
        <svg class="w-12 h-12 text-ink-mute/30" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
        </svg>
      </div>
      <p class="text-[16px] font-light text-ink-mute">暂无收藏商品</p>
      <p class="text-[12px] font-normal text-ink-mute/60 mt-1">去逛逛，把心仪的商品收藏起来</p>
      <button @click="router.push('/')" class="mt-4 px-5 py-2 bg-primary text-white text-[13px] font-normal rounded-pill hover:bg-primary-deep transition-colors">去购物</button>
    </div>

    <!-- 收藏商品网格 -->
    <div v-else class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
      <div v-for="p in products" :key="p.id"
        class="bg-white rounded-2xl shadow-card hover:shadow-card-hover transition-all duration-300 overflow-hidden group cursor-pointer"
        @click="goProduct(p.id)">
        <!-- 商品图片 -->
        <div class="aspect-square bg-canvas-soft overflow-hidden relative">
          <img v-if="p.mainImage"
            :src="p.mainImage"
            :alt="p.name"
            class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
            @error="($event.target.src = '')">
          <div v-else class="w-full h-full flex items-center justify-center">
            <svg class="w-12 h-12 text-ink-mute/20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/>
            </svg>
          </div>
          <!-- 收藏按钮 -->
          <button @click.stop="toggleFavorite(p.id)"
            class="absolute top-2 right-2 w-8 h-8 bg-white/80 backdrop-blur rounded-full flex items-center justify-center hover:bg-white transition-colors shadow-sm">
            <svg class="w-4 h-4 text-ruby" :class="{ 'fill-ruby': favoriteStore.isFavorited(p.id) }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
            </svg>
          </button>
          <!-- 下架标记 -->
          <div v-if="p.status !== undefined && p.status !== 1"
            class="absolute top-2 left-2 px-2 py-0.5 bg-ink/60 text-white text-[10px] rounded-pill backdrop-blur">
            已下架
          </div>
        </div>

        <!-- 商品信息 -->
        <div class="p-3">
          <p class="text-[13px] font-light text-ink truncate mb-1">{{ p.name || '商品' }}</p>
          <div class="flex items-center justify-between">
            <span class="text-[15px] font-normal text-ink font-tabular">{{ formatPrice(p.price) }}</span>
            <span v-if="p.sales !== undefined && p.sales > 0" class="text-[11px] font-normal text-ink-mute">已售 {{ p.sales }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="products.length > 0" class="text-center mt-8">
      <span class="text-[13px] text-ink-mute/60">共 {{ favoriteStore.count }} 件收藏</span>
    </div>
  </div>
</template>
