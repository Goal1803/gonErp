import { route } from 'quasar/wrappers'
import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from 'src/stores/authStore'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('pages/LoginPage.vue'),
    meta: { requiresGuest: true }
  },
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/dashboard'
      },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('pages/DashboardPage.vue'),
        meta: { title: 'Dashboard' }
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('pages/UserManagerPage.vue'),
        meta: { title: 'User Manager', requiresAdmin: true }
      },
      {
        path: 'images',
        name: 'images',
        component: () => import('pages/ImageManagerPage.vue'),
        meta: { title: 'Image Manager' }
      }
    ]
  },
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue')
  }
]

export default route(function () {
  const Router = createRouter({
    scrollBehavior: () => ({ left: 0, top: 0 }),
    routes,
    history: createWebHashHistory()
  })

  Router.beforeEach((to, from, next) => {
    const authStore = useAuthStore()

    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
      return next('/login')
    }

    if (to.meta.requiresGuest && authStore.isAuthenticated) {
      return next('/dashboard')
    }

    if (to.meta.requiresAdmin && !authStore.isAdmin) {
      return next('/dashboard')
    }

    next()
  })

  return Router
})
