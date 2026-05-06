<script setup>
import { ref } from 'vue'
import api from '@/api/axios'
import { ElMessage } from 'element-plus'

const form = ref({
  guestName: '',
  guestPhone: '',
  partySize: 2,
  reservedAt: null,
  note: '',
})
const submitted = ref(false)
const loading = ref(false)

// 只允許選未來 60 天、每半小時整點
function disabledDate(d) {
  const today = new Date(); today.setHours(0, 0, 0, 0)
  const limit = new Date(today); limit.setDate(limit.getDate() + 60)
  return d < today || d > limit
}

async function submit() {
  if (!form.value.guestName.trim()) { ElMessage.warning('請填寫姓名'); return }
  if (!/^09\d{8}$/.test(form.value.guestPhone)) { ElMessage.warning('手機格式：09xxxxxxxx'); return }
  if (!form.value.reservedAt) { ElMessage.warning('請選擇日期時間'); return }
  loading.value = true
  try {
    await api.post('/api/reservations', {
      ...form.value,
      reservedAt: new Date(form.value.reservedAt).toISOString().slice(0, 19),
    })
    submitted.value = true
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '訂位失敗，請稍後再試')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div style="min-height:100vh; background:#f0f2f5; display:flex; align-items:center; justify-content:center; padding:20px">
    <el-card style="width:100%; max-width:480px">
      <template #header>
        <div style="text-align:center">
          <div style="font-size:24px; font-weight:700; margin-bottom:4px">🍹 線上訂位</div>
          <div style="color:#909399; font-size:13px">請填寫以下資料，我們會為您保留座位</div>
        </div>
      </template>

      <!-- 成功畫面 -->
      <div v-if="submitted" style="text-align:center; padding:24px 0">
        <el-icon style="font-size:64px; color:#67c23a"><CircleCheck /></el-icon>
        <div style="font-size:20px; font-weight:700; margin:16px 0 8px">訂位成功！</div>
        <div style="color:#606266; line-height:1.8">
          感謝您的預約<br>
          我們將以簡訊確認您的訂位<br>
          如需取消請提前告知，謝謝
        </div>
        <el-button type="primary" style="margin-top:24px" @click="submitted = false; form = { guestName:'', guestPhone:'', partySize:2, reservedAt:null, note:'' }">
          再訂一位
        </el-button>
      </div>

      <!-- 訂位表單 -->
      <el-form v-else label-width="90px" style="margin-top:8px">
        <el-form-item label="姓名">
          <el-input v-model="form.guestName" placeholder="訂位人姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="手機">
          <el-input v-model="form.guestPhone" placeholder="09xxxxxxxx" maxlength="10" />
        </el-form-item>
        <el-form-item label="用餐人數">
          <el-input-number v-model="form.partySize" :min="1" :max="50" style="width:100%" />
        </el-form-item>
        <el-form-item label="日期時間">
          <el-date-picker
            v-model="form.reservedAt"
            type="datetime"
            placeholder="選擇日期與時間"
            format="YYYY/MM/DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :disabled-date="disabledDate"
            :minutes-step="30"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="備註">
          <el-input v-model="form.note" type="textarea" :rows="2"
            placeholder="過敏、特殊需求等（選填）" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width:100%" :loading="loading" @click="submit">
            確認訂位
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
