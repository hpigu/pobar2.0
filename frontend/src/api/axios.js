import axios from 'axios'
import router from '@/router'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 10000,
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  const sessionToken = localStorage.getItem('sessionToken')
  if (sessionToken) config.headers['X-Session-Token'] = sessionToken
  return config
})

// ─── 401 自動 refresh + retry ───
let isRefreshing = false
let waitQueue = []  // 多個 request 等同一次 refresh

function flushQueue(error, newToken) {
  waitQueue.forEach(({ resolve, reject, config }) => {
    if (error) reject(error)
    else {
      config.headers.Authorization = `Bearer ${newToken}`
      resolve(api(config))
    }
  })
  waitQueue = []
}

function fullLogoutAndRedirect() {
  // 動態 import 避免循環依賴
  import('@/stores/auth').then(({ useAuthStore }) => {
    useAuthStore().clearLocal()
    router.push('/login')
  })
}

api.interceptors.response.use(
  res => res,
  async err => {
    const status = err.response?.status
    const code = err.response?.data?.code
    const original = err.config || {}

    // 強制改密碼 → 跳改密碼頁
    if (code === 1006) {
      router.push('/change-password')
      return Promise.reject(err)
    }

    // 不處理：refresh 本身失敗、login 本身失敗、已經 retry 過的
    const isAuthEndpoint = (original.url || '').includes('/api/auth/')
    const shouldRetry = (status === 401 || code === 1004 || code === 1005)
                     && !original._retried
                     && !isAuthEndpoint
                     && localStorage.getItem('refreshToken')

    if (!shouldRetry) {
      // 真的失效 → 清登入狀態跳 login
      if (status === 401 || code === 1004 || code === 1005) {
        fullLogoutAndRedirect()
      }
      return Promise.reject(err)
    }

    original._retried = true

    if (isRefreshing) {
      // 其他 request 也卡 401，把自己排隊等
      return new Promise((resolve, reject) => {
        waitQueue.push({ resolve, reject, config: original })
      })
    }

    isRefreshing = true
    try {
      const { useAuthStore } = await import('@/stores/auth')
      const auth = useAuthStore()
      await auth.refreshAccess()
      const newToken = auth.token
      flushQueue(null, newToken)
      original.headers.Authorization = `Bearer ${newToken}`
      return api(original)
    } catch (refreshErr) {
      flushQueue(refreshErr, null)
      fullLogoutAndRedirect()
      return Promise.reject(refreshErr)
    } finally {
      isRefreshing = false
    }
  }
)

export default api
