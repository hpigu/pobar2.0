<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const tables = ref([])
const dialog = ref(false)
const form = ref({ id: null, name: '', capacity: 4, posX: 0, posY: 0 })
const qrDialog = ref(false)
const qrTable = ref(null)
const baseUrl = ref(window.location.origin)

async function load() {
  const res = await api.get('/api/tables')
  tables.value = res.data.data || []
}

function openDialog(row = null) {
  form.value = row
    ? { id: row.id, name: row.name, capacity: row.capacity, posX: row.posX ?? 0, posY: row.posY ?? 0 }
    : { id: null, name: '', capacity: 4, posX: 0, posY: 0 }
  dialog.value = true
}

async function save() {
  try {
    if (form.value.id) {
      await api.put(`/api/tables/${form.value.id}`, form.value)
    } else {
      await api.post('/api/tables', form.value)
    }
    ElMessage.success('儲存成功')
    dialog.value = false
    load()
  } catch { ElMessage.error('儲存失敗') }
}

async function remove(row) {
  await ElMessageBox.confirm(`確定刪除桌位「${row.name}」？`, '警告', { type: 'warning' })
  try {
    await api.delete(`/api/tables/${row.id}`)
    ElMessage.success('已刪除')
    load()
  } catch { ElMessage.error('刪除失敗') }
}

function showQr(row) {
  qrTable.value = row
  qrDialog.value = true
}

function qrUrl(row) {
  return `${baseUrl.value}/order/${row.qrToken}`
}

function printQr() {
  window.print()
}

onMounted(load)
</script>

<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px">
      <h2>桌位管理</h2>
      <el-button type="primary" @click="openDialog()">新增桌位</el-button>
    </div>

    <el-table :data="tables" border>
      <el-table-column prop="name" label="桌位名稱" />
      <el-table-column prop="capacity" label="容納人數" width="100" />
      <el-table-column label="狀態" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'OPEN' ? 'success' : 'info'" size="small">
            {{ row.status === 'OPEN' ? '使用中' : '空桌' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDialog(row)">編輯</el-button>
          <el-button link type="success" size="small" @click="showQr(row)">QR Code</el-button>
          <el-button link type="danger" size="small" :disabled="row.status === 'OPEN'" @click="remove(row)">刪除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/編輯 Dialog -->
    <el-dialog v-model="dialog" :title="form.id ? '編輯桌位' : '新增桌位'" width="380px">
      <el-form label-width="90px">
        <el-form-item label="桌位名稱"><el-input v-model="form.name" placeholder="例：A1、吧台1" /></el-form-item>
        <el-form-item label="容納人數"><el-input-number v-model="form.capacity" :min="1" :max="20" /></el-form-item>
        <el-form-item label="位置 X"><el-input-number v-model="form.posX" :min="0" /></el-form-item>
        <el-form-item label="位置 Y"><el-input-number v-model="form.posY" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">儲存</el-button>
      </template>
    </el-dialog>

    <!-- QR Code Dialog -->
    <el-dialog v-model="qrDialog" :title="`${qrTable?.name} QR Code`" width="340px" class="qr-dialog">
      <div v-if="qrTable" style="text-align:center; padding:16px">
        <div style="font-size:16px; font-weight:600; margin-bottom:12px">{{ qrTable.name }}</div>
        <img :src="`https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(qrUrl(qrTable))}`"
          style="width:220px; height:220px" alt="QR Code" />
        <div style="margin-top:12px; font-size:12px; color:#999; word-break:break-all">
          {{ qrUrl(qrTable) }}
        </div>
      </div>
      <template #footer>
        <el-button @click="qrDialog = false">關閉</el-button>
        <el-button type="primary" @click="printQr">列印</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style>
@media print {
  body > *:not(.el-overlay) { display: none; }
  .el-overlay { position: static; }
  .el-dialog__footer { display: none; }
}
</style>
