<script setup>
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { useWebSocket } from '@/composables/useWebSocket'
import api from '@/api/axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const { t, locale } = useI18n()

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

const dialogVisible = ref(false)
const selectedProduct = ref(null)
const quantity = ref(1)
const note = ref('')

function toggleLang() {
  locale.value = locale.value === 'zh-TW' ? 'en' : 'zh-TW'
  localStorage.setItem('lang', locale.value)
}

function catName(c) {
  return locale.value === 'en' ? (c.nameEn || c.nameZh) : c.nameZh
}
function productName(p) {
  return locale.value === 'en' ? (p.nameEn || p.nameZh) : p.nameZh
}

const filteredProducts = computed(() => {
  let list = products.value
  if (selectedCategory.value) list = list.filter(p => p.categoryId === selectedCategory.value)
  if (keyword.value) list = list.filter(p =>
    p.nameZh.includes(keyword.value) || (p.nameEn || '').toLowerCase().includes(keyword.value.toLowerCase())
  )
  return list
})

const { connect } = useWebSocket(`/topic/table/${token}/cart`, (data) => {
  cart.syncFromWs(data)
})

onMounted(async () => {
  try {
    localStorage.setItem('sessionToken', token)
    const s = await api.get(`/api/tables/sessions/${token}`)
    session.value = s.data.data
    cart.setSession(token)
    const [menuRes, catRes, cartRes] = await Promise.all([
      api.get('/api/menu'),
      api.get('/api/categories'),
      api.get('/api/cart'),
    ])
    products.value = menuRes.data.data || []
    categories.value = (catRes.data.data || []).sort((a, b) => a.displayOrder - b.displayOrder)
    if (categories.value.length) selectedCategory.value = categories.value[0].id
    cart.syncFromWs(cartRes.data.data || [])
    connect()
  } catch {
    invalid.value = true
  }
})

function openProductDialog(product) {
  selectedProduct.value = product
  quantity.value = 1
  note.value = ''
  dialogVisible.value = true
}

async function addToCart() {
  try {
    await cart.addItem({ productId: selectedProduct.value.id, quantity: quantity.value, notes: note.value })
    await cart.fetchCart()
    dialogVisible.value = false
    ElMessage({ message: t('order.added'), type: 'success' })
  } catch (e) {
    ElMessage.error(e.response?.data?.message || t('order.opFailed'))
  }
}

async function removeFromCart(itemKey) {
  try {
    await cart.removeItem(itemKey)
    await cart.fetchCart()
  } catch { ElMessage.error(t('order.removeFailed')) }
}

async function submitOrder() {
  if (cart.items.length === 0) return
  try {
    await ElMessageBox.confirm(t('order.confirmSubmit'), t('order.confirmTitle'), { type: 'info' })
    const items = cart.items.map(i => ({ productId: i.productId, quantity: i.quantity, notes: i.notes }))
    await api.post('/api/orders', { items })
    await cart.fetchCart()
    cartDrawer.value = false
    ElMessage.success(t('order.submitted'))
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || t('order.submitFailed'))
  }
}

function glassPath(type) {
  const paths = {
    DRINK: 'M6 4 L26 4 L24 12 Q16 20 8 12 Z M16 20 L16 28 M10 28 L22 28',
    FOOD:  'M8 6 L24 6 L23 22 L9 22 Z M6 22 L26 22',
  }
  return paths[type] || paths.DRINK
}
</script>

<template>
  <!-- Invalid session -->
  <div v-if="invalid" class="speak" style="min-height:100vh; display:flex; align-items:center; justify-content:center; flex-direction:column; gap:16px">
    <div class="mist"/><div class="grain"/><div class="vignette"/>
    <div style="position:relative; z-index:1; text-align:center">
      <div class="display" style="font-size:48px; margin-bottom:12px">×</div>
      <div class="num" style="font-size:11px; color:var(--fg-2); letter-spacing:0.3em">{{ t('order.invalid') }}</div>
      <div style="font-size:13px; color:var(--fg-2); margin-top:8px">{{ t('order.invalidSub') }}</div>
    </div>
  </div>

  <!-- Main order page -->
  <div v-else class="speak" style="min-height:100vh; position:relative;">
    <div class="mist"/><div class="grain"/><div class="vignette"/>

    <div style="position:relative; z-index:1; max-width:960px; margin:0 auto;">

      <!-- ── Header ── -->
      <div class="sp-header">
        <div style="display:inline-flex; align-items:center; gap:10px">
          <svg width="26" height="26" viewBox="0 0 32 32" class="logo-breath">
            <defs>
              <radialGradient id="moonG" cx="40%" cy="40%">
                <stop offset="0%" stop-color="#dfe9f7"/>
                <stop offset="60%" stop-color="#8eb6e8"/>
                <stop offset="100%" stop-color="#3d5e8e"/>
              </radialGradient>
            </defs>
            <circle cx="16" cy="16" r="11" fill="url(#moonG)" opacity="0.95"/>
            <circle cx="20" cy="13" r="9.5" fill="var(--bg-0)"/>
            <circle cx="14" cy="15" r="0.5" fill="#dfe9f7" opacity="0.6"/>
            <circle cx="12" cy="19" r="0.4" fill="#dfe9f7" opacity="0.5"/>
          </svg>
          <div style="line-height:1">
            <div class="display" style="font-size:22px; letter-spacing:0.06em;">
              Po<span style="font-style:italic; opacity:0.85">bar</span>
            </div>
            <div class="num" style="font-size:7px; color:var(--fg-2); letter-spacing:0.4em; margin-top:3px; text-transform:uppercase;">
              {{ t('order.tagline') }}
            </div>
          </div>
        </div>

        <div style="display:flex; align-items:center; gap:12px;">
          <!-- Language toggle -->
          <button class="sp-lang-btn" @click="toggleLang">
            {{ locale === 'zh-TW' ? 'EN' : '中' }}
          </button>
          <!-- Search -->
          <div class="sp-search-box">
            <svg width="12" height="12" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.2">
              <circle cx="7" cy="7" r="5"/><path d="M11 11 L14 14"/>
            </svg>
            <input v-model="keyword" :placeholder="t('order.search')" class="sp-search-input"/>
          </div>
          <!-- Cart -->
          <button class="sp-cart-btn" @click="cartDrawer = true">
            <svg width="15" height="15" viewBox="0 0 16 16" fill="none" stroke="var(--fg-0)" stroke-width="1">
              <path d="M3 4 L13 4 L11.5 11 L4.5 11 Z"/>
              <circle cx="5.5" cy="13" r="1"/><circle cx="10.5" cy="13" r="1"/>
            </svg>
            <span v-if="cart.totalCount > 0" class="sp-cart-badge num">{{ cart.totalCount }}</span>
          </button>
        </div>
      </div>

      <!-- ── Category strip ── -->
      <div class="sp-cat-strip">
        <div v-for="c in categories" :key="c.id"
          class="sp-cat-item" :class="{ active: selectedCategory === c.id }"
          @click="selectedCategory = c.id">{{ catName(c) }}</div>
      </div>

      <!-- ── Section label ── -->
      <div v-if="!keyword" class="sp-section-label">
        <div class="num" style="font-size:10px; color:var(--gold); letter-spacing:0.35em; margin-bottom:10px;">
          ✦ &nbsp; {{ t('order.sectionLabel').toUpperCase() }}
        </div>
        <div class="display-it" style="font-size:22px; color:var(--fg-0); letter-spacing:0.03em;">
          {{ selectedCategory ? catName(categories.find(c => c.id === selectedCategory) || {}) : t('order.tonightMenu') }}
        </div>
      </div>

      <!-- ── Item list ── -->
      <div v-for="p in filteredProducts" :key="p.id"
        class="sp-item-row sp-lift"
        :class="{ 'sp-item-unavailable': !p.isAvailable }"
        @click="p.isAvailable && openProductDialog(p)">
        <svg width="32" height="32" viewBox="0 0 32 32" fill="none" stroke="var(--moon-soft)" stroke-width="0.8">
          <path :d="glassPath(p.type)"/>
        </svg>
        <div style="min-width:0;">
          <div style="display:flex; align-items:baseline; gap:10px; flex-wrap:wrap;">
            <div class="display" style="font-size:clamp(16px,2.5vw,22px); color:var(--fg-0);">{{ productName(p) }}</div>
            <div v-if="!p.isAvailable" style="font-size:11px; color:var(--danger); font-family:var(--font-mono); letter-spacing:0.1em;">
              — {{ t('order.soldOut') }}
            </div>
          </div>
          <div v-if="p.ingredients?.length"
            style="font-size:12px; color:var(--fg-2); margin-top:4px; letter-spacing:0.03em;">
            {{ p.ingredients.join(' · ') }}
          </div>
        </div>
        <div class="num display" style="font-size:20px; color:var(--fg-0); text-align:right; white-space:nowrap;">
          NT$ {{ p.price }}
        </div>
      </div>

      <!-- Empty state -->
      <div v-if="filteredProducts.length === 0" style="padding:80px 40px; text-align:center;">
        <div class="display-it" style="font-size:24px; color:var(--fg-2);">{{ t('order.noItems') }}</div>
      </div>

      <div style="height:100px;"/>
    </div>

    <!-- ── Sticky cart footer ── -->
    <Transition name="footer-slide">
      <div v-if="cart.totalCount > 0" class="sp-cart-footer" @click="cartDrawer = true">
        <div>
          <div class="num" style="font-size:9px; color:var(--fg-2); letter-spacing:0.25em;">
            {{ t('order.items', { count: cart.totalCount }) }} · {{ t('order.tableNo', { id: session?.tableId || '' }) }}
          </div>
          <div class="num display" style="font-size:18px; color:var(--fg-0); margin-top:2px;">
            NT$ {{ cart.subtotal }}
          </div>
        </div>
        <div class="num" style="font-size:11px; color:var(--moon); letter-spacing:0.2em;">
          {{ t('order.viewOrder') }}
        </div>
      </div>
    </Transition>

    <!-- ── Item detail modal ── -->
    <Transition name="modal-fade">
      <div v-if="dialogVisible" class="sp-modal-overlay" @click.self="dialogVisible = false">
        <div class="sp-modal speak">
          <div class="mist" style="position:absolute; opacity:0.5;"/>
          <div style="position:relative; z-index:1; padding:28px 28px 24px;">
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:18px;">
              <div class="num" style="font-size:10px; color:var(--gold); letter-spacing:0.3em;">
                ✦ {{ t('order.detail').toUpperCase() }}
              </div>
              <button class="sp-close-btn" @click="dialogVisible = false">×</button>
            </div>

            <div v-if="selectedProduct?.imageUrl" style="width:100%; height:200px; border-radius:2px; overflow:hidden; margin-bottom:20px;">
              <img :src="selectedProduct.imageUrl" style="width:100%; height:100%; object-fit:cover;"/>
            </div>
            <div v-else class="sp-drink-placeholder" style="height:180px; margin-bottom:20px;">
              <div style="position:absolute; inset:0; background:radial-gradient(circle at 50% 60%, rgba(201,161,74,0.18), transparent 60%);"/>
              <div style="position:absolute; inset:0; background-image:repeating-linear-gradient(135deg, rgba(232,226,200,0.025) 0 1px, transparent 1px 12px);"/>
              <svg width="72" height="72" viewBox="0 0 32 32" fill="none" stroke="rgba(232,226,200,0.4)" stroke-width="0.8">
                <path :d="glassPath(selectedProduct?.type)"/>
              </svg>
            </div>

            <div class="display" style="font-size:38px; line-height:1.05; margin-bottom:4px;">
              {{ productName(selectedProduct) }}
            </div>
            <div v-if="locale === 'zh-TW' && selectedProduct?.nameEn" class="display-it" style="font-size:15px; color:var(--moon); margin-bottom:14px;">
              {{ selectedProduct?.nameEn }}
            </div>

            <div v-if="selectedProduct?.ingredients?.length" style="margin-bottom:18px;">
              <div class="sp-rule" style="margin-bottom:12px;">· {{ t('order.ingredients').toUpperCase() }} ·</div>
              <div style="display:flex; flex-wrap:wrap; gap:8px;">
                <div v-for="name in selectedProduct.ingredients" :key="name" class="sp-tag">{{ name }}</div>
              </div>
            </div>

            <div style="margin-bottom:20px;">
              <div class="num" style="font-size:9px; color:var(--fg-2); letter-spacing:0.25em; margin-bottom:6px;">
                {{ t('order.notes').toUpperCase() }}
              </div>
              <textarea v-model="note" :placeholder="t('order.notesPlaceholder')" class="sp-textarea" rows="2"/>
            </div>

            <div style="display:flex; align-items:center; justify-content:space-between; border-top:1px solid var(--line); padding-top:18px;">
              <div>
                <div class="num" style="font-size:9px; color:var(--fg-2); letter-spacing:0.2em; margin-bottom:8px;">
                  {{ t('order.quantity').toUpperCase() }}
                </div>
                <div style="display:flex; align-items:center; gap:16px;">
                  <button class="sp-qty-btn" @click="quantity = Math.max(1, quantity - 1)">−</button>
                  <div class="num display" style="font-size:20px;">{{ quantity }}</div>
                  <button class="sp-qty-btn" style="border-color:var(--moon); color:var(--moon);"
                    @click="quantity = Math.min(10, quantity + 1)">+</button>
                </div>
              </div>
              <button class="sp-btn" @click="addToCart">
                {{ t('order.addToCart', { price: (selectedProduct?.price || 0) * quantity }) }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <!-- ── Cart drawer ── -->
    <Transition name="drawer-slide">
      <div v-if="cartDrawer" class="sp-drawer-overlay" @click.self="cartDrawer = false">
        <div class="sp-drawer speak">
          <div class="mist" style="position:absolute; opacity:0.6;"/>
          <div style="position:relative; z-index:1; padding:28px 26px; height:100%; display:flex; flex-direction:column;">
            <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:4px;">
              <div class="display-it" style="font-size:32px;">{{ t('order.yourTab') }}</div>
              <button class="sp-close-btn" @click="cartDrawer = false">×</button>
            </div>
            <div class="num" style="font-size:10px; color:var(--fg-2); letter-spacing:0.25em; margin-bottom:24px;">
              {{ t('order.table', { id: session?.tableId || '—' }) }}
            </div>

            <div v-if="cart.items.length === 0" style="flex:1; display:flex; align-items:center; justify-content:center;">
              <div class="display-it" style="font-size:20px; color:var(--fg-2);">{{ t('order.emptyCart') }}</div>
            </div>

            <div v-else style="flex:1; overflow-y:auto;">
              <div v-for="item in cart.items" :key="item.key"
                style="display:grid; grid-template-columns:32px 1fr auto; gap:14px; padding:18px 0; border-bottom:1px solid var(--line); align-items:center;">
                <svg width="28" height="28" viewBox="0 0 32 32" fill="none" stroke="var(--moon-soft)" stroke-width="0.8">
                  <path :d="glassPath('DRINK')"/>
                </svg>
                <div>
                  <div class="display" style="font-size:17px;">{{ item.productName }}</div>
                  <div style="display:flex; align-items:center; gap:10px; margin-top:3px;">
                    <div class="num" style="font-size:11px; color:var(--fg-2);">× {{ item.quantity }}</div>
                    <div v-if="item.notes" class="display-it" style="font-size:11px; color:var(--gold);">— {{ item.notes }}</div>
                  </div>
                </div>
                <div style="display:flex; align-items:center; gap:12px;">
                  <div class="num display" style="font-size:17px;">{{ item.price * item.quantity }}</div>
                  <button @click="removeFromCart(item.key)" class="sp-remove-btn">×</button>
                </div>
              </div>

              <div style="margin-top:28px; padding-top:18px; border-top:1px solid var(--line-strong);">
                <div style="display:flex; justify-content:space-between; color:var(--fg-1); font-size:13px; margin-bottom:6px;">
                  <span>{{ t('order.subtotal') }}</span><span class="num">{{ cart.subtotal }}</span>
                </div>
                <div style="display:flex; justify-content:space-between; align-items:baseline; margin-top:14px;">
                  <div class="display-it" style="font-size:22px;">{{ t('order.total') }}</div>
                  <div class="num display" style="font-size:32px;">{{ cart.subtotal }}</div>
                </div>
              </div>
            </div>

            <div style="padding-top:20px;">
              <button class="sp-btn" style="width:100%; padding:16px; font-size:12px; letter-spacing:0.3em;"
                :disabled="cart.items.length === 0"
                @click="submitOrder">
                {{ t('order.submitOrder') }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.sp-header {
  position: sticky; top: 0; z-index: 20;
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 28px;
  border-bottom: 1px solid var(--line);
  background: linear-gradient(180deg, var(--bg-0) 80%, transparent);
  backdrop-filter: blur(12px);
}

.sp-lang-btn {
  background: transparent;
  border: 1px solid var(--line-strong);
  color: var(--fg-2);
  font-family: var(--font-mono);
  font-size: 10px; letter-spacing: 0.2em;
  padding: 5px 10px; cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}
.sp-lang-btn:hover { color: var(--moon); border-color: var(--moon); }

.sp-search-box {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 14px;
  border: 1px solid var(--line-strong);
  color: var(--fg-2);
  font-size: 12px; letter-spacing: 0.15em;
}
.sp-search-input {
  background: transparent; border: none; outline: none;
  color: var(--fg-0); font-family: var(--font-mono);
  font-size: 12px; letter-spacing: 0.1em; width: 110px;
}
.sp-search-input::placeholder { color: var(--fg-3); }

.sp-cart-btn {
  position: relative;
  width: 36px; height: 36px; border-radius: 50%;
  border: 1px solid var(--line-strong);
  background: transparent; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
}
.sp-cart-badge {
  position: absolute; top: -4px; right: -4px;
  width: 16px; height: 16px; border-radius: 50%;
  background: var(--gold); color: var(--bg-0);
  font-size: 9px; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
}

.sp-cat-strip {
  display: flex; gap: 28px;
  padding: 0 28px;
  border-bottom: 1px solid var(--line);
  overflow-x: auto; white-space: nowrap;
}
.sp-cat-item {
  font-family: var(--font-display);
  font-size: 16px; color: var(--fg-2);
  padding: 14px 0 12px;
  border-bottom: 1px solid transparent;
  cursor: pointer; letter-spacing: 0.03em;
  transition: color 0.2s, border-color 0.2s;
}
.sp-cat-item.active { font-style: italic; color: var(--fg-0); border-bottom-color: var(--moon); }
.sp-cat-item:hover:not(.active) { color: var(--fg-1); }

.sp-section-label { padding: 32px 28px 14px; }

.sp-item-row {
  display: grid;
  grid-template-columns: 36px 1fr auto;
  gap: 20px;
  padding: 20px 28px;
  border: 1px solid transparent;
  border-bottom-color: var(--line);
  align-items: center; cursor: pointer;
  transition: background 0.2s;
}
.sp-item-row:hover:not(.sp-item-unavailable) { background: rgba(142,182,232,0.04); }
.sp-item-unavailable { opacity: 0.45; cursor: not-allowed; }
@media (max-width: 500px) {
  .sp-item-row { grid-template-columns: 30px 1fr auto; gap: 12px; }
}

.sp-drink-placeholder {
  width: 100%; height: 200px;
  background: linear-gradient(135deg, #0e1a2d 0%, #1a2a42 50%, #0e1a2d 100%);
  border: 1px solid var(--line); border-radius: 2px;
  position: relative; overflow: hidden;
  display: flex; align-items: center; justify-content: center;
}

.sp-cart-footer {
  position: fixed; bottom: 0; left: 0; right: 0; z-index: 30;
  padding: 14px 28px;
  background: linear-gradient(180deg, transparent, var(--bg-0) 30%);
  border-top: 1px solid var(--line-strong);
  display: flex; align-items: center; justify-content: space-between;
  cursor: pointer; backdrop-filter: blur(8px);
}

.sp-modal-overlay {
  position: fixed; inset: 0; z-index: 100;
  background: rgba(7,16,29,0.85);
  display: flex; align-items: flex-end; justify-content: center;
}
@media (min-width: 640px) { .sp-modal-overlay { align-items: center; padding: 20px; } }
.sp-modal {
  position: relative; width: 100%; max-width: 480px;
  max-height: 90vh; overflow-y: auto;
  border-top: 1px solid var(--line-strong); border-radius: 2px 2px 0 0;
}
@media (min-width: 640px) { .sp-modal { border: 1px solid var(--line-strong); border-radius: 2px; } }

.sp-drawer-overlay {
  position: fixed; inset: 0; z-index: 100;
  background: rgba(7,16,29,0.75);
  display: flex; justify-content: flex-end;
}
.sp-drawer {
  position: relative; width: 100%; max-width: 400px; height: 100%;
  border-left: 1px solid var(--line-strong); overflow: hidden;
}

.sp-close-btn {
  width: 30px; height: 30px; border-radius: 50%;
  border: 1px solid var(--line-strong);
  background: transparent; color: var(--fg-1);
  font-size: 16px; cursor: pointer; display: grid; place-items: center;
}
.sp-qty-btn {
  width: 30px; height: 30px;
  border: 1px solid var(--line-strong);
  background: transparent; color: var(--fg-1);
  display: grid; place-items: center; cursor: pointer; font-size: 16px;
}
.sp-tag {
  padding: 5px 12px;
  border: 1px solid var(--line-strong);
  font-size: 11px; color: var(--fg-1); letter-spacing: 0.05em;
}
.sp-textarea {
  width: 100%; background: rgba(255,255,255,0.04);
  border: 1px solid var(--line-strong);
  color: var(--fg-0); font-family: var(--font-body);
  font-size: 13px; padding: 10px 12px;
  resize: none; outline: none; border-radius: 0;
}
.sp-textarea::placeholder { color: var(--fg-3); }
.sp-remove-btn {
  background: none; border: 1px solid var(--line-strong);
  color: var(--fg-1); cursor: pointer; font-size: 15px;
  width: 26px; height: 26px; border-radius: 50%;
  display: grid; place-items: center;
  transition: border-color 0.2s, color 0.2s;
}
.sp-remove-btn:hover { border-color: var(--danger); color: var(--danger); }

.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.25s; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
.drawer-slide-enter-active, .drawer-slide-leave-active { transition: transform 0.3s ease; }
.drawer-slide-enter-from, .drawer-slide-leave-to { transform: translateX(100%); }
.footer-slide-enter-active, .footer-slide-leave-active { transition: transform 0.3s ease; }
.footer-slide-enter-from, .footer-slide-leave-to { transform: translateY(100%); }
</style>
