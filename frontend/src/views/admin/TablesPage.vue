<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import api from '@/api/axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const tables = ref([])
const dialog = ref(false)
const form = ref({ id: null, name: '', capacity: 4 })
const qrDialog = ref(false)
const qrTable = ref(null)

// ── 拖曳 ────────────────────────────────────
const FLOOR_W = 800
const FLOOR_H = 500
const CARD_W = 90
const CARD_H = 60
const dragging = ref(null)   // { tableId, offsetX, offsetY }
const positions = ref({})    // { [tableId]: { x, y } }
const saveTimer = {}

function onMouseDown(e, table) {
  if (e.button !== 0) return
  const el = e.currentTarget
  const rect = el.getBoundingClientRect()
  dragging.value = {
    tableId: table.id,
    offsetX: e.clientX - rect.left,
    offsetY: e.clientY - rect.top,
  }
  e.preventDefault()
}

function onMouseMove(e) {
  if (!dragging.value) return
  const floor = document.getElementById('floor-plan')
  const floorRect = floor.getBoundingClientRect()
  const x = Math.max(0, Math.min(FLOOR_W - CARD_W, e.clientX - floorRect.left - dragging.value.offsetX))
  const y = Math.max(0, Math.min(FLOOR_H - CARD_H, e.clientY - floorRect.top - dragging.value.offsetY))
  positions.value[dragging.value.tableId] = { x, y }
}

function onMouseUp() {
  if (!dragging.value) return
  const id = dragging.value.tableId
  dragging.value = null
  // 防抖：停止拖曳 600ms 後才存到後端
  clearTimeout(saveTimer[id])
  saveTimer[id] = setTimeout(() => savePosition(id), 600)
}

async function savePosition(id) {
  const pos = positions.value[id]
  if (!pos) return
  try {
    const table = tables.value.find(t => t.id === id)
    await api.put(`/api/tables/${id}`, { ...table, posX: Math.round(pos.x), posY: Math.round(pos.y) })
  } catch { ElMessage.error('位置儲存失敗') }
}

// ── 桌位 CRUD ────────────────────────────────
async function load() {
  const res = await api.get('/api/tables')
  tables.value = res.data.data || []
  // 初始化位置
  tables.value.forEach(t => {
    if (!positions.value[t.id]) {
      positions.value[t.id] = { x: t.posX ?? 0, y: t.posY ?? 0 }
    }
  })
}

function openDialog(row = null) {
  form.value = row
    ? { id: row.id, name: row.name, capacity: row.capacity }
    : { id: null, name: '', capacity: 4 }
  dialog.value = true
}

async function save() {
  try {
    const pos = form.value.id ? positions.value[form.value.id] : { x: 20, y: 20 }
    const payload = { ...form.value, posX: Math.round(pos?.x ?? 20), posY: Math.round(pos?.y ?? 20) }
    if (form.value.id) {
      await api.put(`/api/tables/${form.value.id}`, payload)
    } else {
      await api.post('/api/tables', payload)
    }
    ElMessage.success('儲存成功')
    dialog.value = false
    load()
  } catch { ElMessage.error('儲存失敗') }
}

async function remove(table) {
  await ElMessageBox.confirm(`確定刪除桌位「${table.name}」？`, '警告', { type: 'warning' })
  try {
    await api.delete(`/api/tables/${table.id}`)
    ElMessage.success('已刪除')
    load()
  } catch { ElMessage.error('刪除失敗') }
}

function showQr(table) {
  qrTable.value = table
  qrDialog.value = true
}

function qrUrl(table) {
  return `${window.location.origin}/order/${table.qrToken}`
}

onMounted(() => {
  load()
  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('mouseup', onMouseUp)
})

onUnmounted(() => {
  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('mouseup', onMouseUp)
})
</script>

<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:16px">
      <div>
        <h2 style="margin:0 0 4px">桌位管理</h2>
        <span style="color:#999; font-size:13px">拖曳桌位調整位置，位置會自動儲存</span>
      </div>
      <el-button type="primary" @click="openDialog()">新增桌位</el-button>
    </div>

    <!-- 平面圖 -->
    <div id="floor-plan"
      :style="`width:${FLOOR_W}px; height:${FLOOR_H}px; position:relative; background:#f8f9fa;
               border:2px dashed #dee2e6; border-radius:8px; user-select:none; overflow:hidden`">

      <!-- 格線 -->
      <svg :width="FLOOR_W" :height="FLOOR_H" style="position:absolute; inset:0; pointer-events:none">
        <defs>
          <pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse">
            <path d="M 40 0 L 0 0 0 40" fill="none" stroke="#e9ecef" stroke-width="1"/>
          </pattern>
        </defs>
        <rect width="100%" height="100%" fill="url(#grid)" />
      </svg>

      <!-- 桌位卡片 -->
      <div v-for="table in tables" :key="table.id"
        :style="`position:absolute; left:${positions[table.id]?.x ?? 0}px; top:${positions[table.id]?.y ?? 0}px;
                 width:${CARD_W}px; height:${CARD_H}px; cursor:${dragging?.tableId === table.id ? 'grabbing' : 'grab'};
                 transition: box-shadow .15s`"
        @mousedown="onMouseDown($event, table)">
        <el-card shadow="hover"
          :style="`height:100%; border:2px solid ${table.status === 'OPEN' ? '#67c23a' : '#dcdfe6'};
                   background:${table.status === 'OPEN' ? '#f0f9eb' : '#fff'}`"
          :body-style="{ padding: '6px', height: '100%', boxSizing: 'border-box' }">
          <div style="font-weight:700; font-size:13px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis">
            {{ table.name }}
          </div>
          <div style="font-size:11px; color:#909399">{{ table.capacity }}人</div>
          <div style="display:flex; gap:4px; margin-top:2px" @mousedown.stop>
            <el-button link size="small" style="font-size:11px; padding:0" @click="openDialog(table)">編輯</el-button>
            <el-button link size="small" style="font-size:11px; padding:0; color:#67c23a" @click="showQr(table)">QR</el-button>
            <el-button link size="small" style="font-size:11px; padding:0; color:#f56c6c"
              :disabled="table.status === 'OPEN'" @click="remove(table)">刪</el-button>
          </div>
        </el-card>
      </div>
    </div>

    <div style="margin-top:8px; color:#999; font-size:12px">
      綠色框 = 使用中　｜　灰色框 = 空桌　｜　拖曳調整位置後會自動儲存
    </div>

    <!-- 新增/編輯 Dialog -->
    <el-dialog v-model="dialog" :title="form.id ? '編輯桌位' : '新增桌位'" width="340px">
      <el-form label-width="80px">
        <el-form-item label="桌位名稱">
          <el-input v-model="form.name" placeholder="例：A1、吧台1、包廂" />
        </el-form-item>
        <el-form-item label="容納人數">
          <el-input-number v-model="form.capacity" :min="1" :max="30" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">儲存</el-button>
      </template>
    </el-dialog>

    <!-- QR Code Dialog -->
    <el-dialog v-model="qrDialog" :title="`${qrTable?.name} — 掃碼點餐`" width="300px">
      <div v-if="qrTable" style="text-align:center; padding:8px">
        <img :src="`https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(qrUrl(qrTable))}`"
          style="width:220px; height:220px" alt="QR Code" />
        <div style="margin-top:10px; font-size:12px; color:#999; word-break:break-all">
          {{ qrUrl(qrTable) }}
        </div>
      </div>
      <template #footer>
        <el-button @click="qrDialog = false">關閉</el-button>
        <el-button type="primary" @click="() => window.print()">列印</el-button>
      </template>
    </el-dialog>
  </div>
</template>
