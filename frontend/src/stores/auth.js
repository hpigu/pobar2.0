import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api/axios'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const mustChangePassword = ref(localStorage.getItem('mustChangePassword') === '1')

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => user.value?.role || '')

  function persist(data) {
    token.value = data.token
    refreshToken.value = data.refreshToken || ''
    user.value = { account: data.account, role: data.role }
    mustChangePassword.value = !!data.mustChangePassword
    localStorage.setItem('token', token.value)
    if (refreshToken.value) localStorage.setItem('refreshToken', refreshToken.value)
    else localStorage.removeItem('refreshToken')
    localStorage.setItem('user', JSON.stringify(user.value))
    localStorage.setItem('mustChangePassword', mustChangePassword.value ? '1' : '0')
  }

  async function login(account, password, rememberDevice = false) {
    const res = await api.post('/api/auth/login', { account, password, rememberDevice })
    persist(res.data.data)
  }

  /** 用 refresh token 換新 access + rotated refresh。失敗會丟例外（呼叫端應 logout） */
  async function refreshAccess() {
    if (!refreshToken.value) throw new Error('no refresh token')
    const res = await api.post('/api/auth/refresh', { refreshToken: refreshToken.value })
    persist(res.data.data)
  }

  async function changePassword(oldPassword, newPassword) {
    const res = await api.post('/api/auth/change-password', { oldPassword, newPassword })
    persist(res.data.data)
  }

  async function logout() {
    try {
      await api.post('/api/auth/logout', { refreshToken: refreshToken.value })
    } catch (_) {}
    clearLocal()
  }

  function clearLocal() {
    token.value = ''
    refreshToken.value = ''
    user.value = null
    mustChangePassword.value = false
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
    localStorage.removeItem('mustChangePassword')
  }

  return {
    token, refreshToken, user, isLoggedIn, role, mustChangePassword,
    login, refreshAccess, changePassword, logout, clearLocal,
  }
})
