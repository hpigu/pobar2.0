<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { useWebSocket } from '@/composables/useWebSocket'
import api from '@/api/axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const cart = useCartStore()
const token = route.params.token

const session = ref(null)
const invalid = ref(false)
const categories = ref([])
const products = ref([])
const selectedCategory = ref(null)
const keyword = ref('')
const cartDrawer = ref(false)

// 品項 dialog
const dialogVisible = ref(false)
const selectedProduct = ref(null)
const quantity = ref(1)
const note = ref('')

const filteredProducts = computed(() => {
  let list = products.value
  if (selectedCategory.value) list = list.filter(p => p.categoryId === selectedCategory.value)
  if (keyword.value) list = list.filter(p => p.name.includes(keyword.value))
  return list
})

// WebSocket 購物車同步
const { connect } = useWebSocket(`/topic/table/${token}/cart`, (data) => {
  cart.syncFromWs(data)
})

onMounted(async () => {
  try {
    const s = await api.get(`/api/tables/sessions/${token}`)
    session.value = s.data.data
    cart.setSession(token)
    const [menuRes, cartRes] = await Promise.all([
      api.get('/api/menu'),
      api.get(`/api/cart/${token}`),
    ])
    products.value = menuRes.data.data || []
    const catMap = {}
    products.value.forEach(p => {
      if (p.categoryId) catMap[p.categoryId] = { id: p.categoryId, name: p.categoryName }
    })
    categories.value = Object.values(catMap)
    cart.syncFromWs(cartRes.data.data || [])
    connect()
  } catch {
    invalid.value = true
  }
})

function openProductDialog(product) {
  selectedProduct.value = product
  selectedOptions.value = {}
  quantity.value = 1
  note.value = ''
  dialogVisible.value = true
}

async function addToCart() {
  try {
    await cart.addItem({
      productId: selectedProduct.value.id,
      quantity: quantity.value,
      notes: note.value,
    })
    dialogVisible.value = false
    ElMessage.success('已加入購物車')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失敗')
  }
}

async function removeFromCart(itemKey) {
  try {
    await cart.removeItem(itemKey)
  } catch (e) {
    ElMessage.error('移除失敗')
  }
}

async function submitOrder() {
  if (cart.items.length === 0) return
  try {
    await ElMessageBox.confirm('確認送出訂單？', '送出訂單', { type: 'info' })
    const items = cart.items.map(i => ({
      productId: i.productId,
      quantity: i.quantity,
      notes: i.notes,
    }))
    await api.post(`/api/orders?token=${token}`, { items })
    // 後端送單後不會自動清購物車，前端主動清
    await cart.fetchCart()
    cartDrawer.value = false
    ElMessage.success('訂單已送出！')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '送出失敗')
  }
}
</script>

<template>
  <div v-if="invalid" style="text-align:center; padding:80px 20px">
    <el-empty description="QR Code 無效或已過期，請重新掃描" />
  </div>

  <div v-else class="order-page">
    <!-- Header -->
    <div class="order-header">
      <span class="logo">🍹 Pobar</span>
      <el-input v-model="keyword" placeholder="搜尋品項" prefix-icon="Search"
        style="width:180px" clearable />
      <el-badge :value="cart.totalCount" :hidden="cart.totalCount === 0">
        <el-button circle icon="ShoppingCart" size="large" @click="cartDrawer = true" />
      </el-badge>
    </div>

    <!-- 分類 tabs -->
    <div class="category-tabs">
      <el-tag :type="selectedCategory === null ? '' : 'info'"
        class="cat-tag" @click="selectedCategory = null">全部</el-tag>
      <el-tag v-for="c in categories" :key="c.id"
        :type="selectedCategory === c.id ? '' : 'info'"
        class="cat-tag" @click="selectedCategory = c.id">{{ c.name }}</el-tag>
    </div>

    <!-- 品項列表 -->
    <div class="product-grid">
      <el-card v-for="p in filteredProducts" :key="p.id"
        class="product-card" :class="{ disabled: !p.isAvailable }"
        @click="p.isAvailable && openProductDialog(p)">
        <img v-if="p.imageUrl" :src="p.imageUrl" class="product-img" />
        <div v-else class="product-img-placeholder">🍸</div>
        <div class="product-name">{{ p.name }}</div>
        <div class="product-price">NT$ {{ p.price }}</div>
        <el-tag v-if="!p.isAvailable" type="danger" size="small">已售完</el-tag>
      </el-card>
    </div>

    <!-- 品項選項 dialog -->
    <el-dialog v-model="dialogVisible" :title="selectedProduct?.name" width="90%" max-width="480px">
      <img v-if="selectedProduct?.imageUrl" :src="selectedProduct.imageUrl"
        style="width:100%; border-radius:8px; margin-bottom:16px" />
      <div style="font-size:18px; color:#f56c6c; margin-bottom:16px">
        NT$ {{ selectedProduct?.price }}
      </div>

<el-form-item label="備註">
        <el-input v-model="note" placeholder="過敏原、特殊需求..." type="textarea" :rows="2" />
      </el-form-item>

      <div style="display:flex; align-items:center; gap:12px; margin-top:12px">
        <span>數量：</span>
        <el-input-number v-model="quantity" :min="1" :max="10" />
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addToCart">
          加入購物車 NT$ {{ (selectedProduct?.price || 0) * quantity }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 購物車 drawer -->
    <el-drawer v-model="cartDrawer" title="購物車" direction="rtl" size="85%">
      <div v-if="cart.items.length === 0" style="text-align:center; padding:40px">
        <el-empty description="購物車是空的" />
      </div>
      <div v-else>
        <el-table :data="cart.items" style="width:100%">
          <el-table-column prop="productName" label="品項" />
          <el-table-column prop="quantity" label="數量" width="70" />
          <el-table-column label="小計" width="90">
            <template #default="{ row }">NT$ {{ row.price * row.quantity }}</template>
          </el-table-column>
          <el-table-column width="60">
            <template #default="{ row }">
              <el-button link type="danger" icon="Delete" @click="removeFromCart(row.key)" />
            </template>
          </el-table-column>
        </el-table>
        <div style="text-align:right; font-size:18px; padding:16px 0; font-weight:700">
          小計：NT$ {{ cart.subtotal }}
        </div>
        <el-button type="primary" size="large" style="width:100%" @click="submitOrder">
          送出訂單
        </el-button>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.order-page { max-width: 600px; margin: 0 auto; padding-bottom: 80px; }
.order-header {
  position: sticky; top: 0; z-index: 10;
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,.1);
}
.logo { font-size: 20px; font-weight: 700; }
.category-tabs { display: flex; gap: 8px; padding: 12px 16px; overflow-x: auto; }
.cat-tag { cursor: pointer; white-space: nowrap; }
.product-grid {
  display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; padding: 0 12px;
}
.product-card { cursor: pointer; text-align: center; }
.product-card.disabled { opacity: .5; cursor: not-allowed; }
.product-img { width: 100%; height: 120px; object-fit: cover; border-radius: 4px; }
.product-img-placeholder {
  width: 100%; height: 120px; display: flex; align-items: center; justify-content: center;
  font-size: 48px; background: #f5f5f5; border-radius: 4px;
}
.product-name { font-weight: 600; margin-top: 8px; }
.product-price { color: #f56c6c; font-size: 15px; }
</style>
