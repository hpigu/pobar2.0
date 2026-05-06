<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()
const isAdmin = computed(() => auth.role === 'ADMIN')

async function logout() {
  await auth.logout()
  router.push('/login')
}
</script>

<template>
  <el-container style="min-height:100vh">
    <el-aside width="200px" style="background:#001529">
      <div style="padding:20px 16px; color:#fff; font-size:18px; font-weight:700">
        🍹 Pobar 後台
      </div>
      <el-menu router background-color="#001529" text-color="#adb5bd" active-text-color="#fff"
        :default-active="$route.path">

        <el-menu-item v-if="isAdmin" index="/admin/reports">
          <el-icon><TrendCharts /></el-icon>營收報表
        </el-menu-item>

        <el-menu-item index="/admin/menu">
          <el-icon><Food /></el-icon>品項管理
        </el-menu-item>

        <el-menu-item index="/admin/tables">
          <el-icon><Grid /></el-icon>桌位管理
        </el-menu-item>

        <el-menu-item index="/admin/ingredients">
          <el-icon><Box /></el-icon>食材管理
        </el-menu-item>

        <el-menu-item v-if="isAdmin" index="/admin/attributes">
          <el-icon><List /></el-icon>屬性管理
        </el-menu-item>

        <el-menu-item v-if="isAdmin" index="/admin/settings">
          <el-icon><Setting /></el-icon>系統設定
        </el-menu-item>

        <el-menu-item v-if="isAdmin" index="/admin/users">
          <el-icon><User /></el-icon>員工管理
        </el-menu-item>

      </el-menu>
      <div style="padding:16px; position:absolute; bottom:0; width:100%; box-sizing:border-box">
        <el-button link style="color:#adb5bd" @click="logout">登出</el-button>
      </div>
    </el-aside>
    <el-main style="background:#f0f2f5; padding:24px">
      <router-view />
    </el-main>
  </el-container>
</template>
