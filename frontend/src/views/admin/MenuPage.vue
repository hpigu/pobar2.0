<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('products')

// ── 分類 ────────────────────────────────────
const categories = ref([])
const catDialog = ref(false)
const catForm = ref({ id: null, name: '', sortOrder: 0 })

async function loadCategories() {
  const res = await api.get('/api/categories')
  categories.value = res.data.data || []
}

function openCatDialog(row = null) {
  catForm.value = row ? { ...row } : { id: null, name: '', sortOrder: 0 }
  catDialog.value = true
}

async function saveCategory() {
  try {
    if (catForm.value.id) {
      await api.put(`/api/categories/${catForm.value.id}`, catForm.value)
    } else {
      await api.post('/api/categories', catForm.value)
    }
    ElMessage.success('儲存成功')
    catDialog.value = false
    loadCategories()
  } catch { ElMessage.error('儲存失敗') }
}

async function deleteCategory(id) {
  await ElMessageBox.confirm('確定刪除此分類？底下品項將失去分類。', '警告', { type: 'warning' })
  try {
    await api.delete(`/api/categories/${id}`)
    ElMessage.success('已刪除')
    loadCategories()
  } catch { ElMessage.error('刪除失敗') }
}

// ── 品項 ────────────────────────────────────
const products = ref([])
const productDialog = ref(false)
const productForm = ref({ id: null, nameZh: '', nameEn: '', categoryId: null, price: 0, type: 'DRINK', description: '' })
const uploadingId = ref(null)

async function loadProducts() {
  const res = await api.get('/api/menu')
  products.value = res.data.data || []
}

function openProductDialog(row = null) {
  productForm.value = row
    ? { id: row.id, nameZh: row.nameZh, nameEn: row.nameEn, categoryId: row.categoryId, price: row.price, type: row.type, description: row.description }
    : { id: null, nameZh: '', nameEn: '', categoryId: null, price: 0, type: 'DRINK', description: '' }
  productDialog.value = true
}

async function saveProduct() {
  try {
    if (productForm.value.id) {
      await api.put(`/api/menu/${productForm.value.id}`, productForm.value)
    } else {
      await api.post('/api/menu', productForm.value)
    }
    ElMessage.success('儲存成功')
    productDialog.value = false
    loadProducts()
  } catch { ElMessage.error('儲存失敗') }
}

async function toggleAvailability(row) {
  try {
    await api.put(`/api/menu/${row.id}/availability`, { available: !row.isAvailable })
    loadProducts()
  } catch { ElMessage.error('更新失敗') }
}

async function deleteProduct(id) {
  await ElMessageBox.confirm('確定刪除此品項？', '警告', { type: 'warning' })
  try {
    await api.delete(`/api/menu/${id}`)
    ElMessage.success('已刪除')
    loadProducts()
  } catch { ElMessage.error('刪除失敗') }
}

async function uploadImage(row, e) {
  const file = e.target.files[0]
  if (!file) return
  uploadingId.value = row.id
  const form = new FormData()
  form.append('file', file)
  try {
    await api.post(`/api/menu/${row.id}/image`, form, { headers: { 'Content-Type': 'multipart/form-data' } })
    ElMessage.success('圖片上傳成功')
    loadProducts()
  } catch { ElMessage.error('圖片上傳失敗') }
  finally { uploadingId.value = null }
}

function categoryName(id) {
  return categories.value.find(c => c.id === id)?.name || '-'
}

onMounted(() => { loadCategories(); loadProducts() })
</script>

<template>
  <div>
    <h2 style="margin-bottom:20px">品項管理</h2>
    <el-tabs v-model="activeTab">

      <!-- ── 品項 ── -->
      <el-tab-pane label="品項" name="products">
        <div style="margin-bottom:12px">
          <el-button type="primary" @click="openProductDialog()">新增品項</el-button>
        </div>
        <el-table :data="products" border>
          <el-table-column label="圖片" width="70">
            <template #default="{ row }">
              <el-image v-if="row.imageUrl" :src="row.imageUrl" style="width:40px;height:40px;border-radius:4px" fit="cover" />
              <span v-else style="color:#ccc">無</span>
            </template>
          </el-table-column>
          <el-table-column prop="nameZh" label="中文名稱" />
          <el-table-column prop="nameEn" label="英文名稱" />
          <el-table-column label="分類" width="100">
            <template #default="{ row }">{{ categoryName(row.categoryId) }}</template>
          </el-table-column>
          <el-table-column label="類型" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="row.type === 'DRINK' ? 'primary' : 'success'">
                {{ row.type === 'DRINK' ? '飲品' : '餐點' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="price" label="價格" width="80" />
          <el-table-column label="供應" width="80">
            <template #default="{ row }">
              <el-switch :model-value="!!row.isAvailable" @change="toggleAvailability(row)" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openProductDialog(row)">編輯</el-button>
              <label style="cursor:pointer">
                <el-button link type="success" size="small" :loading="uploadingId === row.id" tag="span">圖片</el-button>
                <input type="file" accept="image/*" style="display:none" @change="e => uploadImage(row, e)" />
              </label>
              <el-button link type="danger" size="small" @click="deleteProduct(row.id)">刪除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ── 分類 ── -->
      <el-tab-pane label="分類" name="categories">
        <div style="margin-bottom:12px">
          <el-button type="primary" @click="openCatDialog()">新增分類</el-button>
        </div>
        <el-table :data="categories" border>
          <el-table-column prop="name" label="名稱" />
          <el-table-column prop="sortOrder" label="排序" width="80" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openCatDialog(row)">編輯</el-button>
              <el-button link type="danger" size="small" @click="deleteCategory(row.id)">刪除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

    </el-tabs>

    <!-- 分類 Dialog -->
    <el-dialog v-model="catDialog" :title="catForm.id ? '編輯分類' : '新增分類'" width="360px">
      <el-form label-width="70px">
        <el-form-item label="名稱"><el-input v-model="catForm.name" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="catForm.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="catDialog = false">取消</el-button>
        <el-button type="primary" @click="saveCategory">儲存</el-button>
      </template>
    </el-dialog>

    <!-- 品項 Dialog -->
    <el-dialog v-model="productDialog" :title="productForm.id ? '編輯品項' : '新增品項'" width="480px">
      <el-form label-width="80px">
        <el-form-item label="中文名稱"><el-input v-model="productForm.nameZh" /></el-form-item>
        <el-form-item label="英文名稱"><el-input v-model="productForm.nameEn" /></el-form-item>
        <el-form-item label="分類">
          <el-select v-model="productForm.categoryId" placeholder="請選擇" style="width:100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="類型">
          <el-radio-group v-model="productForm.type">
            <el-radio label="DRINK">飲品</el-radio>
            <el-radio label="FOOD">餐點</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="價格"><el-input-number v-model="productForm.price" :min="0" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="productForm.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="productDialog = false">取消</el-button>
        <el-button type="primary" @click="saveProduct">儲存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
