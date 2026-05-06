<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'
import { ElMessage } from 'element-plus'

const settings = ref([])
const saving = ref(false)

const labelMap = {
  service_charge_rate: '服務費率（如 0.10 = 10%）',
  frontend_base_url: '前端網址（QR code 用）',
  reservation_slot_minutes: '訂位時段長度（分鐘）',
  business_day_start_hour: '業務日起始小時（預設 4）',
}

onMounted(async () => {
  const res = await api.get('/api/settings')
  settings.value = res.data.data || []
})

async function save() {
  saving.value = true
  const updates = {}
  settings.value.forEach(s => { updates[s.settingKey] = s.settingValue })
  try {
    await api.put('/api/settings', updates)
    ElMessage.success('設定已儲存')
  } catch { ElMessage.error('儲存失敗') }
  finally { saving.value = false }
}
</script>

<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px">
      <h2>系統設定</h2>
      <el-button type="primary" :loading="saving" @click="save">儲存設定</el-button>
    </div>
    <el-card>
      <el-form label-width="240px" label-position="left">
        <el-form-item v-for="s in settings" :key="s.settingKey"
          :label="labelMap[s.settingKey] || s.settingKey">
          <el-input v-model="s.settingValue" style="max-width:300px" />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
