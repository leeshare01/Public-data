<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  product: { type: Object, required: true },
  index: { type: Number, default: 0 },
  isFav: { type: Boolean, default: false },
})

const emit = defineEmits(['toggle-fav', 'add-cart', 'show-detail'])
const router = useRouter()

const imgError = ref(false)

const tagColors = {
  '热销': 'bg-ruby/10 text-ruby',
  '新品': 'bg-emerald/10 text-emerald',
  '特惠': 'bg-amber/10 text-amber',
}

function getTagClass(tag) {
  return tagColors[tag] || 'bg-primary-bg-subdued text-primary-deep'
}

function goDetail() {
  router.push(`/product/${props.product.id}`)
}
</script>

<template>
  <div
    class="group bg-white rounded-[16px] overflow-hidden shadow-card hover:shadow-card-hover transition-all duration-300 card-enter"
    :style="{ animationDelay: `${(index % 6) * 60}ms` }"
  >
    <!-- 图片区域 -->
    <div class="product-image-container aspect-square bg-canvas-soft relative cursor-pointer" @click="goDetail">
      <img v-if="!imgError"
        :src="product.mainImage || product.image"
        :alt="product.name"
        loading="lazy"
        class="w-full h-full object-cover object-top"
        @error="imgError = true"
      >
      <div v-else class="w-full h-full flex items-center justify-center text-ink-mute/30">
        <svg class="w-12 h-12" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/>
        </svg>
      </div>
      <span
        v-if="product.tag"
        class="absolute top-3 left-3 px-2.5 py-1 rounded-pill text-[10px] font-normal"
        :class="getTagClass(product.tag)"
        style="letter-spacing: 0.1px;"
      >{{ product.tag }}</span>
      <!-- 收藏按钮 -->
      <button
        @click.stop="emit('toggle-fav', product.id)"
        class="absolute top-3 right-3 w-8 h-8 bg-white/80 backdrop-blur-sm rounded-full flex items-center justify-center hover:bg-white transition-all shadow-float"
      >
        <svg
          class="w-[18px] h-[18px] transition-all"
          :class="[isFav ? 'text-ruby fill-ruby' : 'text-ink-mute fill-transparent']"
          stroke="currentColor" viewBox="0 0 24 24" stroke-width="2"
        >
          <path stroke-linecap="round" stroke-linejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
        </svg>
      </button>
      <!-- 添加购物车 -->
      <button
        @click.stop="emit('add-cart', product.id)"
        class="absolute bottom-3 right-3 w-10 h-10 bg-white rounded-full shadow-float opacity-0 group-hover:opacity-100 transform translate-y-2 group-hover:translate-y-0 transition-all duration-300 flex items-center justify-center hover:bg-primary hover:text-white text-ink"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 4v16m8-8H4"/>
        </svg>
      </button>
    </div>
    <!-- 信息区域 -->
    <div class="p-4 cursor-pointer" @click="goDetail">
      <p class="text-[11px] font-normal text-ink-mute mb-1 uppercase tracking-wider" style="letter-spacing: 0.1px;">{{ product.categoryName || product.category }}</p>
      <h3 class="text-[15px] font-light text-ink leading-snug mb-2 line-clamp-2">{{ product.name }}</h3>
      <div class="flex items-center gap-2">
        <span class="text-[18px] font-light text-ink font-tabular tracking-tight">¥{{ product.price }}</span>
        <span v-if="product.originalPrice" class="text-[13px] font-normal text-ink-mute line-through font-tabular">¥{{ product.originalPrice }}</span>
      </div>
      <div v-if="product.rating" class="flex items-center gap-1 mt-1.5">
        <span class="text-[11px] text-amber">★ {{ product.rating }}</span>
        <span class="text-[11px] text-ink-mute/60">({{ product.sales || product.reviewsCount || 0 }})</span>
      </div>
    </div>
  </div>
</template>
