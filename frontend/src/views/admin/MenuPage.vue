<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import api from '@/api/axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('products')

// ── 分類 ────────────────────────────────────
const categories = ref([])
const catDialog = ref(false)
const catForm = ref({ id: null, nameZh: '', nameEn: '', displayOrder: 0 })

async function loadCategories() {
  const res = await api.get('/api/categories')
  categories.value = res.data.data || []
}

function openCatDialog(row = null) {
  catForm.value = row
    ? { id: row.id, nameZh: row.nameZh, nameEn: row.nameEn, displayOrder: row.displayOrder }
    : { id: null, nameZh: '', nameEn: '', displayOrder: 0 }
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
const productForm = ref({ id: null, nameZh: '', nameEn: '', categoryId: null, price: 0, type: 'DRINK', availableFrom: null, availableTo: null })
const uploadingId = ref(null)

// 品項分頁
const productPage = ref(1)
const productPageSize = ref(20)
const pagedProducts = computed(() => {
  const start = (productPage.value - 1) * productPageSize.value
  return products.value.slice(start, start + productPageSize.value)
})
watch(products, () => {
  const max = Math.max(1, Math.ceil(products.value.length / productPageSize.value))
  if (productPage.value > max) productPage.value = max
})

// 分類分頁
const catPage = ref(1)
const catPageSize = ref(20)
const pagedCategories = computed(() => {
  const start = (catPage.value - 1) * catPageSize.value
  return categories.value.slice(start, start + catPageSize.value)
})
watch(categories, () => {
  const max = Math.max(1, Math.ceil(categories.value.length / catPageSize.value))
  if (catPage.value > max) catPage.value = max
})

async function loadProducts() {
  const res = await api.get('/api/menu')
  products.value = res.data.data || []
}

function openProductDialog(row = null) {
  productForm.value = row
    ? { id: row.id, nameZh: row.nameZh, nameEn: row.nameEn, categoryId: row.categoryId, price: row.price, type: row.type, availableFrom: row.availableFrom || null, availableTo: row.availableTo || null }
    : { id: null, nameZh: '', nameEn: '', categoryId: null, price: 0, type: 'DRINK', availableFrom: null, availableTo: null }
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
    await api.put(`/api/menu/${row.id}/availability?available=${row.isAvailable ? 'false' : 'true'}`)
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
  return categories.value.find(c => c.id === id)?.nameZh || '-'
}

// ── 酒譜 ────────────────────────────────────
const recipeDialog = ref(false)
const recipeProductId = ref(null)
const recipeForm = ref({ preparationNotes: '', ingredients: [] })
const allIngredients = ref([])

async function openRecipeDialog(row) {
  recipeProductId.value = row.id
  allIngredients.value = []
  recipeForm.value = { preparationNotes: '', ingredients: [] }
  recipeDialog.value = true
  const [detailRes, ingRes] = await Promise.all([
    api.get(`/api/menu/${row.id}/recipe-detail`),
    api.get('/api/ingredients'),
  ])
  const detail = detailRes.data.data
  recipeForm.value = {
    preparationNotes: detail.preparationNotes || '',
    ingredients: (detail.ingredients || []).map(i => ({
      ingredientId: i.ingredientId,
      ingredientName: i.ingredientName,
      quantity: i.quantity,
      unit: i.unit,
      displayOrder: i.displayOrder,
    })),
  }
  allIngredients.value = ingRes.data.data || []
}

function addIngredientLine() {
  recipeForm.value.ingredients.push({ ingredientId: null, ingredientName: '', quantity: 0, unit: 'ml', displayOrder: recipeForm.value.ingredients.length })
}

function removeIngredientLine(index) {
  recipeForm.value.ingredients.splice(index, 1)
}

async function saveRecipe() {
  const lines = recipeForm.value.ingredients.filter(i => i.ingredientId)
  if (!lines.length) { ElMessage.warning('請至少加入一項食材'); return }
  try {
    await api.post(`/api/menu/${recipeProductId.value}/recipe`, {
      preparationNotes: recipeForm.value.preparationNotes,
      ingredients: lines.map((i, idx) => ({
        ingredientId: i.ingredientId,
        quantity: i.quantity,
        unit: i.unit,
        displayOrder: idx,
      })),
    })
    ElMessage.success('酒譜儲存成功')
    recipeDialog.value = false
  } catch { ElMessage.error('酒譜儲存失敗') }
}

onMounted(() => { loadCategories(); loadProducts() })
</script>

<template>
  <div>
    <h2 style="margin:0 0 20px">品項管理</h2>
    <el-tabs v-model="activeTab">

      <!-- ── 品項 ── -->
      <el-tab-pane label="品項" name="products">
        <div style="margin-bottom:12px">
          <el-button type="primary" @click="openProductDialog()">新增品項</el-button>
        </div>
        <el-table :data="pagedProducts" border>
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
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openProductDialog(row)">編輯</el-button>
              <el-button v-if="row.type === 'DRINK'" link type="warning" size="small" @click="openRecipeDialog(row)">酒譜</el-button>
              <label style="cursor:pointer">
                <el-button link type="success" size="small" :loading="uploadingId === row.id" tag="span">圖片</el-button>
                <input type="file" accept="image/*" style="display:none" @change="e => uploadImage(row, e)" />
              </label>
              <el-button link type="danger" size="small" @click="deleteProduct(row.id)">刪除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          style="margin-top:16px; justify-content:flex-end"
          v-model:current-page="productPage"
          v-model:page-size="productPageSize"
          :total="products.length"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background />
      </el-tab-pane>

      <!-- ── 分類 ── -->
      <el-tab-pane label="分類" name="categories">
        <div style="margin-bottom:12px">
          <el-button type="primary" @click="openCatDialog()">新增分類</el-button>
        </div>
        <el-table :data="pagedCategories" border>
          <el-table-column prop="nameZh" label="中文名稱" />
          <el-table-column prop="nameEn" label="英文名稱" />
          <el-table-column prop="displayOrder" label="排序" width="80" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openCatDialog(row)">編輯</el-button>
              <el-button link type="danger" size="small" @click="deleteCategory(row.id)">刪除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          style="margin-top:16px; justify-content:flex-end"
          v-model:current-page="catPage"
          v-model:page-size="catPageSize"
          :total="categories.length"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background />
      </el-tab-pane>

    </el-tabs>

    <!-- 分類 Dialog -->
    <el-dialog v-model="catDialog" :title="catForm.id ? '編輯分類' : '新增分類'" width="360px">
      <el-form label-width="70px">
        <el-form-item label="中文名稱"><el-input v-model="catForm.nameZh" /></el-form-item>
        <el-form-item label="英文名稱"><el-input v-model="catForm.nameEn" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="catForm.displayOrder" :min="0" /></el-form-item>
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
            <el-option v-for="c in categories" :key="c.id" :label="c.nameZh" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="類型">
          <el-radio-group v-model="productForm.type">
            <el-radio label="DRINK">飲品</el-radio>
            <el-radio label="FOOD">餐點</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="價格"><el-input-number v-model="productForm.price" :min="0" /></el-form-item>
        <el-form-item label="供應起始"><el-date-picker v-model="productForm.availableFrom" type="datetime" placeholder="不限" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" /></el-form-item>
        <el-form-item label="供應截止"><el-date-picker v-model="productForm.availableTo" type="datetime" placeholder="不限" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="productDialog = false">取消</el-button>
        <el-button type="primary" @click="saveProduct">儲存</el-button>
      </template>
    </el-dialog>

    <!-- 酒譜 Dialog -->
    <el-dialog v-model="recipeDialog" title="酒譜管理" width="600px">
      <el-form label-width="80px">
        <el-form-item label="作法說明">
          <el-input v-model="recipeForm.preparationNotes" type="textarea" :rows="2" placeholder="搖盪法、攪拌法…" />
        </el-form-item>
      </el-form>
      <div style="display:flex; justify-content:space-between; align-items:center; margin:12px 0 8px">
        <span style="font-weight:600">食材列表</span>
        <el-button size="small" @click="addIngredientLine">+ 新增食材</el-button>
      </div>
      <el-table :data="recipeForm.ingredients" border size="small">
        <el-table-column label="食材" min-width="160">
          <template #default="{ row }">
            <el-select v-model="row.ingredientId" placeholder="請選擇" size="small" style="width:100%"
              @change="id => { const ing = allIngredients.find(i => i.id === id); if(ing) row.unit = ing.unit }">
              <el-option v-for="i in allIngredients" :key="i.id" :label="i.name" :value="i.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="用量" width="100">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="0" :precision="1" size="small" style="width:100%" />
          </template>
        </el-table-column>
        <el-table-column label="單位" width="90">
          <template #default="{ row }">
            <el-input v-model="row.unit" size="small" />
          </template>
        </el-table-column>
        <el-table-column width="60">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="removeIngredientLine($index)">刪</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="recipeDialog = false">取消</el-button>
        <el-button type="primary" @click="saveRecipe">儲存酒譜</el-button>
      </template>
    </el-dialog>
  </div>
</template>
