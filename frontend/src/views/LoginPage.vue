<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const auth = useAuthStore()
const form = ref({ account: '', password: '', rememberDevice: false })
const loading = ref(false)

async function handleLogin() {
  if (!form.value.account || !form.value.password) {
    ElMessage.warning('請輸入帳號與密碼')
    return
  }
  loading.value = true
  try {
    await auth.login(form.value.account, form.value.password, form.value.rememberDevice)
    // 首次登入 / 密碼被重置 → 強制改密碼
    if (auth.mustChangePassword) {
      ElMessage.warning('請先修改預設密碼')
      router.push('/change-password')
      return
    }
    const role = auth.role
    if (role === 'KITCHEN') router.push('/kitchen')
    else if (role === 'BARTENDER') router.push('/bar')
    else if (role === 'WAITER') router.push('/staff')
    else router.push('/admin')
  } catch (e) {
    const code = e.response?.data?.code
    if (code === 1002) ElMessage.error('帳號已被鎖定，請 15 分鐘後再試')
    else if (code === 1003) ElMessage.error('來源 IP 已被鎖定，請稍後再試')
    else if (code === 1007) ElMessage.error('帳號已停用')
    else ElMessage.error(e.response?.data?.message || '登入失敗')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-bg">
    <el-card class="login-card">
      <h2 style="text-align:center; margin-bottom:24px">Pobar 管理系統</h2>
      <el-form @submit.prevent="handleLogin" label-position="top">
        <el-form-item label="帳號">
          <el-input v-model="form.account" placeholder="請輸入帳號" prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密碼">
          <el-input v-model="form.password" type="password" placeholder="請輸入密碼"
            prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <div style="margin-bottom:12px">
          <el-checkbox v-model="form.rememberDevice">
            本機記住（30 天免重登）
          </el-checkbox>
          <div style="font-size:11px; color:#909399; margin-left:22px; margin-top:-2px">
            僅在店內信任設備（POS / 廚顯）勾選
          </div>
        </div>
        <el-button type="primary" style="width:100%" :loading="loading" @click="handleLogin">
          登入
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login-bg {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #1a1a2e;
}
.login-card {
  width: 360px;
}
</style>
