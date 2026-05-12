import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api/axios'

export const useCartStore = defineStore('cart', () => {
  const items = ref([])
  const sessionToken = ref(localStorage.getItem('sessionToken') || '')

  const totalCount = computed(() => items.value.reduce((s, i) => s + i.quantity, 0))
  const subtotal = computed(() =>
    items.value.reduce((s, i) => s + Number(i.price ?? 0) * i.quantity, 0)
  )

  function setSession(token) {
    sessionToken.value = token
    localStorage.setItem('sessionToken', token)
  }

  function syncFromWs(serverItems) {
    items.value = serverItems || []
  }

  async function addItem(item) {
    await api.post('/api/cart/items', item)
  }

  async function removeItem(itemKey) {
    await api.delete(`/api/cart/items/${itemKey}`)
  }

  async function fetchCart() {
    const res = await api.get('/api/cart')
    items.value = res.data.data || []
  }

  function clear() {
    items.value = []
  }

  return { items, sessionToken, totalCount, subtotal, setSession, syncFromWs, addItem, removeItem, fetchCart, clear }
})
