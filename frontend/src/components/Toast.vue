<script setup>
import { useToastStore } from '../stores/toast'

const toast = useToastStore()
</script>

<template>
  <Teleport to="body">
    <div
      v-show="toast.visible"
      class="fixed bottom-8 left-1/2 -translate-x-1/2 z-[200] px-6 py-3.5 bg-ink text-white text-[14px] font-normal rounded-xl shadow-float translate-y-20 opacity-0 transition-all duration-300 flex items-center gap-3 pointer-events-none"
      :class="{ 'toast-show': toast.visible }"
    >
      <svg class="w-5 h-5 text-primary-soft flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
      </svg>
      <span>{{ toast.message }}</span>
    </div>
  </Teleport>
</template>
