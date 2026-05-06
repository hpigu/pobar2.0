<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const users = ref([])
const dialogVisible = ref(false)
const editing = ref(null)
const form = ref({ account: '', password: '', email: '', phone: '', role: 'WAITER' })
const saving = ref(false)

const roles = [
  { value: 'ADMIN', label: '管理員' },
  { value: 'MANAGER', label: '主管' },
  { value: 'WAITER', label: '服務生' },
  { value: 'BARTENDER', label: '調酒師' },
  { value: 'KITCHEN', label: '廚師' },
]

async function load() {
  const res = await api.get('/api/admin/users')
  users.value = res.data.data || []
}

function openCreate() {
  editing.value = null
  form.value = { account: '', password: '', email: '', phone: '', role: 'WAITER' }
  dialogVisible.value = true
}

function openEdit(u) {
  editing.value = u
  form.value = { account: u.account, password: '', email: u.email || '', phone: u.phone || '', role: u.role }
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (editing.value) {
      const payload = { ...form.value }
      if (!payload.password) delete payload.password
      await api.put(`/api/admin/users/${editing.value.id}`, payload)
    } else {
      await api.post('/api/admin/users', form.value)
    }
    ElMessage.success(editing.value ? '更新成功' : '建立成功')
    dialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失敗')
  } finally { saving.value = false }
}

async function deactivate(u) {
  try {
    await ElMessageBox.confirm(`確認停用 ${u.account}？`, '停用帳號', { type: 'warning' })
    await api.delete(`/api/admin/users/${u.id}`)
    ElMessage.success('已停用')
    await load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失敗')
  }
}

onMounted(load)

function roleLabel(r) {
  return roles.find(x => x.value === r)?.label || r
}
</script>

<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px">
      <h2>員工管理</h2>
      <el-button type="primary" icon="Plus" @click="openCreate">新增員工</el-button>
    </div>

    <el-card>
      <el-table :data="users">
        <el-table-column prop="account" label="帳號" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">{{ roleLabel(row.role) }}</template>
        </el-table-column>
        <el-table-column prop="email" label="Email" />
        <el-table-column prop="phone" label="手機" />
        <el-table-column label="狀態" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isActive === 1 ? 'success' : 'danger'" size="small">
              {{ row.isActive === 1 ? '啟用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">編輯</el-button>
            <el-button link type="danger" :disabled="row.isActive === 0" @click="deactivate(row)">停用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing ? '編輯員工' : '新增員工'" width="440px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="帳號" required>
          <el-input v-model="form.account" :disabled="!!editing" />
        </el-form-item>
        <el-form-item :label="editing ? '新密碼' : '密碼'" :required="!editing">
          <el-input v-model="form.password" type="password" show-password
            :placeholder="editing ? '留空則不修改' : '請輸入密碼'" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role">
            <el-option v-for="r in roles" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="Email">
          <el-input v-model="form.email" type="email" />
        </el-form-item>
        <el-form-item label="手機">
          <el-input v-model="form.phone" placeholder="09xxxxxxxx" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">儲存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
