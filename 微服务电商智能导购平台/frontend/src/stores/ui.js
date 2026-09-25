import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUIStore = defineStore('ui', () => {
  const showAuthModal = ref(false)
  const authTab = ref('login')
  const showCart = ref(false)
  const showDetailModal = ref(false)
  const detailProduct = ref(null)

  function openAuth(tab = 'login') {
    authTab.value = tab
    showAuthModal.value = true
  }

  function closeAuth() {
    showAuthModal.value = false
  }

  function openCart() {
    showCart.value = true
  }

  function closeCart() {
    showCart.value = false
  }

  function openDetail(product) {
    detailProduct.value = product
    showDetailModal.value = true
  }

  function closeDetail() {
    showDetailModal.value = false
    detailProduct.value = null
  }

  return {
    showAuthModal, authTab, showCart, showDetailModal, detailProduct,
    openAuth, closeAuth, openCart, closeCart, openDetail, closeDetail,
  }
})
