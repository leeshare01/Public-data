import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import './style.css'

async function bootstrap() {
  // 是否启用 MSW Mock（默认关闭，直连真实后端；加 ?mock=1 开启）
  const useMock = import.meta.env.DEV && (
    new URLSearchParams(window.location.search).get('mock') === '1' ||
    localStorage.getItem('eshop_use_mock') === 'true'
  )

  if (useMock) {
    try {
      // 先把 Mock 数据暴露到全局，供组件 fallback 使用
      const { mockProducts } = await import('./mock/products')
      window.__mockProducts = mockProducts

      const { worker } = await import('./mock/browser')
      await worker.start({
        onUnhandledRequest: 'bypass',
        quiet: false,
      })
      console.log('%c[MSW] Mock API 已启动 ✅', 'color: #533afd; font-weight: bold')
      console.log('%c[MSW] 请求将被拦截，无需后端', 'color: #64748d')
    } catch (e) {
      console.warn('[MSW] Mock API 启动失败:', e.message)
    }
  } else {
    console.log('%c[API] 直连真实后端 → http://localhost:8086', 'color: #22c55e; font-weight: bold')
  }

  const app = createApp(App)
  app.use(createPinia())
  app.use(router)
  app.mount('#app')
}

bootstrap()
