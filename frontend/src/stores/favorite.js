import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const STORAGE_KEY = 'eshop_favorites'

export const useFavoriteStore = defineStore('favorite', () => {
  // 从 localStorage 加载收藏 ID 列表
  function loadIds() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      return raw ? JSON.parse(raw) : []
    } catch {
      return []
    }
  }

  const ids = ref(loadIds())

  // 持久化到 localStorage
  function persist() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(ids.value))
  }

  const count = computed(() => ids.value.length)
  const isEmpty = computed(() => ids.value.length === 0)

  function isFavorited(productId) {
    return ids.value.includes(productId)
  }

  function add(productId) {
    if (!ids.value.includes(productId)) {
      ids.value.push(productId)
      persist()
    }
  }

  function remove(productId) {
    const idx = ids.value.indexOf(productId)
    if (idx !== -1) {
      ids.value.splice(idx, 1)
      persist()
    }
  }

  function toggle(productId) {
    if (ids.value.includes(productId)) {
      remove(productId)
      return false
    } else {
      add(productId)
      return true
    }
  }

  function getIds() {
    return [...ids.value]
  }

  return { ids, count, isEmpty, isFavorited, add, remove, toggle, getIds }
})
