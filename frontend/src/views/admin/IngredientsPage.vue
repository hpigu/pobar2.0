<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const ingredients = ref([])
const dialog = ref(false)
const form = ref({ id: null, name: '', unit: '' })

async function load() {
  const res = await api.get('/api/ingredients')
  ingredients.value = res.data.data || []
}

function openDialog(row = null) {
  form.value = row
    ? { id: row.id, name: row.name, unit: row.unit }
    : { id: null, name: '', unit: '' }
  dialog.value = true
}

async function save() {
  if (!form.value.name?.trim()) { ElMessage.warning('請輸入名稱'); return }
  if (!form.value.unit?.trim()) { ElMessage.warning('請輸入單位'); return }
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
      <h2 style="margin:0">食材管理</h2>
      <el-button type="primary" @click="openDialog()">新增食材</el-button>
    </div>
    <div style="color:#909399; font-size:13px; margin-bottom:12px">
      只追蹤「有 / 無供應」狀態，缺貨時關閉開關即可，相關酒品會自動下架
    </div>

    <el-table :data="ingredients" border>
      <el-table-column prop="name" label="名稱" />
      <el-table-column prop="unit" label="單位" width="100" />
      <el-table-column label="供應狀態" width="120">
        <template #default="{ row }">
          <el-switch :model-value="!!row.isAvailable" @change="toggleAvailable(row)" />
          <span style="margin-left:8px; font-size:12px"
            :style="{ color: row.isAvailable ? '#67c23a' : '#f56c6c' }">
            {{ row.isAvailable ? '供應中' : '缺貨' }}
          </span>
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
      <el-form label-width="80px">
        <el-form-item label="名稱"><el-input v-model="form.name" placeholder="例：琴酒、檸檬、冰塊" /></el-form-item>
        <el-form-item label="單位"><el-input v-model="form.unit" placeholder="ml、g、顆…" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">儲存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
