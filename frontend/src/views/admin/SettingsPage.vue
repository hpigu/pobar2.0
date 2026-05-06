<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'
import { ElMessage } from 'element-plus'

const settings = ref({})   // { key: value }
const saving = ref(false)

// 設定分組與顯示定義
const groups = [
  {
    title: '營業時間',
    items: [
      { key: 'food_service_start',  label: '廚房開始時間',               hint: '格式 HH:mm，例如 17:00' },
      { key: 'food_service_end',    label: '廚房結束時間',               hint: '格式 HH:mm，例如 22:00' },
      { key: 'drink_service_start', label: '酒水開始時間',               hint: '格式 HH:mm，例如 17:00' },
      { key: 'drink_service_end',   label: '酒水結束時間',               hint: '可跨日，例如 02:00' },
      { key: 'business_day_reset_hour', label: '換日時間（凌晨幾點）',   hint: '帳務換日基準，預設 4' },
    ],
  },
  {
    title: '訂位設定',
    items: [
      { key: 'reservation_duration_minutes', label: '每桌用餐時長（分鐘）', hint: '預設 120，用於判斷時段是否衝突' },
      { key: 'no_show_cancel_minutes',       label: '未到場自動取消（分鐘）', hint: '超過預約時間幾分鐘後標記未到場，預設 10' },
    ],
  },
  {
    title: '結帳設定',
    items: [
      { key: 'service_charge_rate', label: '服務費率', hint: '小數表示，例如 0.10 = 10%' },
    ],
  },
  {
    title: '點餐設定',
    items: [
      { key: 'order_rate_limit_per_min', label: '每分鐘最多送單次數',    hint: '每個 session 的限流，預設 5' },
      { key: 'age_gate_enabled',         label: '顯示年齡確認彈窗',      hint: 'true / false', type: 'switch' },
    ],
  },
]

onMounted(async () => {
  const res = await api.get('/api/settings')
  const list = res.data.data || []
  list.forEach(s => { settings.value[s.settingKey] = s.settingValue })
})

async function save() {
  saving.value = true
  try {
    await api.put('/api/settings', { ...settings.value })
    ElMessage.success('設定已儲存')
  } catch { ElMessage.error('儲存失敗') }
  finally { saving.value = false }
}
</script>

<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px">
      <h2 style="margin:0">系統設定</h2>
      <el-button type="primary" :loading="saving" @click="save">儲存設定</el-button>
    </div>

    <div style="display:flex; flex-direction:column; gap:16px">
      <el-card v-for="group in groups" :key="group.title">
        <template #header>
          <span style="font-weight:600">{{ group.title }}</span>
        </template>
        <el-form label-width="200px" label-position="left">
          <el-form-item v-for="item in group.items" :key="item.key" :label="item.label">
            <template v-if="item.type === 'switch'">
              <el-switch
                :model-value="settings[item.key] === 'true'"
                @update:model-value="v => settings[item.key] = String(v)"
              />
            </template>
            <template v-else>
              <el-input v-model="settings[item.key]" style="max-width:200px" />
            </template>
            <span style="margin-left:10px; color:#909399; font-size:12px">{{ item.hint }}</span>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>
