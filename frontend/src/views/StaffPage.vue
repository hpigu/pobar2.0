<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const tables = ref([])
const sessions = ref([])
const selectedSession = ref(null)
const sessionItems = ref([])
const preview = ref(null)
const checkoutDialog = ref(false)
const checkoutForm = ref({ paymentMethod: 'CASH', splitCount: 1, carrierType: null, carrierId: '' })
const reservations = ref([])
const reservationDate = ref(new Date().toISOString().split('T')[0])

// 訂位日期選擇
async function loadReservations() {
  const res = await api.get(`/api/reservations?date=${reservationDate.value}`)
  reservations.value = res.data.data || []
}

async function loadTables() {
  const [tbRes] = await Promise.all([api.get('/api/tables')])
  tables.value = tbRes.data.data || []
}

async function selectTable(table) {
  if (!table.currentSessionId) return
  selectedSession.value = table
  const res = await api.get(`/api/orders/session/${table.currentSessionId}`)
  sessionItems.value = res.data.data || []
}

async function openCheckout() {
  if (!selectedSession.value?.currentSessionId) return
  const res = await api.get(`/api/sessions/${selectedSession.value.currentSessionId}/payment/preview`)
  preview.value = res.data.data
  checkoutForm.value = { paymentMethod: 'CASH', splitCount: 1, carrierType: null, carrierId: '' }
  checkoutDialog.value = true
}

async function confirmCheckout() {
  try {
    await api.post(`/api/sessions/${selectedSession.value.currentSessionId}/payment`, checkoutForm.value)
    ElMessage.success('結帳完成')
    checkoutDialog.value = false
    selectedSession.value = null
    preview.value = null
    sessionItems.value = []
    await loadTables()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '結帳失敗')
  }
}

async function updateReservation(id, status) {
  try {
    await api.patch(`/api/reservations/${id}/status`, { status })
    await loadReservations()
  } catch { ElMessage.error('更新失敗') }
}

onMounted(() => {
  loadTables()
  loadReservations()
})

function statusColor(s) {
  return { OPEN: '#67c23a', CLOSED: '#909399' }[s] || '#eee'
}
</script>

<template>
  <div style="padding:20px">
    <el-row :gutter="20">
      <!-- 左側：桌位地圖 -->
      <el-col :span="14">
        <h3>桌位狀態</h3>
        <div class="table-grid">
          <div v-for="t in tables" :key="t.id"
            class="table-card"
            :class="{ open: t.status === 'OPEN', selected: selectedSession?.id === t.id }"
            @click="selectTable(t)">
            <div class="table-name">{{ t.name }}</div>
            <el-tag :color="statusColor(t.status)" size="small" effect="plain">
              {{ t.status === 'OPEN' ? '使用中' : '空桌' }}
            </el-tag>
          </div>
        </div>

        <!-- 選中桌位的訂單 -->
        <div v-if="selectedSession" style="margin-top:20px">
          <div style="display:flex; justify-content:space-between; align-items:center">
            <h4>{{ selectedSession.name }} 的訂單</h4>
            <el-button type="primary" @click="openCheckout">結帳</el-button>
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

      <!-- 右側：訂位管理 -->
      <el-col :span="10">
        <h3>訂位管理</h3>
        <div style="display:flex; gap:8px; margin-bottom:12px">
          <el-date-picker v-model="reservationDate" type="date" value-format="YYYY-MM-DD"
            style="flex:1" @change="loadReservations" />
        </div>
        <el-table :data="reservations" size="small">
          <el-table-column prop="guestName" label="姓名" width="70" />
          <el-table-column prop="partySize" label="人數" width="50" />
          <el-table-column label="時間" width="80">
            <template #default="{ row }">{{ row.reservedAt?.slice(11,16) }}</template>
          </el-table-column>
          <el-table-column label="狀態" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="{ PENDING:'info', CONFIRMED:'success', SEATED:'warning', CANCELLED:'danger', NO_SHOW:'danger' }[row.status]">
                {{ { PENDING:'待確認', CONFIRMED:'已確認', SEATED:'已入座', CANCELLED:'取消', NO_SHOW:'未到場' }[row.status] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button v-if="row.status === 'PENDING'" link type="success" size="small"
                @click="updateReservation(row.id, 'CONFIRMED')">確認</el-button>
              <el-button v-if="row.status === 'CONFIRMED'" link type="primary" size="small"
                @click="updateReservation(row.id, 'SEATED')">入座</el-button>
              <el-button v-if="['PENDING','CONFIRMED'].includes(row.status)" link type="danger" size="small"
                @click="updateReservation(row.id, 'CANCELLED')">取消</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-col>
    </el-row>

    <!-- 結帳 dialog -->
    <el-dialog v-model="checkoutDialog" title="結帳" width="400px">
      <div v-if="preview" style="margin-bottom:16px">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="小計">NT$ {{ preview.subtotal }}</el-descriptions-item>
          <el-descriptions-item label="服務費 ({{ (preview.serviceChargeRate * 100).toFixed(0) }}%)">
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
          <el-input v-model="checkoutForm.carrierId" placeholder="請輸入載具號碼" />
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
.table-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(100px, 1fr)); gap: 12px; }
.table-card {
  padding: 16px 8px; text-align: center; border-radius: 8px;
  border: 2px solid #eee; cursor: pointer; transition: all .2s;
}
.table-card.open { border-color: #67c23a; background: #f0f9eb; }
.table-card.selected { border-color: #409eff; background: #ecf5ff; }
.table-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,.1); }
.table-name { font-weight: 700; font-size: 16px; margin-bottom: 6px; }
</style>
