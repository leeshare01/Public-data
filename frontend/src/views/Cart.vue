<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import { useOrderStore } from '../stores/order'
import { useUserStore } from '../stores/user'
import { useToastStore } from '../stores/toast'

const router = useRouter()
const cartStore = useCartStore()
const orderStore = useOrderStore()
const userStore = useUserStore()
const toast = useToastStore()
const creating = ref(false)

async function loadCart() {
  if (userStore.isLoggedIn) {
    try {
      await cartStore.fetchItems()
      return
    } catch { /* fallback to localStorage */ }
  }
  // fallback: 从 localStorage 加载
  const local = JSON.parse(localStorage.getItem('eshop_cart') || '[]')
  // 转换为 store 格式
  cartStore.items = local.map(i => ({
    skuId: i.skuId, productId: i.productId, productName: i.productName || i.name,
    specInfo: i.specInfo || '',
    image: i.image, price: i.price, quantity: i.quantity,
    selected: true, stock: 999,
  }))
}

async function updateQuantity(skuId, quantity) {
  if (userStore.isLoggedIn) {
    try {
      await cartStore.updateQuantity(skuId, quantity)
      return
    } catch {
      // API 调用失败 → 降级到 localStorage
    }
  }
  const item = cartStore.items.find(i => i.skuId === skuId)
  if (item) item.quantity = quantity
  if (item.quantity <= 0) {
    cartStore.items = cartStore.items.filter(i => i.skuId !== skuId)
  }
  saveLocal()
}

async function removeItem(skuId) {
  if (userStore.isLoggedIn) {
    try {
      await cartStore.removeItem(skuId)
      return
    } catch {
      // API 调用失败 → 降级到 localStorage
    }
  }
  cartStore.items = cartStore.items.filter(i => i.skuId !== skuId)
  saveLocal()
  toast.show('已从购物车移除')
}

function saveLocal() {
  localStorage.setItem('eshop_cart', JSON.stringify(cartStore.items.map(i => ({
    skuId: i.skuId, productId: i.productId, productName: i.productName,
    specInfo: i.specInfo, image: i.image, price: i.price, quantity: i.quantity,
  }))))
}

async function checkout() {
  if (!userStore.isLoggedIn) {
    toast.show('请先登录再结算')
    return
  }
  if (cartStore.items.length === 0) {
    toast.show('购物车是空的')
    return
  }
  // 收集选中商品
  const selectedItems = cartStore.items.filter(i => i.selected !== false)
  if (selectedItems.length === 0) {
    toast.show('请至少选中一件商品')
    return
  }
  creating.value = true
  try {
    const orderData = {
      items: selectedItems.map(i => ({
        productId: i.productId || i.skuId,
        skuId: i.skuId,
        productName: i.productName,
        productImage: i.image,
        specValues: i.specInfo || '',
        price: i.price,
        quantity: i.quantity,
      })),
      consigneeName: userStore.user?.nickname || userStore.user?.username || '用户',
      consigneePhone: '138****0000',
      consigneeAddress: '默认地址（模拟）',
      remark: '',
    }

    await orderStore.createOrder(orderData)
    toast.show('🎉 订单提交成功！请前往订单页查看')
    // 清空购物车本地状态
    cartStore.items = []
    localStorage.removeItem('eshop_cart')
    router.push('/orders')
  } catch (e) {
    toast.show('下单失败: ' + (e.message || '请稍后重试'))
  } finally {
    creating.value = false
  }
}

onMounted(loadCart)
</script>

<template>
  <div class="page-container pt-8 pb-24">
    <h1 class="text-[32px] font-light text-ink mb-8">购物车</h1>

    <div v-if="cartStore.items.length === 0" class="text-center py-20">
      <div class="w-24 h-24 rounded-2xl bg-canvas-soft flex items-center justify-center mx-auto mb-4">
        <svg class="w-12 h-12 text-ink-mute/40" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"/>
        </svg>
      </div>
      <p class="text-[16px] font-light text-ink-mute">购物车是空的</p>
      <button @click="router.push('/')" class="mt-4 px-5 py-2 bg-primary text-white text-[13px] font-normal rounded-pill hover:bg-primary-deep transition-colors">去逛逛</button>
    </div>

    <div v-else class="flex flex-col lg:flex-row gap-8">
      <div class="flex-1">
        <div v-for="item in cartStore.items" :key="item.skuId" class="flex gap-4 py-5 border-b border-hairline">
          <img :src="item.image" :alt="item.productName" class="w-24 h-24 rounded-lg object-cover bg-canvas-soft flex-shrink-0">
          <div class="flex-1 min-w-0">
            <h3 class="text-[16px] font-light text-ink mb-1">{{ item.productName }}</h3>
            <p v-if="item.specInfo" class="text-[12px] font-normal text-ink-mute mb-2">{{ item.specInfo }}</p>
            <p class="text-[15px] font-normal text-ink font-tabular mb-3">¥{{ item.price }}</p>
            <div class="flex items-center gap-3">
              <button @click="updateQuantity(item.skuId, item.quantity - 1)" class="w-8 h-8 rounded-lg border border-hairline flex items-center justify-center hover:border-primary hover:text-primary transition-colors">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4"/></svg>
              </button>
              <span class="text-[15px] font-tabular text-ink w-8 text-center">{{ item.quantity }}</span>
              <button @click="updateQuantity(item.skuId, item.quantity + 1)" class="w-8 h-8 rounded-lg border border-hairline flex items-center justify-center hover:border-primary hover:text-primary transition-colors">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/></svg>
              </button>
              <button @click="removeItem(item.skuId)" class="ml-4 text-[12px] font-normal text-ink-mute hover:text-ruby transition-colors">删除</button>
            </div>
          </div>
          <div class="text-right">
            <span class="text-[17px] font-light text-ink font-tabular">¥{{ (item.price * item.quantity).toLocaleString() }}</span>
          </div>
        </div>
      </div>
      <div class="lg:w-80">
        <div class="bg-canvas-soft rounded-2xl p-6 sticky top-8">
          <h4 class="text-[15px] font-normal text-ink mb-4">订单摘要</h4>
          <div class="flex items-center justify-between mb-2">
            <span class="text-[14px] font-light text-ink-mute">商品数量</span>
            <span class="text-[14px] font-light text-ink font-tabular">{{ cartStore.totalCount }} 件</span>
          </div>
          <div class="flex items-center justify-between mb-6">
            <span class="text-[14px] font-light text-ink-mute">合计</span>
            <span class="text-[24px] font-light text-ink font-tabular">¥{{ cartStore.totalAmount.toLocaleString() }}</span>
          </div>
          <button @click="checkout" :disabled="creating"
            class="w-full py-3.5 text-[15px] font-normal rounded-pill transition-all duration-200"
            :class="creating ? 'bg-hairline text-ink-mute/50 cursor-not-allowed' : 'bg-primary text-white hover:bg-primary-deep active:bg-primary-press cursor-pointer'">
            {{ creating ? '提交中…' : '结算' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
