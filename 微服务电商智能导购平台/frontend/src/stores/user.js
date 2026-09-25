import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi } from '../api/user'

export const useUserStore = defineStore('user', () => {
  const user = ref(JSON.parse(localStorage.getItem('eshop_user') || 'null'))
  const token = ref(localStorage.getItem('eshop_token') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  function setAuth(userData, tokenStr) {
    user.value = userData
    token.value = tokenStr
    localStorage.setItem('eshop_user', JSON.stringify(userData))
    localStorage.setItem('eshop_token', tokenStr)
  }

  function clearAuth() {
    user.value = null
    token.value = ''
    localStorage.removeItem('eshop_user')
    localStorage.removeItem('eshop_token')
  }

  async function login(account, password, loginType = 'username') {
    const data = await userApi.login({ loginType, account, password })
    setAuth(
      { userId: data.userId, username: data.username, nickname: data.nickname, role: data.role },
      data.token,
    )
    return data
  }

  async function register(form) {
    const data = await userApi.register({
      username: form.username,
      password: form.password,
      phone: form.phone || '',
      email: form.email,
      nickname: form.nickname || form.username,
    })
    return data
  }

  async function fetchProfile() {
    const data = await userApi.getMe()
    user.value = { ...user.value, ...data }
    localStorage.setItem('eshop_user', JSON.stringify(user.value))
    return data
  }

  function logout() {
    userApi.logout().catch(() => {})
    clearAuth()
  }

  return {
    user, token, isLoggedIn, isAdmin,
    login, register, logout, fetchProfile, setAuth, clearAuth,
  }
})
