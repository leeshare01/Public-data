<script setup>
</script>

<template>
  <footer class="relative z-10 border-t border-hairline bg-canvas-soft">
    <div class="page-container py-16">
      <div class="grid grid-cols-2 md:grid-cols-4 gap-8">
        <div>
          <div class="flex items-center gap-2 mb-4">
            <div class="w-7 h-7 rounded-lg bg-primary flex items-center justify-center">
              <svg class="w-3.5 h-3.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"/>
              </svg>
            </div>
            <span class="text-lg font-light text-ink">商城</span>
          </div>
          <p class="text-[13px] font-normal text-ink-mute leading-relaxed">精选品质商品<br>极简购物体验<br><span class="text-[11px] text-primary/70">AI 驱动 · DeepSeek + RAG</span></p>
        </div>
        <div>
          <h4 class="text-[13px] font-normal text-ink mb-4 uppercase tracking-wider">商品</h4>
          <ul class="space-y-2.5">
            <li><span class="text-[13px] font-normal text-ink-mute">数码电子</span></li>
            <li><span class="text-[13px] font-normal text-ink-mute">生活家居</span></li>
            <li><span class="text-[13px] font-normal text-ink-mute">时尚配饰</span></li>
          </ul>
        </div>
        <div>
          <h4 class="text-[13px] font-normal text-ink mb-4 uppercase tracking-wider">服务</h4>
          <ul class="space-y-2.5">
            <li><span class="text-[13px] font-normal text-ink-mute">配送说明</span></li>
            <li><span class="text-[13px] font-normal text-ink-mute">退换政策</span></li>
            <li><span class="text-[13px] font-normal text-ink-mute">联系客服</span></li>
          </ul>
        </div>
        <div>
          <h4 class="text-[13px] font-normal text-ink mb-4 uppercase tracking-wider">关注我们</h4>
          <div class="flex items-center gap-3">
            <span class="w-9 h-9 rounded-lg bg-white border border-hairline flex items-center justify-center hover:border-primary hover:bg-primary-bg-subdued transition-colors duration-200 cursor-pointer">
              <svg class="w-4 h-4 text-ink-mute" fill="currentColor" viewBox="0 0 24 24"><path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/></svg>
            </span>
            <span class="w-9 h-9 rounded-lg bg-white border border-hairline flex items-center justify-center hover:border-primary hover:bg-primary-bg-subdued transition-colors duration-200 cursor-pointer">
              <svg class="w-4 h-4 text-ink-mute" fill="currentColor" viewBox="0 0 24 24"><path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838c-3.403 0-6.162 2.759-6.162 6.162s2.759 6.163 6.162 6.163 6.162-2.759 6.162-6.163c0-3.403-2.759-6.162-6.162-6.162zm0 10.162c-2.209 0-4-1.79-4-4 0-2.209 1.791-4 4-4s4 1.791 4 4c0 2.21-1.791 4-4 4zm6.406-11.845c-.796 0-1.441.645-1.441 1.44s.645 1.44 1.441 1.44c.795 0 1.439-.645 1.439-1.44s-.644-1.44-1.439-1.44z"/></svg>
            </span>
          </div>
        </div>
      </div>
      <div class="mt-12 pt-8 border-t border-hairline flex flex-col md:flex-row items-center justify-between gap-4">
        <p class="text-[11px] font-light text-ink-mute">2024 商城. 保留所有权利.</p>
        <div class="flex items-center gap-6">
          <span class="text-[11px] font-light text-ink-mute hover:text-primary transition-colors cursor-pointer">隐私政策</span>
          <span class="text-[11px] font-light text-ink-mute hover:text-primary transition-colors cursor-pointer">服务条款</span>
        </div>
      </div>
    </div>
  </footer>
</template>
