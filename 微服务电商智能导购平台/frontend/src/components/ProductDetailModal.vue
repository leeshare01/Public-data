<script setup>
import { ref, watch } from 'vue'
import { useUIStore } from '../stores/ui'
import { useCartStore } from '../stores/cart'
import { useToastStore } from '../stores/toast'

const props = defineProps({
  visible: { type: Boolean, default: false },
})
const emit = defineEmits(['close'])
const ui = useUIStore()
const cartStore = useCartStore()
const toast = useToastStore()

const selectedColor = ref('')
const selectedSize = ref('')

watch(() => props.visible, (v) => {
  if (v) {
    const p = ui.detailProduct
    if (p) {
      selectedColor.value = p.colors?.[0] || ''
      selectedSize.value = p.sizes?.[0] || ''
    }
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
})

function close() {
  emit('close')
}

function addToCart() {
  const p = ui.detailProduct
  if (!p) return
  let cart = JSON.parse(localStorage.getItem('eshop_cart') || '[]')
  const existing = cart.find(item => item.skuId === p.id)
  if (existing) {
    existing.quantity++
  } else {
    cart.push({
      skuId: p.id, productId: p.id, productName: p.name,
      price: p.price, image: p.image || p.mainImage,
      specInfo: [selectedColor.value, selectedSize.value].filter(Boolean).join(' / '),
      quantity: 1,
    })
  }
  localStorage.setItem('eshop_cart', JSON.stringify(cart))
  toast.show(`${p.name} 已添加到购物车`)
}
</script>

<template>
  <Teleport to="body">
    <div class="modal-overlay" :class="{ active: visible }">
      <div class="backdrop" @click="close" />
      <div class="modal-panel w-full max-w-2xl bg-white rounded-2xl shadow-2xl mx-4">
        <div v-if="ui.detailProduct" class="flex flex-col md:flex-row">
          <div class="md:w-1/2 bg-canvas-soft min-h-[300px] flex items-center">
            <img :src="ui.detailProduct.image || ui.detailProduct.mainImage" :alt="ui.detailProduct.name"
              width="400" height="400" loading="lazy"
              class="w-full h-full object-cover object-top" style="max-height: 400px;">
          </div>
          <div class="md:w-1/2 p-8">
            <div class="flex items-start justify-between mb-2">
              <div>
                <span class="text-[11px] font-normal text-ink-mute uppercase tracking-wider">{{ ui.detailProduct.categoryName || ui.detailProduct.category }}</span>
                <span v-if="ui.detailProduct.tag" class="ml-2 px-2 py-0.5 text-[10px] font-normal rounded-pill"
                  :class="{'bg-ruby/10 text-ruby': ui.detailProduct.tag==='热销','bg-emerald/10 text-emerald': ui.detailProduct.tag==='新品','bg-amber/10 text-amber': ui.detailProduct.tag==='特惠'}">{{ ui.detailProduct.tag }}</span>
              </div>
              <button @click="close" class="w-8 h-8 rounded-lg hover:bg-canvas-soft flex items-center justify-center transition-colors flex-shrink-0">
                <svg class="w-4 h-4 text-ink-mute" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M6 18L18 6M6 6l12 12"/>
                </svg>
              </button>
            </div>
            <h2 class="text-[22px] font-light text-ink leading-snug mb-3">{{ ui.detailProduct.name }}</h2>
            <div class="flex items-center gap-2 mb-4">
              <span class="text-[24px] font-light text-ink font-tabular">¥{{ ui.detailProduct.price }}</span>
              <span v-if="ui.detailProduct.originalPrice" class="text-[15px] font-normal text-ink-mute line-through font-tabular">¥{{ ui.detailProduct.originalPrice }}</span>
            </div>
            <div v-if="ui.detailProduct.rating" class="flex items-center gap-1 mb-5">
              <span class="text-[14px] text-amber">★ {{ ui.detailProduct.rating }}</span>
              <span class="text-[13px] text-ink-mute/60">({{ ui.detailProduct.reviewsCount || ui.detailProduct.sales || 0 }} 条评价)</span>
            </div>
            <p class="text-[14px] font-light text-ink-secondary leading-relaxed mb-6">{{ ui.detailProduct.description || ui.detailProduct.subtitle }}</p>

            <div v-if="ui.detailProduct.colors" class="mb-4">
              <h4 class="text-[12px] font-normal text-ink mb-2">颜色</h4>
              <div class="flex gap-2 flex-wrap">
                <button v-for="c in ui.detailProduct.colors" :key="c" @click="selectedColor = c"
                  class="sku-btn" :class="{ active: selectedColor === c }">{{ c }}</button>
              </div>
            </div>
            <div v-if="ui.detailProduct.sizes" class="mb-4">
              <h4 class="text-[12px] font-normal text-ink mb-2">规格</h4>
              <div class="flex gap-2 flex-wrap">
                <button v-for="s in ui.detailProduct.sizes" :key="s" @click="selectedSize = s"
                  class="sku-btn" :class="{ active: selectedSize === s }">{{ s }}</button>
              </div>
            </div>

            <div v-if="ui.detailProduct.specs" class="mb-5">
              <h4 class="text-[12px] font-normal text-ink mb-3 uppercase tracking-wider">规格参数</h4>
              <div class="grid grid-cols-2 gap-2">
                <div v-for="spec in ui.detailProduct.specs" :key="spec" class="text-[12px]">
                  <span class="text-ink-mute">{{ spec.split(': ')[0] }}</span>
                  <span class="text-ink ml-1">{{ spec.split(': ').slice(1).join(': ') }}</span>
                </div>
              </div>
            </div>

            <button @click="addToCart"
              class="w-full py-3 bg-primary text-white text-[14px] font-normal rounded-pill hover:bg-primary-deep active:bg-primary-press transition-colors duration-200 flex items-center justify-center gap-2">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 100 4 2 2 0 000-4z"/>
              </svg>
              加入购物车
            </button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>
