<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const auth = useAuthStore()

const form = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const loading = ref(false)

const forced = computed(() => auth.mustChangePassword)

const passwordRules = computed(() => {
  const np = form.value.newPassword
  return {
    length: np.length >= 8,
    hasLetter: /[A-Za-z]/.test(np),
    hasDigit: /\d/.test(np),
    notSame: np && np !== form.value.oldPassword,
    match: np && np === form.value.confirmPassword,
  }
})

const allValid = computed(() => Object.values(passwordRules.value).every(Boolean))

async function submit() {
  if (!form.value.oldPassword) return ElMessage.warning('請輸入舊密碼')
  if (!allValid.value) return ElMessage.warning('新密碼不符合規則')

  loading.value = true
  try {
    await auth.changePassword(form.value.oldPassword, form.value.newPassword)
    ElMessage.success('密碼已更新')
    // 改完後依角色導頁
    const role = auth.role
    if (role === 'KITCHEN') router.push('/kitchen')
    else if (role === 'BARTENDER') router.push('/bar')
    else if (role === 'WAITER') router.push('/staff')
    else router.push('/admin')
  } catch (e) {
    const code = e.response?.data?.code
    if (code === 1008) ElMessage.error('舊密碼錯誤')
    else if (code === 1009) ElMessage.error('新密碼強度不足')
    else ElMessage.error(e.response?.data?.message || '修改失敗')
  } finally {
    loading.value = false
  }
}

async function logout() {
  await auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="cp-bg">
    <el-card class="cp-card">
      <h2 style="text-align:center; margin-bottom:8px">修改密碼</h2>
      <el-alert v-if="forced" :closable="false" type="warning" show-icon style="margin-bottom:16px">
        首次登入或管理員重置密碼後，必須先修改密碼才能使用其他功能。
      </el-alert>

      <el-form @submit.prevent="submit" label-position="top">
        <el-form-item label="舊密碼">
          <el-input v-model="form.oldPassword" type="password" show-password
                    placeholder="輸入目前的密碼" />
        </el-form-item>
        <el-form-item label="新密碼">
          <el-input v-model="form.newPassword" type="password" show-password
                    placeholder="新密碼" />
        </el-form-item>
        <el-form-item label="再次輸入新密碼">
          <el-input v-model="form.confirmPassword" type="password" show-password
                    placeholder="再次輸入新密碼" @keyup.enter="submit" />
        </el-form-item>
      </el-form>

      <ul class="rules">
        <li :class="{ ok: passwordRules.length }">✓ 至少 8 字元</li>
        <li :class="{ ok: passwordRules.hasLetter }">✓ 至少含一個英文字母</li>
        <li :class="{ ok: passwordRules.hasDigit }">✓ 至少含一個數字</li>
        <li :class="{ ok: passwordRules.notSame }">✓ 不可與舊密碼相同</li>
        <li :class="{ ok: passwordRules.match }">✓ 兩次新密碼一致</li>
      </ul>

      <div style="display:flex; gap:8px; margin-top:16px">
        <el-button type="primary" style="flex:1" :loading="loading"
                   :disabled="!allValid" @click="submit">確認修改</el-button>
        <el-button v-if="!forced" @click="$router.back()">取消</el-button>
        <el-button v-else type="danger" plain @click="logout">登出</el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.cp-bg {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #1a1a2e;
  padding: 20px;
}
.cp-card { width: 420px; }
.rules { list-style: none; padding: 0; margin: 8px 0 0; font-size: 13px; }
.rules li { color: #b3b3b3; padding: 2px 0; }
.rules li.ok { color: #67c23a; }
</style>
