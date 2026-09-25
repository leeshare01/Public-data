<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useProductStore } from '../stores/product'
import { useCartStore } from '../stores/cart'
import { useToastStore } from '../stores/toast'
import { productApi } from '../api/product'
import ProductCard from '../components/ProductCard.vue'
import { useFavoriteStore } from '../stores/favorite'

const productStore = useProductStore()
const cartStore = useCartStore()
const toast = useToastStore()
const route = useRoute()

// ============ 搜索与筛选状态 ============
const searchQuery = ref('')
const selectedCategoryId = ref(null)  // null = 全部
const sortBy = ref('default')         // frontend 格式
const page = ref(1)
const pageSize = 50
const categories = ref([])
const loading = ref(false)
const serverError = ref(false)

// ============ Banner 轮播 ============
const bannerProducts = ref([])
const currentSlide = ref(0)
let carouselTimer = null

function nextSlide() {
  if (bannerProducts.value.length === 0) return
  currentSlide.value = (currentSlide.value + 1) % bannerProducts.value.length
}

function prevSlide() {
  if (bannerProducts.value.length === 0) return
  currentSlide.value = (currentSlide.value - 1 + bannerProducts.value.length) % bannerProducts.value.length
}

function goToSlide(idx) {
  currentSlide.value = idx
}

function pauseCarousel() {
  if (carouselTimer) { clearInterval(carouselTimer); carouselTimer = null }
}

function startCarousel() {
  pauseCarousel()
  if (bannerProducts.value.length > 1) {
    carouselTimer = setInterval(nextSlide, 4000)
  }
}

async function loadBannerProducts() {
  try {
    const data = await productApi.getProducts({ sortBy: 'sales', asc: false, size: 5 })
    bannerProducts.value = data.records || []
    startCarousel()
  } catch {
    bannerProducts.value = []
  }
}

// 商品区域 DOM 引用（搜索后自动滚动）
const productsSection = ref(null)

// 直接使用 store 的数据（后端已处理好搜索与排序）
const products = computed(() => productStore.products)
const total = computed(() => productStore.total)

// ============ 前端分页展示 ============
const displayProducts = computed(() => {
  return products.value.slice(0, page.value * pageSize)
})
const hasMore = computed(() => {
  return displayProducts.value.length < total.value
})

// ============ sortBy 映射到后端格式 ============
const sortByMap = {
  'price-asc':  { sortBy: 'price', asc: true },
  'price-desc': { sortBy: 'price', asc: false },
  'sales':      { sortBy: 'sales', asc: false },
  'newest':     { sortBy: 'createTime', asc: false },
}

// ============ 收藏 ============
const favoriteStore = useFavoriteStore()

function toggleFavorite(productId) {
  const now = favoriteStore.toggle(productId)
  toast.show(now ? '已收藏' : '已取消收藏')
}

async function addToCart(productId, productName) {
  try {
    await cartStore.addItem(productId, 1)
    toast.show('已添加到购物车')
  } catch {
    // fallback localStorage（防御性编程，防止任何异常导致静默失败）
    try {
      let cart = JSON.parse(localStorage.getItem('eshop_cart') || '[]')
      const existing = cart.find(item => item.skuId === productId)
      if (existing) {
        existing.quantity++
      } else {
        const product = products.value.find(p => p.id === productId)
        cart.push({
          skuId: productId, productId,
          productName: product?.name || productName || '商品',
          price: product?.price || 0,
          image: product?.mainImage || '',
          quantity: 1,
        })
      }
      localStorage.setItem('eshop_cart', JSON.stringify(cart))
      toast.show(`${productName || '商品'} 已添加到购物车`)
    } catch {
      // 极端情况也保证有反馈
      toast.show('已添加到购物车')
    }
  }
}

function loadMore() {
  if (hasMore.value) page.value++
}

function selectCategory(categoryId) {
  selectedCategoryId.value = categoryId
  searchQuery.value = ''
  page.value = 1
  loadProducts()
}

function clearFilters() {
  searchQuery.value = ''
  selectedCategoryId.value = null
  sortBy.value = 'default'
  page.value = 1
  loadProducts()
}

// ============ 核心搜索函数 ============
async function loadProducts() {
  loading.value = true
  serverError.value = false
  page.value = 1
  try {
    const params = { size: pageSize }
    if (searchQuery.value.trim()) {
      params.keyword = searchQuery.value.trim()
    }
    if (selectedCategoryId.value) {
      params.categoryId = selectedCategoryId.value
    }
    // 排序映射：前端格式 → 后端格式
    if (sortBy.value !== 'default') {
      const mapped = sortByMap[sortBy.value]
      if (mapped) {
        params.sortBy = mapped.sortBy
        params.asc = mapped.asc
      } else {
        params.sortBy = sortBy.value
      }
    }
    await productStore.fetchProducts(params)
    // 搜索后滚动到商品列表
    if (searchQuery.value.trim()) {
      setTimeout(() => {
        productsSection.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }, 100)
    }
  } catch (e) {
    console.error('搜索请求失败:', e)
    serverError.value = true
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const tree = await productApi.getCategoryTree()
    if (tree && tree.length > 0) {
      categories.value = tree
    }
  } catch {
    // 分类加载失败不影响搜索功能
  }
}

// 监听路由参数变化（NavBar 搜索跳转到首页时触发）
watch(() => route.query.search, (newSearch) => {
  if (newSearch !== undefined) {
    searchQuery.value = String(newSearch || '').trim()
    loadProducts()
  }
})

onMounted(async () => {
  // 从 URL 参数读取搜索关键词（NavBar 搜索跳转时带入）
  const searchParam = route.query.search
  if (searchParam) {
    searchQuery.value = String(searchParam).trim()
  }
  await loadCategories()
  await Promise.all([loadProducts(), loadBannerProducts()])
})

onUnmounted(() => {
  pauseCarousel()
})
</script>

<template>
  <div>
    <section class="page-container pt-12 pb-8 text-center">
      <h1 class="text-[48px] font-light leading-[1.15] tracking-tight text-ink mb-4" style="letter-spacing: -0.96px;">
        发现心仪好物
      </h1>
      <p class="text-[16px] font-light text-ink-mute mb-8 max-w-md mx-auto leading-relaxed">
        精选品质商品，极简购物体验
      </p>

      <div class="max-w-2xl mx-auto relative search-glow rounded-xl transition-shadow duration-300">
        <div class="relative flex items-center bg-white rounded-xl border border-hairline-input shadow-card hover:shadow-card-hover transition-shadow duration-300">
          <svg class="absolute left-5 w-5 h-5 text-ink-mute" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
          </svg>
          <input v-model="searchQuery" type="text" placeholder="搜索商品名称或品牌..."
            class="w-full pl-14 pr-32 py-4 bg-transparent text-[15px] font-light text-ink placeholder-ink-mute focus:outline-none rounded-xl">
          <button @click="loadProducts" class="absolute right-2 px-6 py-2.5 bg-primary text-white text-[14px] font-normal rounded-pill hover:bg-primary-deep active:bg-primary-press transition-colors duration-200">搜索</button>
        </div>
      </div>
    </section>

    <!-- ========== Banner 轮播 ========== -->
    <section class="page-container mb-10">
      <div v-if="bannerProducts.length > 0"
        class="relative rounded-xl overflow-hidden bg-canvas-soft shadow-card group"
        @mouseenter="pauseCarousel" @mouseleave="startCarousel"
        @focusin="pauseCarousel" @focusout="startCarousel">

        <!-- 滑动轨道 -->
        <div class="flex transition-transform duration-500 ease-in-out"
          :style="{ transform: `translateX(-${currentSlide * 100}%)` }">
          <router-link v-for="(item, idx) in bannerProducts" :key="item.id"
            :to="`/product/${item.id}`" class="min-w-full relative block focus:outline-none"
            :tabindex="currentSlide === idx ? 0 : -1" :aria-hidden="currentSlide !== idx">

            <div class="aspect-[21/9] max-h-[420px] bg-canvas-soft">
              <img :src="item.mainImage" :alt="item.name"
                class="w-full h-full object-cover" loading="lazy">
            </div>
            <!-- 渐变遮罩 -->
            <div class="absolute inset-0 bg-gradient-to-t from-black/60 via-black/20 to-transparent"></div>
            <!-- 文字信息 -->
            <div class="absolute bottom-0 left-0 right-0 p-6 md:p-10 text-white">
              <span v-if="item.brand" class="inline-block text-[11px] font-normal uppercase tracking-wider opacity-80 mb-1">{{ item.brand }}</span>
              <h3 class="text-[20px] md:text-[28px] font-light leading-snug">{{ item.name }}</h3>
              <div class="flex items-center gap-3 mt-2">
                <span class="text-[18px] md:text-[24px] font-light font-tabular">¥{{ item.price }}</span>
                <span v-if="item.originalPrice && item.originalPrice > item.price"
                  class="text-[13px] md:text-[15px] font-normal line-through opacity-60">¥{{ item.originalPrice }}</span>
                <span v-if="item.sales >= 5000" class="ml-2 px-2 py-0.5 bg-ruby/20 text-ruby text-[11px] rounded-pill">热销</span>
              </div>
            </div>
          </router-link>
        </div>

        <!-- 左右箭头 -->
        <button @click.stop="prevSlide" aria-label="上一张"
          class="absolute left-3 top-1/2 -translate-y-1/2 w-9 h-9 md:w-11 md:h-11 rounded-full bg-white/20 backdrop-blur-sm text-white hover:bg-white/40 transition-all duration-200 flex items-center justify-center opacity-0 group-hover:opacity-100 focus:opacity-100">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
        </button>
        <button @click.stop="nextSlide" aria-label="下一张"
          class="absolute right-3 top-1/2 -translate-y-1/2 w-9 h-9 md:w-11 md:h-11 rounded-full bg-white/20 backdrop-blur-sm text-white hover:bg-white/40 transition-all duration-200 flex items-center justify-center opacity-0 group-hover:opacity-100 focus:opacity-100">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
        </button>

        <!-- 圆点指示器 -->
        <div class="absolute bottom-3 md:bottom-5 left-1/2 -translate-x-1/2 flex gap-2">
          <button v-for="(item, idx) in bannerProducts" :key="idx"
            @click.stop="goToSlide(idx)" :aria-label="`第${idx+1}张`"
            class="h-2 rounded-full transition-all duration-300 cursor-pointer"
            :class="currentSlide === idx ? 'bg-white w-6' : 'bg-white/40 hover:bg-white/70 w-2'">
          </button>
        </div>
      </div>
    </section>

    <section ref="productsSection" class="page-container pb-24">
      <!-- 分类导航 -->
      <div class="mb-8">
        <div class="flex items-center gap-2 flex-wrap">
          <button class="filter-btn" :class="{ active: selectedCategoryId === null }"
            @click="selectCategory(null)">全部</button>
          <button v-for="cat in categories" :key="cat.id" class="filter-btn"
            :class="{ active: selectedCategoryId === cat.id }"
            @click="selectCategory(cat.id)">{{ cat.name }}</button>
          <div class="flex-1"></div>
          <select v-model="sortBy" @change="loadProducts"
            class="px-4 py-2 bg-white border border-hairline rounded-lg text-[14px] font-light text-ink-mute focus:outline-none focus:border-primary cursor-pointer">
            <option value="default">默认排序</option>
            <option value="price-asc">价格从低到高</option>
            <option value="price-desc">价格从高到低</option>
            <option value="sales">销量优先</option>
            <option value="newest">最新上架</option>
          </select>
        </div>
        <div class="flex items-center justify-between mt-3">
          <p class="text-[13px] font-normal text-ink-mute">
            <span>{{ searchQuery ? `"${searchQuery}" 的搜索结果` : selectedCategoryId ? categories.find(c => c.id === selectedCategoryId)?.name || '已选分类' : '全部商品' }}</span>
            ｜共 <span class="font-tabular">{{ total }}</span> 件
            <span v-if="total > pageSize && displayProducts.length < total" class="text-ink-mute/60">（显示 {{ displayProducts.length }} 件）</span>
          </p>
          <button v-if="selectedCategoryId !== null || searchQuery" @click="clearFilters" class="text-[12px] font-normal text-ink-mute hover:text-primary transition-colors">清除筛选 ✕</button>
        </div>
      </div>

      <div v-if="loading" class="text-center py-20 text-ink-mute">加载中...</div>

      <div v-else-if="serverError" class="flex flex-col items-center justify-center py-20 text-center">
        <div class="w-24 h-24 rounded-2xl bg-canvas-soft flex items-center justify-center mb-4">
          <svg class="w-12 h-12 text-ink-mute/30" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M18.364 5.636a9 9 0 010 12.728M5.636 18.364a9 9 0 010-12.728M8.464 15.536a5 5 0 010-7.072M15.536 8.464a5 5 0 010 7.072"/>
          </svg>
        </div>
        <p class="text-[16px] font-light text-ink-mute">服务暂不可用，请稍后再试</p>
        <button @click="loadProducts" class="mt-4 px-5 py-2 bg-primary text-white text-[13px] font-normal rounded-pill hover:bg-primary-deep transition-colors">重新加载</button>
      </div>

      <div v-else-if="displayProducts.length > 0" class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6 gap-5">
        <ProductCard v-for="(p, i) in displayProducts" :key="p.id"
          :product="p" :index="i" :is-fav="favoriteStore.isFavorited(p.id)"
          @toggle-fav="toggleFavorite" @add-cart="(id) => addToCart(id, p.name)" />
      </div>

      <div v-else class="flex flex-col items-center justify-center py-20 text-center">
        <div class="w-24 h-24 rounded-2xl bg-canvas-soft flex items-center justify-center mb-4">
          <svg class="w-12 h-12 text-ink-mute/30" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
        </div>
        <p class="text-[16px] font-light text-ink-mute">暂无匹配的商品</p>
        <button @click="clearFilters" class="mt-4 px-5 py-2 bg-primary text-white text-[13px] font-normal rounded-pill hover:bg-primary-deep transition-colors">清除筛选</button>
      </div>

      <div class="text-center mt-12">
        <button v-if="hasMore" @click="loadMore" class="px-8 py-3 bg-white border border-hairline text-ink text-[14px] font-normal rounded-pill hover:border-primary hover:text-primary transition-colors duration-200 shadow-card">加载更多</button>
        <p v-if="total > 0 && !hasMore" class="text-[13px] font-normal text-ink-mute/60">— 已显示全部商品 —</p>
      </div>
    </section>
  </div>
</template>
