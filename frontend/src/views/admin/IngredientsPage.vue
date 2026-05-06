<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const ingredients = ref([])
const dialog = ref(false)
const form = ref({ id: null, name: '', unit: '', quantity: 0, lowStockThreshold: 0 })

async function load() {
  const res = await api.get('/api/ingredients')
  ingredients.value = res.data.data || []
}

function openDialog(row = null) {
  form.value = row
    ? { id: row.id, name: row.name, unit: row.unit, quantity: row.quantity, lowStockThreshold: row.lowStockThreshold }
    : { id: null, name: '', unit: '', quantity: 0, lowStockThreshold: 0 }
  dialog.value = true
}

async function save() {
  try {
    if (form.value.id) {
      await api.put(`/api/ingredients/${form.value.id}`, form.value)
    } else {
      await api.post('/api/ingredients', form.value)
    }
    ElMessage.success('儲存成功')
    dialog.value = false
    load()
  } catch { ElMessage.error('儲存失敗') }
}

async function toggleAvailable(row) {
  try {
    await api.patch(`/api/ingredients/${row.id}/availability`, { available: !row.isAvailable })
    load()
  } catch { ElMessage.error('更新失敗') }
}

async function remove(id) {
  await ElMessageBox.confirm('確定刪除此食材？相關品項將連動下架。', '警告', { type: 'warning' })
  try {
    await api.delete(`/api/ingredients/${id}`)
    ElMessage.success('已刪除')
    load()
  } catch { ElMessage.error('刪除失敗') }
}

onMounted(load)
</script>

<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px">
      <h2>食材管理</h2>
      <el-button type="primary" @click="openDialog()">新增食材</el-button>
    </div>

    <el-table :data="ingredients" border>
      <el-table-column prop="name" label="名稱" />
      <el-table-column prop="unit" label="單位" width="80" />
      <el-table-column prop="quantity" label="庫存" width="100" />
      <el-table-column prop="lowStockThreshold" label="低庫存警示" width="120" />
      <el-table-column label="供應" width="80">
        <template #default="{ row }">
          <el-switch :model-value="!!row.isAvailable" @change="toggleAvailable(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDialog(row)">編輯</el-button>
          <el-button link type="danger" size="small" @click="remove(row.id)">刪除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '編輯食材' : '新增食材'" width="400px">
      <el-form label-width="100px">
        <el-form-item label="名稱"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="單位"><el-input v-model="form.unit" placeholder="ml、g、顆…" /></el-form-item>
        <el-form-item label="庫存量"><el-input-number v-model="form.quantity" :min="0" :precision="1" /></el-form-item>
        <el-form-item label="低庫存警示"><el-input-number v-model="form.lowStockThreshold" :min="0" :precision="1" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">儲存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
