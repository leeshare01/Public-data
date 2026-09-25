<script setup>
import { ref, reactive, watch } from 'vue'
import { useUserStore } from '../stores/user'
import { useToastStore } from '../stores/toast'
import { useUIStore } from '../stores/ui'

const props = defineProps({
  visible: { type: Boolean, default: false },
})
const emit = defineEmits(['close'])
const ui = useUIStore()
const userStore = useUserStore()
const toast = useToastStore()

const authTab = ref('login')
const loginForm = reactive({ account: '', password: '' })
const regForm = reactive({
  nickname: '', username: '', phone: '', email: '', password: '', confirm: '',
})
const loginError = ref('')
const regError = ref('')
const submitting = ref(false)

watch(() => props.visible, (v) => {
  if (v) {
    authTab.value = ui.authTab
    loginError.value = ''
    regError.value = ''
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
})

function close() {
  emit('close')
}

function switchTab(tab) {
  authTab.value = tab
  loginError.value = ''
  regError.value = ''
}

async function handleLogin() {
  loginError.value = ''
  if (!loginForm.account || !loginForm.password) {
    loginError.value = '请填写账号和密码'
    return
  }
  submitting.value = true
  try {
    const loginType = loginForm.account.includes('@') ? 'email' : /^1\d{10}$/.test(loginForm.account) ? 'phone' : 'username'
    await userStore.login(loginForm.account, loginForm.password, loginType)
    toast.show('欢迎回来！')
    close()
  } catch (e) {
    loginError.value = e.message || '登录失败，请重试'
  } finally {
    submitting.value = false
  }
}

async function handleRegister() {
  regError.value = ''
  if (!regForm.nickname || !regForm.username || !regForm.email || !regForm.password) {
    regError.value = '请填写昵称、用户名、邮箱和密码'
    return
  }
  if (!/^[a-zA-Z0-9]{4,20}$/.test(regForm.username)) {
    regError.value = '用户名需为4-20位字母或数字'
    return
  }
  if (regForm.password.length < 6) {
    regError.value = '密码至少6位'
    return
  }
  if (regForm.password !== regForm.confirm) {
    regError.value = '两次密码输入不一致'
    return
  }
  submitting.value = true
  try {
    await userStore.register(regForm)
    toast.show('注册成功，请登录')
    switchTab('login')
    loginForm.account = regForm.email
  } catch (e) {
    regError.value = e.message || '注册失败，请重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div class="modal-overlay" :class="{ active: visible }">
      <div class="backdrop" @click="close" />
      <div class="modal-panel w-full max-w-md bg-white rounded-2xl shadow-2xl mx-4 p-8">
        <div class="flex items-center justify-between mb-6">
          <div class="flex gap-1 bg-canvas-soft rounded-pill p-1">
            <button class="auth-tab px-6 py-2 text-[14px] font-normal rounded-pill transition-all"
              :class="{ 'active bg-white shadow-sm': authTab === 'login' }" @click="switchTab('login')">登录</button>
            <button class="auth-tab px-6 py-2 text-[14px] font-normal rounded-pill transition-all"
              :class="{ 'active bg-white shadow-sm': authTab === 'register' }" @click="switchTab('register')">注册</button>
          </div>
          <button @click="close" class="w-9 h-9 rounded-lg hover:bg-canvas-soft flex items-center justify-center transition-colors">
            <svg class="w-5 h-5 text-ink-mute" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <div v-show="authTab === 'login'">
          <div class="mb-4">
            <label class="text-[13px] font-normal text-ink-mute block mb-1.5">邮箱 / 用户名 / 手机号</label>
            <input v-model="loginForm.account" type="text" placeholder="your@email.com"
              class="w-full px-4 py-3 border border-hairline-input rounded-lg text-[14px] font-light text-ink focus:outline-none focus:border-primary transition-colors" @keyup.enter="handleLogin">
          </div>
          <div class="mb-6">
            <label class="text-[13px] font-normal text-ink-mute block mb-1.5">密码</label>
            <input v-model="loginForm.password" type="password" placeholder="输入密码"
              class="w-full px-4 py-3 border border-hairline-input rounded-lg text-[14px] font-light text-ink focus:outline-none focus:border-primary transition-colors" @keyup.enter="handleLogin">
          </div>
          <button @click="handleLogin" :disabled="submitting"
            class="w-full py-3 bg-primary text-white text-[15px] font-normal rounded-pill hover:bg-primary-deep active:bg-primary-press transition-colors duration-200 disabled:opacity-50">
            {{ submitting ? '登录中...' : '登录' }}
          </button>
          <p v-if="loginError" class="text-[12px] text-ruby mt-2">{{ loginError }}</p>
        </div>

        <div v-show="authTab === 'register'">
          <div class="mb-4">
            <label class="text-[13px] font-normal text-ink-mute block mb-1.5">昵称</label>
            <input v-model="regForm.nickname" type="text" placeholder="你的昵称"
              class="w-full px-4 py-3 border border-hairline-input rounded-lg text-[14px] font-light text-ink focus:outline-none focus:border-primary transition-colors">
          </div>
          <div class="mb-4">
            <label class="text-[13px] font-normal text-ink-mute block mb-1.5">用户名 <span class="text-ink-mute/60">（4-20位字母数字）</span></label>
            <input v-model="regForm.username" type="text" placeholder="myusername"
              class="w-full px-4 py-3 border border-hairline-input rounded-lg text-[14px] font-light text-ink focus:outline-none focus:border-primary transition-colors">
          </div>
          <div class="mb-4">
            <label class="text-[13px] font-normal text-ink-mute block mb-1.5">手机号 <span class="text-ink-mute/60">（选填）</span></label>
            <input v-model="regForm.phone" type="tel" placeholder="13800138000"
              class="w-full px-4 py-3 border border-hairline-input rounded-lg text-[14px] font-light text-ink focus:outline-none focus:border-primary transition-colors">
          </div>
          <div class="mb-4">
            <label class="text-[13px] font-normal text-ink-mute block mb-1.5">邮箱</label>
            <input v-model="regForm.email" type="email" placeholder="your@email.com"
              class="w-full px-4 py-3 border border-hairline-input rounded-lg text-[14px] font-light text-ink focus:outline-none focus:border-primary transition-colors">
          </div>
          <div class="mb-4">
            <label class="text-[13px] font-normal text-ink-mute block mb-1.5">密码</label>
            <input v-model="regForm.password" type="password" placeholder="至少6位密码"
              class="w-full px-4 py-3 border border-hairline-input rounded-lg text-[14px] font-light text-ink focus:outline-none focus:border-primary transition-colors">
          </div>
          <div class="mb-6">
            <label class="text-[13px] font-normal text-ink-mute block mb-1.5">确认密码</label>
            <input v-model="regForm.confirm" type="password" placeholder="再次输入密码"
              class="w-full px-4 py-3 border border-hairline-input rounded-lg text-[14px] font-light text-ink focus:outline-none focus:border-primary transition-colors">
          </div>
          <button @click="handleRegister" :disabled="submitting"
            class="w-full py-3 bg-primary text-white text-[15px] font-normal rounded-pill hover:bg-primary-deep active:bg-primary-press transition-colors duration-200 disabled:opacity-50">
            {{ submitting ? '注册中...' : '注册' }}
          </button>
          <p v-if="regError" class="text-[12px] text-ruby mt-2">{{ regError }}</p>
        </div>
      </div>
    </div>
  </Teleport>
</template>
