<script setup>
import { ref, computed } from 'vue'
import api from '@/api/axios'
import { ElMessage } from 'element-plus'

const step = ref(1)   // 1 = 選日期, 2 = 選時間, 3 = 填資料, 4 = 成功

const selectedDate = ref(null)   // 'YYYY-MM-DD'
const selectedTime = ref(null)   // 'HH:mm'
const slots = ref([])
const loadingSlots = ref(false)

const form = ref({ customerName: '', customerPhone: '', partySize: 2, notes: '' })
const submitting = ref(false)

function disabledDate(d) {
  const today = new Date(); today.setHours(0, 0, 0, 0)
  const limit = new Date(today); limit.setDate(limit.getDate() + 60)
  return d < today || d > limit
}

async function onDateSelect(val) {
  selectedDate.value = val
  selectedTime.value = null
  loadingSlots.value = true
  try {
    const res = await api.get(`/api/reservations/slots?date=${val}`)
    slots.value = res.data.data || []
    step.value = 2
  } catch {
    ElMessage.error('載入時段失敗')
  } finally {
    loadingSlots.value = false
  }
}

function isPastSlot(time) {
  if (!selectedDate.value) return false
  const now = new Date()
  const [h, m] = time.split(':').map(Number)
  const slotDt = new Date(selectedDate.value)
  slotDt.setHours(h, m, 0, 0)
  return slotDt <= now
}

function selectTime(slot) {
  if (!slot.available || isPastSlot(slot.time)) return
  selectedTime.value = slot.time
  step.value = 3
}

function backToDate() { step.value = 1; selectedDate.value = null; slots.value = [] }
function backToTime() { step.value = 2; selectedTime.value = null }

const reservedAtIso = computed(() => {
  if (!selectedDate.value || !selectedTime.value) return null
  return `${selectedDate.value}T${selectedTime.value}:00`
})

async function submit() {
  if (!form.value.customerName.trim()) { ElMessage.warning('請填寫姓名'); return }
  if (!/^09\d{8}$/.test(form.value.customerPhone)) { ElMessage.warning('手機格式：09xxxxxxxx'); return }
  submitting.value = true
  try {
    await api.post('/api/reservations', {
      customerName:  form.value.customerName,
      customerPhone: form.value.customerPhone,
      partySize:     form.value.partySize,
      notes:         form.value.notes,
      reservedAt:    reservedAtIso.value,
    })
    step.value = 4
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '訂位失敗，請稍後再試')
  } finally {
    submitting.value = false
  }
}

function reset() {
  step.value = 1
  selectedDate.value = null
  selectedTime.value = null
  slots.value = []
  form.value = { customerName: '', customerPhone: '', partySize: 2, notes: '' }
}
</script>

<template>
  <div style="min-height:100vh; background:#f0f2f5; display:flex; align-items:center; justify-content:center; padding:20px">
    <el-card style="width:100%; max-width:520px">

      <!-- Header -->
      <template #header>
        <div style="text-align:center">
          <div style="font-size:22px; font-weight:700; margin-bottom:4px">🍹 線上訂位</div>
          <!-- 步驟指示 -->
          <el-steps :active="step - 1" finish-status="success" simple style="margin-top:12px">
            <el-step title="選日期" />
            <el-step title="選時段" />
            <el-step title="填資料" />
          </el-steps>
        </div>
      </template>

      <!-- Step 4: 成功 -->
      <div v-if="step === 4" style="text-align:center; padding:24px 0">
        <el-icon style="font-size:64px; color:#67c23a"><CircleCheck /></el-icon>
        <div style="font-size:20px; font-weight:700; margin:16px 0 8px">訂位成功！</div>
        <div style="color:#606266; line-height:2">
          {{ selectedDate }} {{ selectedTime }}<br>
          {{ form.customerName }} · {{ form.partySize }} 位<br>
          <span style="font-size:12px; color:#999">如需取消請提前來電，謝謝</span>
        </div>
        <el-button type="primary" style="margin-top:24px" @click="reset">再訂一筆</el-button>
      </div>

      <!-- Step 1: 選日期 -->
      <div v-else-if="step === 1">
        <div style="color:#606266; font-size:13px; margin-bottom:12px">請選擇用餐日期：</div>
        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="選擇日期"
          value-format="YYYY-MM-DD"
          :disabled-date="disabledDate"
          style="width:100%"
          @change="onDateSelect"
        />
        <div v-if="loadingSlots" style="text-align:center; margin-top:20px">
          <el-icon class="is-loading"><Loading /></el-icon> 載入時段中...
        </div>
      </div>

      <!-- Step 2: 選時段 -->
      <div v-else-if="step === 2">
        <div style="display:flex; align-items:center; margin-bottom:16px; gap:8px">
          <el-button link @click="backToDate">← 重選日期</el-button>
          <span style="color:#606266; font-size:14px">{{ selectedDate }} 可用時段</span>
        </div>
        <div style="display:flex; flex-wrap:wrap; gap:10px">
          <div v-for="slot in slots" :key="slot.time"
            :class="['slot-chip',
              isPastSlot(slot.time) ? 'slot-past' :
              !slot.available ? 'slot-full' : 'slot-ok']"
            @click="selectTime(slot)">
            <span>{{ slot.time }}</span>
            <span v-if="!slot.available && !isPastSlot(slot.time)" style="font-size:10px; display:block">已滿</span>
            <span v-else-if="isPastSlot(slot.time)" style="font-size:10px; display:block">已過</span>
          </div>
        </div>
      </div>

      <!-- Step 3: 填資料 -->
      <div v-else-if="step === 3">
        <div style="display:flex; align-items:center; margin-bottom:16px; gap:8px">
          <el-button link @click="backToTime">← 重選時段</el-button>
          <el-tag type="success">{{ selectedDate }} {{ selectedTime }}</el-tag>
        </div>
        <el-form label-width="80px">
          <el-form-item label="姓名">
            <el-input v-model="form.customerName" placeholder="訂位人姓名" maxlength="50" />
          </el-form-item>
          <el-form-item label="手機">
            <el-input v-model="form.customerPhone" placeholder="09xxxxxxxx" maxlength="10" />
          </el-form-item>
          <el-form-item label="用餐人數">
            <el-input-number v-model="form.partySize" :min="1" :max="50" style="width:100%" />
          </el-form-item>
          <el-form-item label="備註">
            <el-input v-model="form.notes" type="textarea" :rows="2"
              placeholder="過敏、特殊需求等（選填）" maxlength="200" show-word-limit />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" style="width:100%" :loading="submitting" @click="submit">
              確認訂位
            </el-button>
          </el-form-item>
        </el-form>
      </div>

    </el-card>
  </div>
</template>

<style scoped>
.slot-chip {
  width: 72px; padding: 8px 4px; border-radius: 8px;
  text-align: center; font-size: 14px; font-weight: 600;
  border: 2px solid transparent; cursor: pointer; transition: all .15s;
}
.slot-ok {
  border-color: #409eff; color: #409eff; background: #ecf5ff;
}
.slot-ok:hover { background: #409eff; color: #fff; }
.slot-full {
  border-color: #dcdfe6; color: #c0c4cc; background: #f5f7fa; cursor: not-allowed;
}
.slot-past {
  border-color: #e9ecef; color: #c0c4cc; background: #f5f7fa; cursor: not-allowed;
  opacity: 0.5;
}
</style>
