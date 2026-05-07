import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/login', component: () => import('@/views/LoginPage.vue'), meta: { public: true } },
  { path: '/order/:token', component: () => import('@/views/CustomerOrderPage.vue'), meta: { public: true } },
  { path: '/reservation', component: () => import('@/views/ReservationPage.vue'), meta: { public: true } },
  { path: '/kitchen', component: () => import('@/views/KitchenDisplayPage.vue'), meta: { roles: ['KITCHEN', 'MANAGER', 'ADMIN'] } },
  { path: '/bar', component: () => import('@/views/BarDisplayPage.vue'), meta: { roles: ['BARTENDER', 'MANAGER', 'ADMIN'] } },
  { path: '/staff', component: () => import('@/views/StaffPage.vue'), meta: { roles: ['WAITER', 'MANAGER', 'ADMIN'] } },
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { roles: ['ADMIN', 'MANAGER'] },
    redirect: to => {
      const auth = useAuthStore()
      return auth.role === 'ADMIN' ? '/admin/reports' : '/admin/menu'
    },
    children: [
      { path: 'reports',     component: () => import('@/views/admin/ReportsPage.vue'),     meta: { roles: ['ADMIN'] } },
      { path: 'menu',        component: () => import('@/views/admin/MenuPage.vue'),         meta: { roles: ['ADMIN', 'MANAGER'] } },
      { path: 'tables',      component: () => import('@/views/admin/TablesPage.vue'),       meta: { roles: ['ADMIN', 'MANAGER'] } },
      { path: 'ingredients', component: () => import('@/views/admin/IngredientsPage.vue'), meta: { roles: ['ADMIN', 'MANAGER'] } },
{ path: 'settings',    component: () => import('@/views/admin/SettingsPage.vue'),    meta: { roles: ['ADMIN'] } },
      { path: 'users',       component: () => import('@/views/admin/UsersPage.vue'),       meta: { roles: ['ADMIN'] } },
    ],
  },
  { path: '/', redirect: '/login' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn) return '/login'
  if (to.meta.roles && !to.meta.roles.includes(auth.role)) return '/login'
})

export default router
