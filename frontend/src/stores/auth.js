import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as adminApi from '@/api/admin'

export const useAuthStore = defineStore('auth', () => {
  const admin = ref(null)
  const checked = ref(false)

  async function checkSession() {
    try {
      admin.value = await adminApi.getCurrentAdmin()
    } catch {
      admin.value = null
    } finally {
      checked.value = true
    }
    return Boolean(admin.value)
  }

  async function signIn(credentials) {
    admin.value = await adminApi.login(credentials)
    checked.value = true
  }

  async function signOut() {
    try {
      await adminApi.logout()
    } finally {
      admin.value = null
      checked.value = true
    }
  }

  return { admin, checked, checkSession, signIn, signOut }
})
