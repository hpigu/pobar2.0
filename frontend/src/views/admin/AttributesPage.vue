<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const types = ref([])

// 屬性類型
const typeDialog = ref(false)
const typeForm = ref({ id: null, name: '', isRequired: false, isMultiple: false })

// 屬性選項
const optionDialog = ref(false)
const optionForm = ref({ id: null, typeId: null, name: '', priceAdjust: 0 })

async function load() {
  const res = await api.get('/api/attributes')
  types.value = res.data.data || []
}

function openTypeDialog(row = null) {
  typeForm.value = row
    ? { id: row.id, name: row.name, isRequired: !!row.isRequired, isMultiple: !!row.isMultiple }
    : { id: null, name: '', isRequired: false, isMultiple: false }
  typeDialog.value = true
}

async function saveType() {
  try {
    if (typeForm.value.id) {
      await api.put(`/api/attributes/types/${typeForm.value.id}`, typeForm.value)
    } else {
      await api.post('/api/attributes/types', typeForm.value)
    }
    ElMessage.success('儲存成功')
    typeDialog.value = false
    load()
  } catch { ElMessage.error('儲存失敗') }
}

async function deleteType(id) {
  await ElMessageBox.confirm('確定刪除此屬性類型？底下所有選項也會一併刪除。', '警告', { type: 'warning' })
  try {
    await api.delete(`/api/attributes/types/${id}`)
    ElMessage.success('已刪除')
    load()
  } catch { ElMessage.error('刪除失敗') }
}

function openOptionDialog(typeId, row = null) {
  optionForm.value = row
    ? { id: row.id, typeId, name: row.name, priceAdjust: row.priceAdjust }
    : { id: null, typeId, name: '', priceAdjust: 0 }
  optionDialog.value = true
}

async function saveOption() {
  try {
    if (optionForm.value.id) {
      await api.put(`/api/attributes/options/${optionForm.value.id}`, optionForm.value)
    } else {
      await api.post(`/api/attributes/types/${optionForm.value.typeId}/options`, optionForm.value)
    }
    ElMessage.success('儲存成功')
    optionDialog.value = false
    load()
  } catch { ElMessage.error('儲存失敗') }
}

async function deleteOption(id) {
  await ElMessageBox.confirm('確定刪除此選項？', '警告', { type: 'warning' })
  try {
    await api.delete(`/api/attributes/options/${id}`)
    ElMessage.success('已刪除')
    load()
  } catch { ElMessage.error('刪除失敗') }
}

onMounted(load)
</script>

<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px">
      <h2>屬性管理</h2>
      <el-button type="primary" @click="openTypeDialog()">新增屬性類型</el-button>
    </div>

    <el-collapse>
      <el-collapse-item v-for="t in types" :key="t.id" :name="t.id">
        <template #title>
          <div style="display:flex; align-items:center; gap:12px; width:100%">
            <span style="font-weight:600">{{ t.name }}</span>
            <el-tag size="small" v-if="t.isRequired" type="danger">必選</el-tag>
            <el-tag size="small" v-if="t.isMultiple" type="info">多選</el-tag>
            <div style="margin-left:auto; display:flex; gap:8px" @click.stop>
              <el-button link type="primary" size="small" @click="openTypeDialog(t)">編輯</el-button>
              <el-button link type="success" size="small" @click="openOptionDialog(t.id)">新增選項</el-button>
              <el-button link type="danger" size="small" @click="deleteType(t.id)">刪除</el-button>
            </div>
          </div>
        </template>

        <el-table :data="t.options || []" size="small" border>
          <el-table-column prop="name" label="選項名稱" />
          <el-table-column label="加價" width="100">
            <template #default="{ row }">
              {{ row.priceAdjust > 0 ? `+NT$${row.priceAdjust}` : row.priceAdjust < 0 ? `-NT$${Math.abs(row.priceAdjust)}` : '不加價' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openOptionDialog(t.id, row)">編輯</el-button>
              <el-button link type="danger" size="small" @click="deleteOption(row.id)">刪除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-collapse-item>
    </el-collapse>

    <!-- 屬性類型 Dialog -->
    <el-dialog v-model="typeDialog" :title="typeForm.id ? '編輯屬性類型' : '新增屬性類型'" width="380px">
      <el-form label-width="80px">
        <el-form-item label="名稱"><el-input v-model="typeForm.name" placeholder="例：甜度、溫度" /></el-form-item>
        <el-form-item label="必選"><el-switch v-model="typeForm.isRequired" /></el-form-item>
        <el-form-item label="可多選"><el-switch v-model="typeForm.isMultiple" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialog = false">取消</el-button>
        <el-button type="primary" @click="saveType">儲存</el-button>
      </template>
    </el-dialog>

    <!-- 屬性選項 Dialog -->
    <el-dialog v-model="optionDialog" :title="optionForm.id ? '編輯選項' : '新增選項'" width="360px">
      <el-form label-width="80px">
        <el-form-item label="名稱"><el-input v-model="optionForm.name" placeholder="例：全糖、去冰" /></el-form-item>
        <el-form-item label="加價">
          <el-input-number v-model="optionForm.priceAdjust" />
          <span style="margin-left:8px; color:#999; font-size:12px">可為負數</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="optionDialog = false">取消</el-button>
        <el-button type="primary" @click="saveOption">儲存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
