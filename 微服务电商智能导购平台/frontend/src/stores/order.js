import { defineStore } from 'pinia'
import { ref } from 'vue'
import { orderApi } from '../api/order'

export const useOrderStore = defineStore('order', () => {
  const orders = ref([])
  const currentOrder = ref(null)
  const loading = ref(false)

  async function fetchOrders(params = {}) {
    loading.value = true
    try {
      const data = await orderApi.getList(params)
      orders.value = data.records || []
      return data
    } catch (e) {
      console.error('获取订单列表失败:', e)
      orders.value = []
    } finally {
      loading.value = false
    }
  }

  async function fetchOrderDetail(id) {
    loading.value = true
    try {
      const data = await orderApi.getDetail(id)
      currentOrder.value = data
      return data
    } catch (e) {
      console.error('获取订单详情失败:', e)
      currentOrder.value = null
    } finally {
      loading.value = false
    }
  }

  async function createOrder(data) {
    try {
      return await orderApi.create(data)
    } catch (e) {
      console.error('创建订单失败:', e)
      throw e
    }
  }

  async function cancelOrder(id, reason) {
    try {
      await orderApi.cancel(id, { reason })
      if (currentOrder.value?.id === id) {
        currentOrder.value.status = 4
      }
    } catch (e) {
      console.error('取消订单失败:', e)
      throw e
    }
  }

  return {
    orders, currentOrder, loading,
    fetchOrders, fetchOrderDetail, createOrder, cancelOrder,
  }
})
