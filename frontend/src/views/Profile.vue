<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useToastStore } from '../stores/toast'
import { userApi } from '../api/user'

const router = useRouter()
const userStore = useUserStore()
const toast = useToastStore()
const profile = ref(userStore.user || {})
const editing = ref(false)
const editForm = ref({ nickname: '', phone: '', email: '' })

// ============ 地址管理 ============
const addresses = ref([])
const addressLoading = ref(false)
const showAddressForm = ref(false)
const addressForm = ref({
  id: null,
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: 0,
})
const deletingId = ref(null)

onMounted(async () => {
  try {
    const data = await userStore.fetchProfile()
    if (data) Object.assign(profile.value, data)
    editForm.value = {
      nickname: profile.value.nickname || '',
      phone: profile.value.phone || '',
      email: profile.value.email || '',
    }
  } catch {}
  await loadAddresses()
})

function startEdit() {
  editForm.value = {
    nickname: profile.value.nickname || '',
    phone: profile.value.phone || '',
    email: profile.value.email || '',
  }
  editing.value = true
}

async function saveProfile() {
  try {
    await userApi.updateProfile(editForm.value)
    profile.value.nickname = editForm.value.nickname
    profile.value.phone = editForm.value.phone
    profile.value.email = editForm.value.email
    userStore.user = { ...userStore.user, ...editForm.value }
    localStorage.setItem('eshop_user', JSON.stringify(userStore.user))
    editing.value = false
    toast.show('信息已更新')
  } catch (e) {
    toast.show('更新失败: ' + (e.message || '未知错误'))
  }
}

// ============ 地址函数 ============
async function loadAddresses() {
  addressLoading.value = true
  try {
    addresses.value = await userApi.getAddresses() || []
  } catch {
    addresses.value = []
  } finally {
    addressLoading.value = false
  }
}

function openAddAddress() {
  addressForm.value = { id: null, receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', isDefault: 0 }
  showAddressForm.value = true
}

function openEditAddress(addr) {
  addressForm.value = { ...addr }
  showAddressForm.value = true
}

async function saveAddress() {
  const form = addressForm.value
  if (!form.receiverName || !form.receiverPhone || !form.detailAddress) {
    toast.show('请填写完整的地址信息')
    return
  }
  try {
    if (form.id) {
      await userApi.updateAddress(form.id, form)
      toast.show('地址已更新')
    } else {
      await userApi.addAddress(form)
      toast.show('地址已添加')
    }
    showAddressForm.value = false
    await loadAddresses()
  } catch (e) {
    toast.show('操作失败: ' + (e.message || '未知错误'))
  }
}

async function confirmDelete(id) {
  deletingId.value = id
}

async function doDelete(id) {
  try {
    await userApi.deleteAddress(id)
    toast.show('地址已删除')
    deletingId.value = null
    await loadAddresses()
  } catch (e) {
    toast.show('删除失败: ' + (e.message || '未知错误'))
  }
}

async function setDefault(id) {
  try {
    await userApi.setDefaultAddress(id)
    toast.show('已设为默认地址')
    await loadAddresses()
  } catch (e) {
    toast.show('操作失败: ' + (e.message || '未知错误'))
  }
}

function cancelAddressForm() {
  showAddressForm.value = false
}

// 省市区辅助（演示用，生产环境可接地区 API）
const presetDistricts = [
  { province: '广东省', cities: ['广州市', '深圳市', '珠海市', '佛山市', '东莞市'] },
  { province: '北京市', cities: ['朝阳区', '海淀区', '丰台区', '东城区', '西城区'] },
  { province: '上海市', cities: ['浦东新区', '黄浦区', '徐汇区', '静安区', '长宁区'] },
  { province: '浙江省', cities: ['杭州市', '宁波市', '温州市', '嘉兴市', '绍兴市'] },
  { province: '江苏省', cities: ['南京市', '苏州市', '无锡市', '常州市', '南通市'] },
]

const filteredCities = ref([])

function onProvinceChange() {
  const p = presetDistricts.find(d => d.province === addressForm.value.province)
  filteredCities.value = p ? p.cities : []
  addressForm.value.city = ''
  addressForm.value.district = ''
}
</script>

<template>
  <div class="page-container pt-8 pb-24">
    <h1 class="text-[32px] font-light text-ink mb-8">个人中心</h1>

    <div class="max-w-2xl">
      <!-- ========== 个人信息卡片 ========== -->
      <div class="bg-white rounded-2xl p-8 shadow-card">
        <div class="flex items-center gap-4 mb-8">
          <div class="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center">
            <span class="text-2xl font-light text-primary">{{ (profile.nickname || profile.username || '?')[0] }}</span>
          </div>
          <div>
            <h2 class="text-[18px] font-normal text-ink">{{ profile.nickname || profile.username }}</h2>
            <p class="text-[13px] font-normal text-ink-mute">@{{ profile.username }}</p>
          </div>
          <div class="ml-auto">
            <span class="px-3 py-1 text-[12px] font-normal rounded-pill" :class="profile.role === 'ADMIN' ? 'bg-amber/10 text-amber' : 'bg-primary-bg-subdued text-primary'">
              {{ profile.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </span>
          </div>
        </div>

        <!-- 详情模式 -->
        <div v-if="!editing" class="space-y-4">
          <div class="flex justify-between py-3 border-b border-hairline">
            <span class="text-[14px] text-ink-mute">邮箱</span>
            <span class="text-[14px] text-ink">{{ profile.email || '未设置' }}</span>
          </div>
          <div class="flex justify-between py-3 border-b border-hairline">
            <span class="text-[14px] text-ink-mute">手机号</span>
            <span class="text-[14px] text-ink">{{ profile.phone || '未设置' }}</span>
          </div>
          <div class="flex justify-between py-3 border-b border-hairline">
            <span class="text-[14px] text-ink-mute">注册时间</span>
            <span class="text-[14px] text-ink">{{ profile.createTime || '-' }}</span>
          </div>
          <button @click="startEdit" class="mt-6 px-6 py-2.5 bg-primary text-white text-[14px] font-normal rounded-pill hover:bg-primary-deep transition-colors">编辑信息</button>
        </div>

        <!-- 编辑模式 -->
        <div v-else class="space-y-4">
          <div>
            <label class="text-[13px] font-normal text-ink-mute block mb-1">昵称</label>
            <input v-model="editForm.nickname" class="w-full px-4 py-2.5 border border-hairline-input rounded-lg text-[14px] focus:outline-none focus:border-primary">
          </div>
          <div>
            <label class="text-[13px] font-normal text-ink-mute block mb-1">手机号</label>
            <input v-model="editForm.phone" class="w-full px-4 py-2.5 border border-hairline-input rounded-lg text-[14px] focus:outline-none focus:border-primary">
          </div>
          <div>
            <label class="text-[13px] font-normal text-ink-mute block mb-1">邮箱</label>
            <input v-model="editForm.email" class="w-full px-4 py-2.5 border border-hairline-input rounded-lg text-[14px] focus:outline-none focus:border-primary">
          </div>
          <div class="flex gap-3 mt-6">
            <button @click="saveProfile" class="px-6 py-2.5 bg-primary text-white text-[14px] font-normal rounded-pill hover:bg-primary-deep transition-colors">保存</button>
            <button @click="editing = false" class="px-6 py-2.5 border border-hairline text-ink text-[14px] font-normal rounded-pill hover:border-primary transition-colors">取消</button>
          </div>
        </div>
      </div>

      <!-- ========== 收货地址 ========== -->
      <div class="bg-white rounded-2xl p-8 shadow-card mt-6">
        <div class="flex items-center justify-between mb-6">
          <h3 class="text-[18px] font-normal text-ink">收货地址</h3>
          <button @click="openAddAddress" class="px-4 py-2 bg-primary text-white text-[13px] font-normal rounded-pill hover:bg-primary-deep transition-colors">+ 添加新地址</button>
        </div>

        <div v-if="addressLoading" class="text-center py-8 text-ink-mute text-[14px]">加载中...</div>

        <div v-else-if="addresses.length === 0" class="text-center py-8">
          <p class="text-[14px] font-light text-ink-mute">暂无收货地址</p>
          <p class="text-[12px] font-normal text-ink-mute/60 mt-1">添加地址以方便下单</p>
        </div>

        <div v-else class="space-y-3">
          <div v-for="addr in addresses" :key="addr.id"
            class="flex items-start justify-between p-4 rounded-xl border border-hairline hover:border-primary/30 transition-colors group">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <span class="text-[15px] font-normal text-ink">{{ addr.receiverName }}</span>
                <span class="text-[13px] font-normal text-ink-mute">{{ addr.receiverPhone }}</span>
                <span v-if="addr.isDefault" class="px-2 py-0.5 bg-primary-bg-subdued text-primary text-[10px] rounded-pill font-normal">默认</span>
              </div>
              <p class="text-[13px] font-light text-ink-mute truncate">
                {{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detailAddress }}
              </p>
            </div>
            <div class="flex items-center gap-2 ml-4 opacity-0 group-hover:opacity-100 transition-opacity flex-shrink-0">
              <button v-if="!addr.isDefault" @click="setDefault(addr.id)"
                class="text-[12px] text-ink-mute hover:text-primary transition-colors">设为默认</button>
              <button @click="openEditAddress(addr)"
                class="text-[12px] text-ink-mute hover:text-primary transition-colors">编辑</button>
              <template v-if="deletingId === addr.id">
                <span class="text-[12px] text-ink-mute/60">确认删除？</span>
                <button @click="doDelete(addr.id)" class="text-[12px] text-ruby hover:text-ruby/80 transition-colors">确认</button>
                <button @click="deletingId = null" class="text-[12px] text-ink-mute hover:text-primary transition-colors">取消</button>
              </template>
              <button v-else @click="confirmDelete(addr.id)"
                class="text-[12px] text-ink-mute hover:text-ruby transition-colors">删除</button>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== 快捷入口 ========== -->
      <div class="grid grid-cols-3 gap-4 mt-6">
        <router-link to="/orders" class="bg-white rounded-xl p-5 shadow-card hover:shadow-card-hover transition-shadow">
          <p class="text-[16px] font-normal text-ink">我的订单</p>
          <p class="text-[12px] font-normal text-ink-mute mt-1">查看所有订单</p>
        </router-link>
        <router-link to="/favorites" class="bg-white rounded-xl p-5 shadow-card hover:shadow-card-hover transition-shadow">
          <p class="text-[16px] font-normal text-ink">我的收藏</p>
          <p class="text-[12px] font-normal text-ink-mute mt-1">查看收藏商品</p>
        </router-link>
        <router-link to="/cart" class="bg-white rounded-xl p-5 shadow-card hover:shadow-card-hover transition-shadow">
          <p class="text-[16px] font-normal text-ink">购物车</p>
          <p class="text-[12px] font-normal text-ink-mute mt-1">管理购物车商品</p>
        </router-link>
      </div>

      <div v-if="userStore.isAdmin" class="mt-6">
        <router-link to="/admin" class="block bg-white rounded-xl p-5 shadow-card hover:shadow-card-hover transition-shadow border-l-4 border-amber">
          <p class="text-[16px] font-normal text-ink">⚙️ 管理后台</p>
          <p class="text-[12px] font-normal text-ink-mute mt-1">商品管理、订单管理</p>
        </router-link>
      </div>
    </div>

    <!-- ========== 地址表单弹窗 ========== -->
    <div v-if="showAddressForm" class="fixed inset-0 z-50 flex items-center justify-center p-4"
      @click.self="cancelAddressForm">
      <!-- 遮罩 -->
      <div class="absolute inset-0 bg-black/20 backdrop-blur-sm"></div>
      <!-- 弹窗 -->
      <div class="relative bg-white rounded-2xl w-full max-w-lg p-8 shadow-float">
        <h3 class="text-[18px] font-normal text-ink mb-6">{{ addressForm.id ? '编辑地址' : '新增地址' }}</h3>

        <div class="space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="text-[13px] font-normal text-ink-mute block mb-1">收件人 *</label>
              <input v-model="addressForm.receiverName" placeholder="姓名"
                class="w-full px-4 py-2.5 border border-hairline-input rounded-lg text-[14px] focus:outline-none focus:border-primary">
            </div>
            <div>
              <label class="text-[13px] font-normal text-ink-mute block mb-1">手机号 *</label>
              <input v-model="addressForm.receiverPhone" placeholder="手机号码"
                class="w-full px-4 py-2.5 border border-hairline-input rounded-lg text-[14px] focus:outline-none focus:border-primary">
            </div>
          </div>

          <div class="grid grid-cols-3 gap-3">
            <div>
              <label class="text-[13px] font-normal text-ink-mute block mb-1">省份</label>
              <select v-model="addressForm.province" @change="onProvinceChange"
                class="w-full px-3 py-2.5 border border-hairline-input rounded-lg text-[14px] focus:outline-none focus:border-primary bg-white">
                <option value="">选择</option>
                <option v-for="d in presetDistricts" :key="d.province" :value="d.province">{{ d.province }}</option>
              </select>
            </div>
            <div>
              <label class="text-[13px] font-normal text-ink-mute block mb-1">城市</label>
              <select v-model="addressForm.city" :disabled="!addressForm.province"
                class="w-full px-3 py-2.5 border border-hairline-input rounded-lg text-[14px] focus:outline-none focus:border-primary bg-white disabled:opacity-50">
                <option value="">选择</option>
                <option v-for="c in filteredCities" :key="c" :value="c">{{ c }}</option>
              </select>
            </div>
            <div>
              <label class="text-[13px] font-normal text-ink-mute block mb-1">区/县</label>
              <input v-model="addressForm.district" placeholder="区/县"
                class="w-full px-3 py-2.5 border border-hairline-input rounded-lg text-[14px] focus:outline-none focus:border-primary">
            </div>
          </div>

          <div>
            <label class="text-[13px] font-normal text-ink-mute block mb-1">详细地址 *</label>
            <input v-model="addressForm.detailAddress" placeholder="街道、门牌号等"
              class="w-full px-4 py-2.5 border border-hairline-input rounded-lg text-[14px] focus:outline-none focus:border-primary">
          </div>

          <div class="flex items-center gap-2">
            <input type="checkbox" v-model="addressForm.isDefault" :true-value="1" :false-value="0"
              class="w-4 h-4 rounded border-hairline text-primary focus:ring-primary">
            <label class="text-[14px] text-ink-mute cursor-pointer">设为默认地址</label>
          </div>
        </div>

        <div class="flex gap-3 mt-8">
          <button @click="saveAddress" class="flex-1 py-3 bg-primary text-white text-[14px] font-normal rounded-pill hover:bg-primary-deep transition-colors">保存</button>
          <button @click="cancelAddressForm" class="flex-1 py-3 border border-hairline text-ink text-[14px] font-normal rounded-pill hover:border-primary transition-colors">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>
