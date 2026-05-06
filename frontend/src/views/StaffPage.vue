<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import api from '@/api/axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const auth = useAuthStore()

const tables = ref([])
const sessions = ref([])
const selectedTable = ref(null)
const sessionItems = ref([])
const preview = ref(null)

// 開桌
const openDialog = ref(false)
const openForm = ref({ guestCount: 2 })

// QR Code
const qrDialog = ref(false)
const qrUrl = ref('')

// 結帳
const checkoutDialog = ref(false)
const checkoutForm = ref({ paymentMethod: 'CASH', splitCount: 1, carrierType: null, carrierId: '' })

// 訂位
const reservations = ref([])
const reservationDate = ref(new Date().toISOString().split('T')[0])

async function loadTables() {
  const res = await api.get('/api/tables')
  tables.value = res.data.data || []
}

async function loadReservations() {
  const res = await api.get(`/api/reservations?date=${reservationDate.value}`)
  reservations.value = res.data.data || []
}

async function selectTable(table) {
  selectedTable.value = table
  sessionItems.value = []
  if (table.currentSessionId) {
    const res = await api.get(`/api/orders/session/${table.currentSessionId}`)
    sessionItems.value = res.data.data || []
  }
}

// 開桌
function clickTable(table) {
  if (table.status === 'OPEN') {
    selectTable(table)
  } else {
    selectedTable.value = table
    openForm.value = { guestCount: Math.min(2, table.capacity) }
    openDialog.value = true
  }
}

async function confirmOpen() {
  try {
    const res = await api.post('/api/tables/sessions', {
      tableIds: [selectedTable.value.id],
      partySize: openForm.value.guestCount
    })
    const session = res.data.data
    openDialog.value = false
    ElMessage.success('開桌成功')
    await loadTables()
    // 重新選取更新後的桌位
    const updated = tables.value.find(t => t.id === selectedTable.value.id)
    if (updated) selectTable(updated)
    // 顯示 QR Code（token 來自 openSession 回傳的 TableSession.qrToken）
    qrUrl.value = `${window.location.origin}/order/${session.qrToken}`
    qrDialog.value = true
  } catch { ElMessage.error('開桌失敗') }
}

// 關桌
async function closeTable() {
  if (!selectedTable.value?.currentSessionId) return
  await ElMessageBox.confirm('確定關桌？', '確認', { type: 'warning' })
  try {
    await api.delete(`/api/tables/sessions/${selectedTable.value.currentSessionId}`)
    ElMessage.success('已關桌')
    selectedTable.value = null
    sessionItems.value = []
    loadTables()
  } catch { ElMessage.error('關桌失敗') }
}

// 顯示 QR Code（sessionQrToken 來自 BarTableVO JOIN 查詢）
function showQr() {
  if (!selectedTable.value) return
  qrUrl.value = `${window.location.origin}/order/${selectedTable.value.sessionQrToken}`
  qrDialog.value = true
}

// 結帳
async function openCheckout() {
  if (!selectedTable.value?.currentSessionId) return
  const res = await api.get(`/api/sessions/${selectedTable.value.currentSessionId}/payment/preview`)
  preview.value = res.data.data
  checkoutForm.value = { paymentMethod: 'CASH', splitCount: 1, carrierType: null, carrierId: '' }
  checkoutDialog.value = true
}

async function confirmCheckout() {
  try {
    await api.post(`/api/sessions/${selectedTable.value.currentSessionId}/payment`, checkoutForm.value)
    ElMessage.success('結帳完成')
    checkoutDialog.value = false
    selectedTable.value = null
    preview.value = null
    sessionItems.value = []
    loadTables()
  } catch { ElMessage.error(e.response?.data?.message || '結帳失敗') }
}

// 訂位
async function updateReservation(id, status) {
  try {
    await api.patch(`/api/reservations/${id}/status`, { status })
    loadReservations()
  } catch { ElMessage.error('更新失敗') }
}

// 登出
async function logout() {
  await auth.logout()
  router.push('/login')
}

function statusColor(s) {
  return { OPEN: '#67c23a', CLOSED: '#909399' }[s] || '#eee'
}

onMounted(() => {
  loadTables()
  loadReservations()
})
</script>

<template>
  <div style="padding:20px">
    <!-- Header -->
    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:16px">
      <h2 style="margin:0">服務生工作台</h2>
      <el-button @click="logout" plain>登出</el-button>
    </div>

    <el-row :gutter="20">
      <!-- 左側：桌位平面圖 -->
      <el-col :span="14">
        <h3 style="margin-bottom:8px">桌位狀態</h3>
        <div style="font-size:12px; color:#999; margin-bottom:8px">
          綠色框 = 使用中　｜　灰色框 = 空桌　｜　點擊桌位操作
        </div>
        <div id="staff-floor"
          style="width:700px; height:440px; position:relative; background:#f8f9fa;
                 border:2px dashed #dee2e6; border-radius:8px; overflow:hidden; user-select:none">
          <svg width="700" height="440" style="position:absolute; inset:0; pointer-events:none">
            <defs>
              <pattern id="sgrid" width="40" height="40" patternUnits="userSpaceOnUse">
                <path d="M 40 0 L 0 0 0 40" fill="none" stroke="#e9ecef" stroke-width="1"/>
              </pattern>
            </defs>
            <rect width="100%" height="100%" fill="url(#sgrid)" />
          </svg>
          <div v-for="t in tables" :key="t.id"
            :style="`position:absolute; left:${t.posX ?? 0}px; top:${t.posY ?? 0}px;
                     width:90px; height:60px; cursor:pointer`"
            @click="clickTable(t)">
            <el-card shadow="hover"
              :style="`height:100%; border:2px solid ${t.status === 'OPEN' ? '#67c23a' : (selectedTable?.id === t.id ? '#409eff' : '#dcdfe6')};
                       background:${t.status === 'OPEN' ? '#f0f9eb' : (selectedTable?.id === t.id ? '#ecf5ff' : '#fff')}`"
              :body-style="{ padding: '6px', height: '100%', boxSizing: 'border-box' }">
              <div style="font-weight:700; font-size:13px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis">
                {{ t.name }}
              </div>
              <div style="font-size:11px; color:#909399">{{ t.capacity }}人</div>
              <div style="font-size:10px; margin-top:2px"
                :style="{ color: t.status === 'OPEN' ? '#67c23a' : '#c0c4cc' }">
                {{ t.status === 'OPEN' ? '使用中' : '空桌' }}
              </div>
            </el-card>
          </div>
        </div>

        <!-- 選中桌位的訂單 -->
        <div v-if="selectedTable?.status === 'OPEN'" style="margin-top:20px">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:8px">
            <h4 style="margin:0">{{ selectedTable.name }} 的訂單</h4>
            <div style="display:flex; gap:8px">
              <el-button size="small" @click="showQr">QR Code</el-button>
              <el-button size="small" type="warning" @click="closeTable">關桌</el-button>
              <el-button size="small" type="primary" @click="openCheckout">結帳</el-button>
            </div>
          </div>
          <el-table :data="sessionItems" size="small">
            <el-table-column prop="productName" label="品項" />
            <el-table-column prop="quantity" label="數量" width="60" />
            <el-table-column prop="price" label="單價" width="80" />
            <el-table-column label="狀態" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="{ PENDING:'danger', IN_PROGRESS:'warning', READY:'success', CANCELLED:'info' }[row.status]">
                  {{ { PENDING:'待製作', IN_PROGRESS:'製作中', READY:'完成', CANCELLED:'已取消' }[row.status] }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>

      <!-- 右側：訂位 -->
      <el-col :span="10">
        <h3>訂位管理</h3>
        <div style="margin-bottom:12px">
          <el-date-picker v-model="reservationDate" type="date" value-format="YYYY-MM-DD"
            style="width:100%" @change="loadReservations" />
        </div>
        <el-table :data="reservations" size="small">
          <el-table-column prop="customerName" label="姓名" width="80" />
          <el-table-column prop="partySize" label="人數" width="50" />
          <el-table-column label="時間" width="70">
            <template #default="{ row }">{{ row.reservedAt?.slice(11,16) }}</template>
          </el-table-column>
          <el-table-column label="狀態" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="{ CONFIRMED:'success', SEATED:'warning', CANCELLED:'danger', NO_SHOW:'danger' }[row.status] || 'info'">
                {{ { CONFIRMED:'已確認', SEATED:'已入座', CANCELLED:'取消', NO_SHOW:'未到場' }[row.status] || row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110">
            <template #default="{ row }">
              <el-button v-if="row.status === 'CONFIRMED'" link type="primary" size="small"
                @click="updateReservation(row.id, 'SEATED')">入座</el-button>
              <el-button v-if="['CONFIRMED','SEATED'].includes(row.status)" link type="danger" size="small"
                @click="updateReservation(row.id, 'CANCELLED')">取消</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-col>
    </el-row>

    <!-- 開桌 Dialog -->
    <el-dialog v-model="openDialog" :title="`開桌 — ${selectedTable?.name}`" width="320px">
      <el-form label-width="80px">
        <el-form-item label="用餐人數">
          <el-input-number v-model="openForm.guestCount" :min="1" :max="selectedTable?.capacity || 20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="openDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmOpen">開桌</el-button>
      </template>
    </el-dialog>

    <!-- QR Code Dialog -->
    <el-dialog v-model="qrDialog" title="掃碼點餐" width="300px">
      <div style="text-align:center; padding:8px">
        <img :src="`https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(qrUrl)}`"
          style="width:200px; height:200px" alt="QR Code" />
        <div style="margin-top:8px; font-size:12px; color:#999; word-break:break-all">{{ qrUrl }}</div>
      </div>
      <template #footer>
        <el-button type="primary" @click="qrDialog = false">關閉</el-button>
      </template>
    </el-dialog>

    <!-- 結帳 Dialog -->
    <el-dialog v-model="checkoutDialog" title="結帳" width="400px">
      <div v-if="preview" style="margin-bottom:16px">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="小計">NT$ {{ preview.subtotal }}</el-descriptions-item>
          <el-descriptions-item :label="`服務費 (${(preview.serviceChargeRate * 100).toFixed(0)}%)`">
            NT$ {{ preview.serviceCharge }}
          </el-descriptions-item>
          <el-descriptions-item label="合計">
            <strong style="font-size:18px; color:#f56c6c">NT$ {{ preview.total }}</strong>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <el-form label-width="80px">
        <el-form-item label="付款方式">
          <el-radio-group v-model="checkoutForm.paymentMethod">
            <el-radio label="CASH">現金</el-radio>
            <el-radio label="CARD">刷卡</el-radio>
            <el-radio label="OTHER">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="均攤人數">
          <el-input-number v-model="checkoutForm.splitCount" :min="1" :max="20" />
          <span v-if="preview && checkoutForm.splitCount > 1" style="margin-left:8px; color:#f56c6c">
            每人 NT$ {{ Math.ceil(preview.total / checkoutForm.splitCount) }}
          </span>
        </el-form-item>
        <el-form-item label="電子發票">
          <el-select v-model="checkoutForm.carrierType" placeholder="不開立" clearable>
            <el-option label="手機條碼" value="MOBILE_BARCODE" />
            <el-option label="自然人憑證" value="CITIZEN_CERT" />
            <el-option label="紙本發票" value="PAPER" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="checkoutForm.carrierType && checkoutForm.carrierType !== 'PAPER'" label="載具號碼">
          <el-input v-model="checkoutForm.carrierId" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="checkoutDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmCheckout">確認結帳</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
</style>
