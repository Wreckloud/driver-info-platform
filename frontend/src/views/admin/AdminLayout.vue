<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { SwitchButton, Van } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

async function signOut() {
  try {
    await auth.signOut()
    await router.replace('/admin/login')
  } catch (error) {
    ElMessage.error(error.message)
  }
}
</script>

<template>
  <div class="admin-shell">
    <header class="admin-header">
      <RouterLink class="admin-brand" to="/admin/records">
        <span class="admin-brand-icon"><el-icon><Van /></el-icon></span>
        <span><strong>出车登记</strong><small>管理后台</small></span>
      </RouterLink>
      <div class="admin-account">
        <span>{{ auth.admin?.username }}</span>
        <el-button text :icon="SwitchButton" @click="signOut">退出</el-button>
      </div>
    </header>
    <RouterView />
  </div>
</template>
