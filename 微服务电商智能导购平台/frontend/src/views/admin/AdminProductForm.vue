<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { productApi } from '../../api/product'
import { useToastStore } from '../../stores/toast'

const route = useRoute()
const router = useRouter()
const toast = useToastStore()

const isEdit = computed(() => !!route.params.id)
const formTitle = computed(() => (isEdit.value ? '编辑商品' : '新增商品'))
const pageTitle = computed(() => (isEdit.value ? '编辑商品' : '新增商品'))

// ============ 表单数据 ============
const form = ref({
  name: '',
  subtitle: '',
  brand: '',
  description: '',
  mainImage: '',
  subImageList: [],
  categoryId: null,
  price: null,
  originalPrice: null,
  status: 1,
  keywords: '',
  skuList: [],
  attributes: [],
})

const categories = ref([])
const subCategories = ref([])
const selectedLevel1 = ref(null)
const loading = ref(false)
const saving = ref(false)

// ============ 加载分类树 ============
async function loadCategories() {
  try {
    const tree = await productApi.getCategoryTree()
    categories.value = tree || []
  } catch {
    categories.value = []
  }
}

// 一级分类变化 → 加载二级分类
watch(selectedLevel1, (val) => {
  subCategories.value = []
  if (isEdit.value && form.value.categoryId) return // 编辑模式保留已选
  form.value.categoryId = null
  if (val) {
    const parent = categories.value.find(c => c.id === val)
    subCategories.value = parent?.children || []
  }
})

// ============ 加载商品数据（编辑模式） ============
async function loadProduct() {
  if (!isEdit.value) return
  loading.value = true
  try {
    const data = await productApi.getAdminProductDetail(route.params.id)
    form.value.name = data.name || ''
    form.value.subtitle = data.subtitle || ''
    form.value.brand = data.brand || ''
    form.value.description = data.description || ''
    form.value.mainImage = data.mainImage || ''
    form.value.subImageList = parseSubImages(data.subImages)
    form.value.categoryId = data.categoryId
    form.value.price = data.price
    form.value.originalPrice = data.originalPrice
    form.value.status = data.status ?? 1
    form.value.keywords = data.keywords || ''
    form.value.skuList = (data.skuList || []).map(s => ({
      id: s.id,
      specValues: s.specValues && typeof s.specValues !== 'string'
        ? JSON.stringify(s.specValues)
        : (s.specValues || '{}'),
      price: s.price,
      stock: s.stock ?? 0,
      image: s.image || '',
      sortOrder: s.sortOrder ?? 0,
    }))
    form.value.attributes = (data.attributes || []).map(a => ({
      id: a.id,
      attrName: a.attrName,
      attrValue: a.attrValue,
      sortOrder: a.sortOrder ?? 0,
    }))

    // 恢复一级分类选择
    if (data.categoryId) {
      for (const cat of categories.value) {
        if (cat.children?.some(c => c.id === data.categoryId)) {
          selectedLevel1.value = cat.id
          break
        }
      }
    }
  } catch (e) {
    toast.show('加载商品失败')
    router.push('/admin/products')
  } finally {
    loading.value = false
  }
}

function parseSubImages(val) {
  if (!val) return []
  if (Array.isArray(val)) return val
  if (typeof val === 'string') {
    try { return JSON.parse(val) } catch { return [val] }
  }
  return []
}

// ============ 副图管理 ============
function addSubImage() { form.value.subImageList.push('') }
function removeSubImage(idx) { form.value.subImageList.splice(idx, 1) }

// ============ SKU 管理 ============
function addSku() {
  form.value.skuList.push({
    specValues: '{}',
    price: form.value.price,
    stock: 0,
    image: '',
    sortOrder: form.value.skuList.length,
  })
}
function removeSku(idx) { form.value.skuList.splice(idx, 1) }

// ============ 参数管理 ============
function addAttribute() {
  form.value.attributes.push({ attrName: '', attrValue: '', sortOrder: form.value.attributes.length })
}
function removeAttribute(idx) { form.value.attributes.splice(idx, 1) }

// ============ 保存 ============
async function save() {
  if (!form.value.name.trim()) { toast.show('请输入商品名称'); return }
  if (!form.value.categoryId) { toast.show('请选择分类'); return }
  if (form.value.price == null || form.value.price < 0) { toast.show('请输入有效的价格'); return }

  saving.value = true
  try {
    const payload = {
      name: form.value.name.trim(),
      subtitle: form.value.subtitle.trim(),
      brand: form.value.brand.trim(),
      description: form.value.description.trim(),
      mainImage: form.value.mainImage.trim(),
      subImages: form.value.subImageList.filter(s => s.trim()).length > 0
        ? JSON.stringify(form.value.subImageList.filter(s => s.trim()))
        : null,
      categoryId: form.value.categoryId,
      price: Number(form.value.price),
      originalPrice: form.value.originalPrice ? Number(form.value.originalPrice) : null,
      status: form.value.status,
      keywords: form.value.keywords.trim(),
      skuList: form.value.skuList
        .filter(s => s.price != null)
        .map(s => ({
          id: s.id || undefined,
          specValues: s.specValues,
          price: Number(s.price),
          stock: s.stock ?? 0,
          image: s.image || '',
          sortOrder: s.sortOrder ?? 0,
        })),
      attributes: form.value.attributes
        .filter(a => a.attrName.trim())
        .map(a => ({
          id: a.id || undefined,
          attrName: a.attrName.trim(),
          attrValue: a.attrValue.trim(),
          sortOrder: a.sortOrder ?? 0,
        })),
    }

    if (isEdit.value) {
      await productApi.updateProduct(route.params.id, payload)
      toast.show('商品更新成功')
    } else {
      payload.sales = 0
      await productApi.createProduct(payload)
      toast.show('商品创建成功')
    }
    router.push('/admin/products')
  } catch (e) {
    toast.show('保存失败: ' + (e.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadCategories()
  await loadProduct()
})
</script>

<template>
  <div class="page-container pt-8 pb-24 max-w-4xl mx-auto">
    <!-- 顶部导航 -->
    <div class="flex items-center justify-between mb-8">
      <div>
        <router-link to="/admin/products" class="text-[13px] text-ink-mute hover:text-primary transition-colors">← 返回商品管理</router-link>
        <h1 class="text-[32px] font-light text-ink mt-1">{{ pageTitle }}</h1>
      </div>
      <button @click="save" :disabled="saving"
        class="px-6 py-2.5 bg-primary text-white text-[14px] font-normal rounded-pill hover:bg-primary-deep active:bg-primary-press transition-colors disabled:opacity-40 disabled:cursor-not-allowed">
        {{ saving ? '保存中...' : '保存' }}
      </button>
    </div>

    <div v-if="loading" class="text-center py-20 text-ink-mute">加载中...</div>

    <form v-else @submit.prevent="save" class="space-y-8">
      <!-- ===== 基本信息 ===== -->
      <section class="bg-white rounded-2xl p-6 shadow-card">
        <h2 class="text-[18px] font-light text-ink mb-5">基本信息</h2>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
          <div class="md:col-span-2">
            <label class="block text-[13px] font-normal text-ink-mute mb-1.5">商品名称 <span class="text-ruby">*</span></label>
            <input v-model="form.name" type="text" placeholder="输入商品名称"
              class="w-full px-4 py-2.5 border border-hairline rounded-lg text-[14px] text-ink placeholder-ink-mute/50 focus:outline-none focus:border-primary transition-colors">
          </div>
          <div>
            <label class="block text-[13px] font-normal text-ink-mute mb-1.5">副标题</label>
            <input v-model="form.subtitle" type="text" placeholder="商品简短描述"
              class="w-full px-4 py-2.5 border border-hairline rounded-lg text-[14px] text-ink placeholder-ink-mute/50 focus:outline-none focus:border-primary transition-colors">
          </div>
          <div>
            <label class="block text-[13px] font-normal text-ink-mute mb-1.5">品牌</label>
            <input v-model="form.brand" type="text" placeholder="品牌名称"
              class="w-full px-4 py-2.5 border border-hairline rounded-lg text-[14px] text-ink placeholder-ink-mute/50 focus:outline-none focus:border-primary transition-colors">
          </div>
          <div>
            <label class="block text-[13px] font-normal text-ink-mute mb-1.5">一级分类</label>
            <select v-model="selectedLevel1"
              class="w-full px-4 py-2.5 border border-hairline rounded-lg text-[14px] text-ink focus:outline-none focus:border-primary transition-colors bg-white">
              <option :value="null">请选择一级分类</option>
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
            </select>
          </div>
          <div>
            <label class="block text-[13px] font-normal text-ink-mute mb-1.5">二级分类 <span class="text-ruby">*</span></label>
            <select v-model="form.categoryId"
              class="w-full px-4 py-2.5 border border-hairline rounded-lg text-[14px] text-ink focus:outline-none focus:border-primary transition-colors bg-white">
              <option :value="null">请选择二级分类</option>
              <option v-for="cat in subCategories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
            </select>
          </div>
          <div class="md:col-span-2">
            <label class="block text-[13px] font-normal text-ink-mute mb-1.5">关键词（逗号分隔，用于搜索）</label>
            <input v-model="form.keywords" type="text" placeholder="例如: 舒适, 简约, 北欧风"
              class="w-full px-4 py-2.5 border border-hairline rounded-lg text-[14px] text-ink placeholder-ink-mute/50 focus:outline-none focus:border-primary transition-colors">
          </div>
        </div>
      </section>

      <!-- ===== 价格与状态 ===== -->
      <section class="bg-white rounded-2xl p-6 shadow-card">
        <h2 class="text-[18px] font-light text-ink mb-5">价格与状态</h2>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-5">
          <div>
            <label class="block text-[13px] font-normal text-ink-mute mb-1.5">价格 <span class="text-ruby">*</span></label>
            <div class="relative">
              <span class="absolute left-3 top-1/2 -translate-y-1/2 text-[14px] text-ink-mute">¥</span>
              <input v-model.number="form.price" type="number" step="0.01" min="0" placeholder="0.00"
                class="w-full pl-8 pr-4 py-2.5 border border-hairline rounded-lg text-[14px] text-ink placeholder-ink-mute/50 focus:outline-none focus:border-primary transition-colors font-tabular">
            </div>
          </div>
          <div>
            <label class="block text-[13px] font-normal text-ink-mute mb-1.5">原价</label>
            <div class="relative">
              <span class="absolute left-3 top-1/2 -translate-y-1/2 text-[14px] text-ink-mute">¥</span>
              <input v-model.number="form.originalPrice" type="number" step="0.01" min="0" placeholder="0.00"
                class="w-full pl-8 pr-4 py-2.5 border border-hairline rounded-lg text-[14px] text-ink placeholder-ink-mute/50 focus:outline-none focus:border-primary transition-colors font-tabular">
            </div>
          </div>
          <div>
            <label class="block text-[13px] font-normal text-ink-mute mb-1.5">状态</label>
            <div class="flex items-center gap-3 pt-2">
              <label class="flex items-center gap-2 cursor-pointer">
                <input type="radio" v-model="form.status" :value="1" class="text-primary focus:ring-primary">
                <span class="text-[14px] text-ink">上架</span>
              </label>
              <label class="flex items-center gap-2 cursor-pointer">
                <input type="radio" v-model="form.status" :value="0" class="text-ink-mute focus:ring-ink-mute">
                <span class="text-[14px] text-ink">下架</span>
              </label>
            </div>
          </div>
        </div>
      </section>

      <!-- ===== 图片 ===== -->
      <section class="bg-white rounded-2xl p-6 shadow-card">
        <h2 class="text-[18px] font-light text-ink mb-5">图片</h2>
        <div class="space-y-4">
          <div>
            <label class="block text-[13px] font-normal text-ink-mute mb-1.5">主图 URL</label>
            <div class="flex gap-3">
              <input v-model="form.mainImage" type="text" placeholder="/images/products/xxx.jpg"
                class="flex-1 px-4 py-2.5 border border-hairline rounded-lg text-[14px] text-ink placeholder-ink-mute/50 focus:outline-none focus:border-primary transition-colors">
              <div v-if="form.mainImage" class="w-12 h-12 rounded-lg overflow-hidden bg-canvas-soft flex-shrink-0 border border-hairline">
                <img :src="form.mainImage" class="w-full h-full object-cover" @error="$event.target.style.display='none'" @load="$event.target.style.display='block'">
              </div>
            </div>
          </div>
          <div>
            <div class="flex items-center justify-between mb-1.5">
              <label class="text-[13px] font-normal text-ink-mute">副图 URL</label>
              <button type="button" @click="addSubImage" class="text-[12px] text-primary hover:text-primary-deep transition-colors">+ 添加副图</button>
            </div>
            <div v-for="(_, idx) in form.subImageList" :key="idx" class="flex gap-2 mb-2">
              <input v-model="form.subImageList[idx]" type="text" placeholder="/images/products/xxx.jpg"
                class="flex-1 px-4 py-2 border border-hairline rounded-lg text-[14px] text-ink placeholder-ink-mute/50 focus:outline-none focus:border-primary transition-colors">
              <div v-if="form.subImageList[idx]" class="w-10 h-10 rounded-lg overflow-hidden bg-canvas-soft flex-shrink-0 border border-hairline">
                <img :src="form.subImageList[idx]" class="w-full h-full object-cover" @error="$event.target.style.display='none'">
              </div>
              <button type="button" @click="removeSubImage(idx)" class="text-ink-mute/50 hover:text-ruby transition-colors self-center">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
              </button>
            </div>
            <p v-if="form.subImageList.length === 0" class="text-[12px] text-ink-mute/50">暂无副图，点击上方按钮添加</p>
          </div>
        </div>
      </section>

      <!-- ===== 描述 ===== -->
      <section class="bg-white rounded-2xl p-6 shadow-card">
        <h2 class="text-[18px] font-light text-ink mb-5">商品描述</h2>
        <textarea v-model="form.description" rows="6" placeholder="输入商品描述（支持 HTML）"
          class="w-full px-4 py-3 border border-hairline rounded-lg text-[14px] text-ink placeholder-ink-mute/50 focus:outline-none focus:border-primary transition-colors resize-y"></textarea>
      </section>

      <!-- ===== SKU ===== -->
      <section class="bg-white rounded-2xl p-6 shadow-card">
        <div class="flex items-center justify-between mb-5">
          <h2 class="text-[18px] font-light text-ink">SKU（规格）</h2>
          <button type="button" @click="addSku" class="text-[12px] text-primary hover:text-primary-deep transition-colors">+ 添加 SKU</button>
        </div>
        <div v-if="form.skuList.length === 0" class="text-[13px] text-ink-mute/50 text-center py-4">暂无 SKU（非必填，简单商品可不设置）</div>
        <div v-for="(sku, idx) in form.skuList" :key="idx" class="border border-hairline rounded-lg p-4 mb-3">
          <div class="flex items-center justify-between mb-3">
            <span class="text-[13px] font-normal text-ink-mute">SKU #{{ idx + 1 }}</span>
            <button type="button" @click="removeSku(idx)" class="text-[12px] text-ruby/60 hover:text-ruby transition-colors">删除</button>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-4 gap-3">
            <div class="md:col-span-4">
              <label class="block text-[11px] text-ink-mute mb-1">规格（JSON 格式）</label>
              <input v-model="sku.specValues" type="text" placeholder='{"颜色":"红色","尺寸":"M"}'
                class="w-full px-3 py-2 border border-hairline rounded-lg text-[13px] text-ink placeholder-ink-mute/50 focus:outline-none focus:border-primary transition-colors font-mono">
            </div>
            <div>
              <label class="block text-[11px] text-ink-mute mb-1">价格</label>
              <input v-model.number="sku.price" type="number" step="0.01" min="0" placeholder="0.00"
                class="w-full px-3 py-2 border border-hairline rounded-lg text-[13px] text-ink placeholder-ink-mute/50 focus:outline-none focus:border-primary transition-colors font-tabular">
            </div>
            <div>
              <label class="block text-[11px] text-ink-mute mb-1">库存</label>
              <input v-model.number="sku.stock" type="number" min="0" placeholder="0"
                class="w-full px-3 py-2 border border-hairline rounded-lg text-[13px] text-ink placeholder-ink-mute/50 focus:outline-none focus:border-primary transition-colors font-tabular">
            </div>
            <div class="md:col-span-2">
              <label class="block text-[11px] text-ink-mute mb-1">SKU 图片 URL（可选）</label>
              <input v-model="sku.image" type="text" placeholder="/images/products/xxx.jpg"
                class="w-full px-3 py-2 border border-hairline rounded-lg text-[13px] text-ink placeholder-ink-mute/50 focus:outline-none focus:border-primary transition-colors">
            </div>
          </div>
        </div>
      </section>

      <!-- ===== 参数 ===== -->
      <section class="bg-white rounded-2xl p-6 shadow-card">
        <div class="flex items-center justify-between mb-5">
          <h2 class="text-[18px] font-light text-ink">商品参数</h2>
          <button type="button" @click="addAttribute" class="text-[12px] text-primary hover:text-primary-deep transition-colors">+ 添加参数</button>
        </div>
        <div v-if="form.attributes.length === 0" class="text-[13px] text-ink-mute/50 text-center py-4">暂无参数（非必填）</div>
        <div v-for="(attr, idx) in form.attributes" :key="idx" class="flex gap-3 items-center mb-2">
          <input v-model="attr.attrName" type="text" placeholder="参数名（如：材质）"
            class="flex-1 px-3 py-2 border border-hairline rounded-lg text-[13px] text-ink placeholder-ink-mute/50 focus:outline-none focus:border-primary transition-colors">
          <input v-model="attr.attrValue" type="text" placeholder="参数值（如：纯棉）"
            class="flex-1 px-3 py-2 border border-hairline rounded-lg text-[13px] text-ink placeholder-ink-mute/50 focus:outline-none focus:border-primary transition-colors">
          <button type="button" @click="removeAttribute(idx)" class="text-ink-mute/50 hover:text-ruby transition-colors flex-shrink-0">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
          </button>
        </div>
      </section>

      <!-- 底部栏 -->
      <div class="flex items-center justify-between pt-4 border-t border-hairline">
        <router-link to="/admin/products" class="text-[14px] text-ink-mute hover:text-primary transition-colors">取消</router-link>
        <button type="submit" :disabled="saving"
          class="px-8 py-2.5 bg-primary text-white text-[14px] font-normal rounded-pill hover:bg-primary-deep active:bg-primary-press transition-colors disabled:opacity-40 disabled:cursor-not-allowed">
          {{ saving ? '保存中...' : (isEdit ? '更新商品' : '创建商品') }}
        </button>
      </div>
    </form>
  </div>
</template>
