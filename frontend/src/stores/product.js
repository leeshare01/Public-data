import { defineStore } from 'pinia'
import { ref } from 'vue'
import { productApi } from '../api/product'

export const useProductStore = defineStore('product', () => {
  const products = ref([])
  const total = ref(0)
  const page = ref(1)
  const pageSize = ref(12)
  const loading = ref(false)

  async function fetchProducts(params = {}) {
    loading.value = true
    try {
      const data = await productApi.getProducts({
        page: page.value,
        size: pageSize.value,
        ...params,
      })
      products.value = data.records || []
      total.value = data.total || 0
      page.value = data.page || 1
    } catch (e) {
      console.error('获取商品列表失败:', e)
      products.value = []
    } finally {
      loading.value = false
    }
  }

  async function fetchProductDetail(id) {
    try {
      return await productApi.getProductDetail(id)
    } catch (e) {
      console.error('获取商品详情失败:', e)
      return null
    }
  }

  return { products, total, page, pageSize, loading, fetchProducts, fetchProductDetail }
})
