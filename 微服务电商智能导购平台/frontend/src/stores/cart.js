import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { cartApi } from '../api/cart'

export const useCartStore = defineStore('cart', () => {
  const items = ref([])
  const loading = ref(false)

  const totalCount = computed(() => items.value.reduce((s, i) => s + i.quantity, 0))
  const totalAmount = computed(() => items.value.reduce((s, i) => s + i.price * i.quantity, 0))

  async function fetchItems() {
    loading.value = true
    try {
      const data = await cartApi.getItems()
      items.value = data.items || []
    } finally {
      loading.value = false
    }
  }

  async function addItem(skuId, quantity = 1) {
    await cartApi.addItem({ skuId, quantity })
    // 加购后局部更新数量，不刷新整个购物车
    // 查找本地是否已有该 SKU，有则增量，无则追加占位
    const existing = items.value.find(i => i.skuId === skuId || i.productId === skuId)
    if (existing) {
      existing.quantity += quantity
    } else {
      items.value.push({ skuId, productId: skuId, quantity, selected: true })
    }
  }

  async function updateQuantity(skuId, quantity) {
    await cartApi.updateItem(skuId, { quantity })
    await fetchItems()
  }

  async function removeItem(skuId) {
    await cartApi.removeItem(skuId)
    await fetchItems()
  }

  async function selectItem(skuId, selected) {
    await cartApi.selectItem(skuId, { selected })
    await fetchItems()
  }

  async function selectAll(selected) {
    await cartApi.selectAll({ selected })
    await fetchItems()
  }

  async function clearAll() {
    await cartApi.clear()
    items.value = []
  }

  return {
    items, loading, totalCount, totalAmount,
    fetchItems, addItem, updateQuantity, removeItem,
    selectItem, selectAll, clearAll,
  }
})
