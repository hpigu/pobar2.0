<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const auth = useAuthStore()
const form = ref({ account: '', password: '' })
const loading = ref(false)

async function handleLogin() {
  if (!form.value.account || !form.value.password) {
    ElMessage.warning('請輸入帳號與密碼')
    return
  }
  loading.value = true
  try {
    await auth.login(form.value.account, form.value.password)
    const role = auth.role
    if (role === 'KITCHEN') router.push('/kitchen')
    else if (role === 'BARTENDER') router.push('/bar')
    else if (role === 'WAITER') router.push('/staff')
    else router.push('/admin')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '登入失敗')
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
