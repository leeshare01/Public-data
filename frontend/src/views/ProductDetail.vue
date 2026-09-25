<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useProductStore } from '../stores/product'
import { useCartStore } from '../stores/cart'
import { useToastStore } from '../stores/toast'
import { useFavoriteStore } from '../stores/favorite'

const route = useRoute()
const router = useRouter()
const productStore = useProductStore()
const cartStore = useCartStore()
const toast = useToastStore()
const favoriteStore = useFavoriteStore()

const product = ref(null)
const loading = ref(true)
const selectedImage = ref(0)
const brokenImages = ref(new Set())  // 记录加载失败的图片索引

// ============ SKU 选择状态 ============
const selectedSpecs = ref({})          // { "颜色": "白色", "尺寸": "M" }
const selectedSku = ref(null)

const tagColors = {
  '热销': 'bg-ruby/10 text-ruby',
  '新品': 'bg-emerald/10 text-emerald',
  '特惠': 'bg-amber/10 text-amber',
}

// ============ 从 skuList 提取规格选项 ============
const specOptions = computed(() => {
  if (!product.value?.skuList?.length) return []
  const specMap = {}   // { "颜色": Set{"白色","黑色"}, "尺寸": Set{"S","M","L"} }
  for (const sku of product.value.skuList) {
    if (!sku.specValues) continue
    let parsed = sku.specValues
    if (typeof parsed === 'string') {
      try { parsed = JSON.parse(parsed) } catch { continue }
    }
    for (const [key, val] of Object.entries(parsed)) {
      if (!specMap[key]) specMap[key] = new Set()
      specMap[key].add(String(val))
    }
  }
  return Object.entries(specMap).map(([name, vals]) => ({
    name,
    values: Array.from(vals),
  }))
})

// ============ 计算 Tag ============
const computedTag = computed(() => {
  const p = product.value
  if (!p) return null
  // 超过 5000 销量标记为热销
  if (p.sales >= 5000) return '热销'
  // 30 天内创建标记为新品
  if (p.createTime) {
    const created = new Date(p.createTime)
    const now = new Date()
    const daysDiff = (now - created) / (1000 * 60 * 60 * 24)
    if (daysDiff <= 30) return '新品'
  }
  return null
})

// ============ 图片列表 ============
const images = computed(() => {
  const p = product.value
  if (!p) return []
  const list = [p.mainImage]
  if (p.subImages) {
    let parsed = p.subImages
    if (typeof parsed === 'string') {
      try { parsed = JSON.parse(parsed) } catch { /* ignore */ }
    }
    if (Array.isArray(parsed)) {
      list.push(...parsed)
    }
  }
  // 去重后过滤掉加载失败的
  const unique = [...new Set(list)]
  return unique.filter((_, idx) => !brokenImages.value.has(idx))
})

function onImageError(idx) {
  brokenImages.value = new Set([...brokenImages.value, idx])
  // 如果当前选中的是坏图，跳到第一张可用图
  if (!images.value[selectedImage.value]) {
    selectedImage.value = 0
  }
}

// ============ 收藏 ============
function toggleFavorite() {
  if (!product.value) return
  const now = favoriteStore.toggle(product.value.id)
  toast.show(now ? '已收藏' : '已取消收藏')
}

// ============ SKU 价格和库存 ============
const skuPrice = computed(() => {
  if (selectedSku.value) return selectedSku.value.price
  // 显示价格区间
  if (!product.value?.skuList?.length) return product.value?.price
  const prices = product.value.skuList.map(s => s.price).filter(Boolean)
  if (!prices.length) return product.value?.price
  const min = Math.min(...prices)
  const max = Math.max(...prices)
  return min === max ? min : { min, max }
})

const skuStock = computed(() => {
  if (selectedSku.value) return selectedSku.value.stock
  return null
})

const canAddToCart = computed(() => {
  // 有 SKU 时必须选完所有规格才能加购
  if (specOptions.value.length > 0) return selectedSku.value !== null
  return true
})

// ============ 选择规格 ============
function selectSpec(specName, value) {
  selectedSpecs.value = { ...selectedSpecs.value, [specName]: value }
  matchSku()
}

function isSpecSelected(specName, value) {
  return selectedSpecs.value[specName] === value
}

function matchSku() {
  const p = product.value
  if (!p?.skuList?.length) return
  const selected = selectedSpecs.value
  const requiredCount = specOptions.value.length

  // 如果还没选完所有规格，不清空已选
  if (Object.keys(selected).length < requiredCount) {
    selectedSku.value = null
    return
  }

  // 找完全匹配的 SKU
  for (const sku of p.skuList) {
    let parsed = sku.specValues
    if (typeof parsed === 'string') {
      try { parsed = JSON.parse(parsed) } catch { continue }
    }
    const matches = Object.entries(selected).every(([k, v]) => parsed[k] === v)
    if (matches) {
      selectedSku.value = sku
      return
    }
  }
  selectedSku.value = null
}

function isSkuOutOfStock(specName, value) {
  // 检查某个规格值对应的 SKU 是否全部无货
  const p = product.value
  if (!p?.skuList?.length) return false
  const otherSpecs = { ...selectedSpecs.value }
  delete otherSpecs[specName]
  const hasAny = p.skuList.some(sku => {
    let parsed = sku.specValues
    if (typeof parsed === 'string') {
      try { parsed = JSON.parse(parsed) } catch { return false }
    }
    const matchesOther = Object.entries(otherSpecs).every(([k, v]) => parsed[k] === v)
    return matchesOther && parsed[specName] === value && (sku.stock || 0) > 0
  })
  return !hasAny
}

// ============ 加购 ============
async function addToCart() {
  if (!product.value || !canAddToCart.value) return
  const skuId = selectedSku.value?.id || product.value.id
  try {
    await cartStore.addItem(skuId, 1)
    toast.show(`${product.value.name} 已添加到购物车`)
  } catch {
    try {
      let cart = JSON.parse(localStorage.getItem('eshop_cart') || '[]')
      const existing = cart.find(item => item.skuId === skuId)
      if (existing) existing.quantity++
      else cart.push({
        skuId, productId: product.value.id,
        productName: product.value.name,
        price: selectedSku.value?.price || product.value.price,
        image: selectedSku.value?.image || product.value.mainImage,
        quantity: 1,
      })
      localStorage.setItem('eshop_cart', JSON.stringify(cart))
      toast.show(`${product.value.name} 已添加到购物车`)
    } catch {
      toast.show(`${product.value?.name || '商品'} 已添加到购物车`)
    }
  }
}

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    const data = await productStore.fetchProductDetail(id)
    if (data) {
      product.value = data
      // 默认选中第一个 SKU 的规格
      if (data.skuList?.length && specOptions.value.length > 0) {
        for (const opt of specOptions.value) {
          selectSpec(opt.name, opt.values[0])
        }
      }
    }
  } catch {
    const mock = window.__mockProducts?.find(p => p.id === id)
    if (mock) product.value = mock
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page-container pt-8 pb-24">
    <button @click="router.back()" class="flex items-center gap-1 text-[14px] font-normal text-ink-mute hover:text-primary transition-colors mb-6">
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
      返回
    </button>

    <div v-if="loading" class="text-center py-20 text-ink-mute">加载中...</div>
    <div v-else-if="!product" class="text-center py-20">
      <p class="text-[16px] font-light text-ink-mute">商品不存在</p>
    </div>

    <div v-else class="flex flex-col md:flex-row gap-10">
      <!-- ========== 商品图片 ========== -->
      <div class="md:w-1/2">
        <div class="bg-canvas-soft rounded-2xl overflow-hidden mb-3 relative">
          <img :src="images[selectedImage]" :alt="product.name" class="w-full object-cover aspect-square" loading="lazy" @error="onImageError(selectedImage)">
          <!-- Tag 角标 -->
          <span v-if="computedTag" class="absolute top-4 left-4 px-3 py-1 rounded-pill text-[12px] font-normal"
            :class="tagColors[computedTag]">{{ computedTag }}</span>
        </div>
        <!-- 缩略图列表（自适应数量） -->
        <div v-if="images.length > 1" class="flex gap-2 flex-wrap">
          <button v-for="(img, idx) in images" :key="idx"
            @click="selectedImage = idx"
            class="rounded-lg overflow-hidden border-2 transition-all flex-shrink-0"
            :class="[selectedImage === idx ? 'border-primary' : 'border-transparent opacity-60 hover:opacity-100', images.length <= 3 ? 'flex-1 min-w-[60px] max-w-[80px]' : 'w-16 h-16']">
            <div class="aspect-square">
              <img :src="img" :alt="`${product.name} ${idx + 1}`" class="w-full h-full object-cover" loading="lazy" @error="onImageError(idx)">
            </div>
          </button>
        </div>
      </div>

      <!-- ========== 商品信息 ========== -->
      <div class="md:w-1/2">
        <div class="flex items-center gap-2 mb-2">
          <span class="text-[11px] font-normal text-ink-mute uppercase tracking-wider">{{ product.categoryName || product.category }}</span>
          <span v-if="product.brand" class="text-[11px] font-normal text-ink-mute/60">｜{{ product.brand }}</span>
        </div>
        <h1 class="text-[28px] font-light text-ink leading-snug mb-3">{{ product.name }}</h1>

        <!-- 评分（后端无 rating 字段时隐藏） -->
        <div v-if="product.rating" class="flex items-center gap-1 mb-3">
          <span class="text-[14px] text-amber">★</span>
          <span class="text-[14px] font-normal text-ink">{{ product.rating }}</span>
          <span v-if="product.sales" class="text-[13px] font-normal text-ink-mute/60 ml-1">(已售 {{ product.sales }})</span>
        </div>
        <div v-else-if="product.sales" class="text-[13px] font-normal text-ink-mute/60 mb-3">已售 {{ product.sales }}</div>

        <!-- 价格 -->
        <div class="flex items-baseline gap-3 mb-5">
          <template v-if="typeof skuPrice === 'object'">
            <span class="text-[32px] font-light text-ink font-tabular">¥{{ skuPrice.min }} ~ ¥{{ skuPrice.max }}</span>
          </template>
          <template v-else>
            <span class="text-[32px] font-light text-ink font-tabular">¥{{ skuPrice }}</span>
            <span v-if="product.originalPrice && product.originalPrice > skuPrice" class="text-[16px] font-normal text-ink-mute line-through font-tabular">¥{{ product.originalPrice }}</span>
          </template>
          <span v-if="skuStock !== null" class="text-[12px] font-normal text-ink-mute/60 ml-auto"
            :class="{ 'text-ruby': skuStock <= 10 }">库存 {{ skuStock }}</span>
        </div>

        <!-- 副标题/描述 -->
        <p v-if="product.subtitle" class="text-[15px] font-light text-ink-secondary leading-relaxed mb-6">{{ product.subtitle }}</p>
        <div v-if="product.description" class="text-[14px] font-light text-ink-secondary leading-relaxed mb-8" v-html="product.description"></div>

        <!-- ========== SKU 选择器 ========== -->
        <div v-if="specOptions.length > 0" class="mb-8 border-t border-hairline pt-6 space-y-5">
          <div v-for="spec in specOptions" :key="spec.name">
            <h4 class="text-[13px] font-normal text-ink mb-2">{{ spec.name }}</h4>
            <div class="flex flex-wrap gap-2">
              <button v-for="val in spec.values" :key="val"
                @click="selectSpec(spec.name, val)"
                :disabled="isSkuOutOfStock(spec.name, val)"
                class="px-4 py-2 rounded-lg border text-[13px] transition-all duration-150"
                :class="[
                  isSpecSelected(spec.name, val)
                    ? 'border-primary bg-primary-bg-subdued text-primary font-normal'
                    : isSkuOutOfStock(spec.name, val)
                      ? 'border-hairline/30 text-ink-mute/30 line-through cursor-not-allowed'
                      : 'border-hairline text-ink-mute hover:border-primary/50 hover:text-ink'
                ]">
                {{ val }}
              </button>
            </div>
          </div>
        </div>

        <!-- ========== 商品参数 ========== -->
        <div v-if="product.attributes && product.attributes.length > 0" class="mb-8 border-t border-hairline pt-4">
          <h4 class="text-[13px] font-normal text-ink mb-3">商品参数</h4>
          <div class="grid grid-cols-2 gap-x-6 gap-y-2">
            <div v-for="(attr, idx) in product.attributes" :key="idx" class="flex justify-between py-1 border-b border-hairline/50">
              <span class="text-[13px] text-ink-mute">{{ attr.attrName }}</span>
              <span class="text-[13px] text-ink">{{ attr.attrValue }}</span>
            </div>
          </div>
        </div>

        <!-- ========== 操作按钮 ========== -->
        <div class="flex gap-3">
          <button @click="addToCart"
            :disabled="!canAddToCart"
            class="flex-1 py-3.5 text-[15px] font-normal rounded-pill transition-all duration-200"
            :class="canAddToCart
              ? 'bg-primary text-white hover:bg-primary-deep active:bg-primary-press cursor-pointer'
              : 'bg-hairline text-ink-mute/50 cursor-not-allowed'">
            {{ specOptions.length > 0 && !selectedSku ? '请选择完整规格' : '加入购物车' }}
          </button>
          <!-- 收藏按钮已隐藏（功能废弃） -->
        </div>
      </div>
    </div>
  </div>
</template>
