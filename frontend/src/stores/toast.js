import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useToastStore = defineStore('toast', () => {
  const message = ref('')
  const visible = ref(false)
  let timer = null

  function show(msg) {
    message.value = msg
    visible.value = true
    clearTimeout(timer)
    timer = setTimeout(() => { visible.value = false }, 2500)
  }

  return { message, visible, show }
})
