import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/driver' },
    { path: '/driver', component: () => import('@/views/driver/DriverFormView.vue') },
    { path: '/driver/success', component: () => import('@/views/driver/DriverSuccessView.vue') },
    { path: '/admin/login', component: () => import('@/views/admin/AdminLoginView.vue'), meta: { guestOnly: true } },
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/admin/records' },
        { path: 'records', component: () => import('@/views/admin/AdminRecordsView.vue') },
        { path: 'records/:id', component: () => import('@/views/admin/AdminRecordDetailView.vue') }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/driver' }
  ]
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if ((to.meta.requiresAuth || to.meta.guestOnly) && !auth.checked) await auth.checkSession()
  if (to.meta.requiresAuth && !auth.admin) {
    return { path: '/admin/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && auth.admin) return '/admin/records'
})

export default router
