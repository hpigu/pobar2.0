<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import api from '@/api/axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const CATEGORIES = [
  { value: 'BASE_SPIRIT', label: '基酒' },
  { value: 'LIQUEUR',     label: '利口酒' },
  { value: 'WINE',        label: '葡萄酒' },
  { value: 'BEER',        label: '啤酒' },
  { value: 'SYRUP',       label: '糖漿/甜味劑' },
  { value: 'JUICE',       label: '果汁/飲料' },
  { value: 'FRESH',       label: '新鮮食材' },
  { value: 'GARNISH',     label: '配料/裝飾' },
  { value: 'OTHER',       label: '其他' },
]
const CATEGORY_LABEL = Object.fromEntries(CATEGORIES.map(c => [c.value, c.label]))

const ingredients = ref([])
const filterCategory = ref('ALL')
const dialog = ref(false)
const form = ref({ id: null, name: '', unit: '', category: 'BASE_SPIRIT' })

// 分頁
const currentPage = ref(1)
const pageSize = ref(20)

const filtered = computed(() =>
  filterCategory.value === 'ALL'
    ? ingredients.value
    : ingredients.value.filter(i => (i.category || 'OTHER') === filterCategory.value)
)

const paged = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filtered.value.slice(start, start + pageSize.value)
})

// 換分類或重 load 時回到第一頁
watch(filterCategory, () => { currentPage.value = 1 })
watch(filtered, () => {
  const max = Math.max(1, Math.ceil(filtered.value.length / pageSize.value))
  if (currentPage.value > max) currentPage.value = max
})

async function load() {
  const res = await api.get('/api/ingredients')
  ingredients.value = res.data.data || []
}

function openDialog(row = null) {
  form.value = row
    ? { id: row.id, name: row.name, unit: row.unit, category: row.category || 'OTHER' }
    : { id: null, name: '', unit: '', category: filterCategory.value === 'ALL' ? 'BASE_SPIRIT' : filterCategory.value }
  dialog.value = true
}

async function save() {
  if (!form.value.name?.trim()) { ElMessage.warning('請輸入名稱'); return }
  if (!form.value.unit?.trim()) { ElMessage.warning('請輸入單位'); return }
  if (!form.value.category) { ElMessage.warning('請選擇分類'); return }
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

    <div style="margin-bottom:12px; display:flex; align-items:center; gap:8px">
      <span style="font-size:13px; color:#606266">分類：</span>
      <el-select v-model="filterCategory" style="width:180px" size="default">
        <el-option label="全部" value="ALL" />
        <el-option v-for="c in CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
      </el-select>
      <span style="font-size:13px; color:#909399">共 {{ filtered.length }} 項</span>
    </div>

    <el-table :data="paged" border>
      <el-table-column prop="name" label="名稱" />
      <el-table-column label="分類" width="140">
        <template #default="{ row }">
          {{ CATEGORY_LABEL[row.category] || '其他' }}
        </template>
      </el-table-column>
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

    <el-pagination
      style="margin-top:16px; justify-content:flex-end"
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="filtered.length"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      background />

    <el-dialog v-model="dialog" :title="form.id ? '編輯食材' : '新增食材'" width="400px">
      <el-form label-width="80px">
        <el-form-item label="名稱"><el-input v-model="form.name" placeholder="例：琴酒、檸檬、冰塊" /></el-form-item>
        <el-form-item label="分類">
          <el-select v-model="form.category" style="width:100%">
            <el-option v-for="c in CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="單位"><el-input v-model="form.unit" placeholder="ml、g、顆…" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">儲存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
