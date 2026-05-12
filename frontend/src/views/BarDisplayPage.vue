<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useWebSocket } from '@/composables/useWebSocket'
import api from '@/api/axios'
import { ElMessage } from 'element-plus'

const router = useRouter()
const auth = useAuthStore()
const items = ref([])
let pollTimer = null

async function logout() {
  await auth.logout()
  router.push('/login')
}

async function fetchItems() {
  const res = await api.get('/api/orders/display?type=DRINK')
  items.value = res.data.data || []
}

async function updateStatus(id, status) {
  try {
    await api.put(`/api/orders/items/${id}/status`, { status })
    await fetchItems()
  } catch { ElMessage.error('更新失敗') }
}

function playNotify() {
  try {
    const ctx = new AudioContext()
    const osc = ctx.createOscillator()
    osc.connect(ctx.destination)
    osc.frequency.value = 660
    osc.start(); osc.stop(ctx.currentTime + 0.15)
  } catch (_) {}
}

const { connect } = useWebSocket('/topic/bar', () => {
  fetchItems()
  playNotify()
})

onMounted(() => {
  fetchItems()
  connect()
  pollTimer = setInterval(fetchItems, 30000)
})
onUnmounted(() => clearInterval(pollTimer))

function statusLabel(s) {
  return { PENDING: '待調製', IN_PROGRESS: '調製中', READY: '完成' }[s] || s
}
function statusType(s) {
  return { PENDING: 'danger', IN_PROGRESS: 'warning', READY: 'success' }[s] || 'info'
}
function elapsedMin(createdAt) {
  return Math.floor((Date.now() - new Date(createdAt)) / 60000)
}
</script>

<template>
  <div class="display-page">
    <div class="display-header">
      <span>🍹 吧台顯示</span>
      <div style="display:flex; align-items:center; gap:16px">
        <span style="font-size:14px; color:#aaa">{{ items.length }} 筆待處理</span>
        <el-button size="small" plain @click="logout" style="color:#aaa; border-color:#444; background:transparent">登出</el-button>
      </div>
    </div>

    <div v-if="items.length === 0" class="empty-state">
      <el-empty description="目前沒有待調製的飲品" />
    </div>

    <div class="order-grid">
      <el-card v-for="item in items" :key="item.id"
        class="order-card" :class="item.status.toLowerCase()">
        <div class="card-header">
          <span class="table-label">{{ item.tableNames || `Session ${item.sessionId}` }}</span>
          <el-tag :type="statusType(item.status)" size="small">{{ statusLabel(item.status) }}</el-tag>
        </div>
        <div class="item-name">
          {{ item.productName }}
          <span class="item-qty">× {{ item.quantity }}</span>
        </div>
        <div v-if="item.ingredientNames" class="item-ingredients">{{ item.ingredientNames }}</div>
        <div v-if="item.notes" class="item-note">📝 {{ item.notes }}</div>
        <div class="item-elapsed">{{ elapsedMin(item.createdAt) }} 分鐘前</div>
        <div class="card-actions">
          <el-button type="success" size="small"
            @click="updateStatus(item.id, 'READY')">完成</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.display-page { min-height: 100vh; background: #0d1117; color: #fff; padding: 16px; }
.display-header {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 24px; font-weight: 700; margin-bottom: 16px;
}
.empty-state { display: flex; align-items: center; justify-content: center; height: 60vh; }
.order-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; }
.order-card { background: #161b22 !important; border: none !important; }
.order-card.pending { border-left: 4px solid #f56c6c !important; }
.order-card.in_progress { border-left: 4px solid #e6a23c !important; }
.order-card.ready { border-left: 4px solid #67c23a !important; }
.card-header { display: flex; justify-content: space-between; margin-bottom: 8px; }
.table-label { font-weight: 700; font-size: 16px; color: #58a6ff; }
.item-name { font-size: 18px; font-weight: 700; color: #fff; margin: 4px 0; }
.item-qty { font-size: 16px; font-weight: 700; color: #58a6ff; margin-left: 6px; }
.item-ingredients { font-size: 13px; color: #8b949e; margin: 4px 0 2px; line-height: 1.6; }
.item-note { font-size: 13px; color: #aaa; margin-top: 4px; }
.item-elapsed { font-size: 12px; color: #666; margin-top: 4px; }
.card-actions { margin-top: 12px; }
</style>
