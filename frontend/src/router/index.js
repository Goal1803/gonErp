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
        meta: { title: 'Image Manager', requiresModule: 'hasImageManager' }
      },
      {
        path: 'tasks',
        name: 'tasks',
        component: () => import('pages/TaskManagerPage.vue'),
        meta: { title: 'Task Manager', requiresModule: 'hasTaskManager' }
      },
      {
        path: 'tasks/:boardId',
        name: 'board',
        component: () => import('pages/BoardPage.vue'),
        meta: { title: 'Board', requiresModule: 'hasTaskManager' }
      },
      {
        path: 'designs',
        name: 'designs',
        component: () => import('pages/DesignsPage.vue'),
        meta: { title: 'Designs', requiresModule: 'hasDesigns' }
      },
      {
        path: 'admin/lookups',
        name: 'lookups',
        component: () => import('pages/LookupManagerPage.vue'),
        meta: { title: 'Lookups', requiresAdmin: true }
      },
      {
        path: 'organizations',
        name: 'organizations',
        component: () => import('pages/OrganizationManagerPage.vue'),
        meta: { title: 'Organizations', requiresSuperAdmin: true }
      },
      {
        path: 'organization-types',
        name: 'organizationTypes',
        component: () => import('pages/OrgTypeManagerPage.vue'),
        meta: { title: 'Organization Types', requiresSuperAdmin: true }
      },
      {
        path: 'org-structure',
        name: 'orgStructure',
        component: () => import('pages/OrgStructurePage.vue'),
        meta: { title: 'Organization Structure', requiresAdmin: true }
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

    if (to.meta.requiresSuperAdmin && !authStore.isSuperAdmin) {
      return next('/dashboard')
    }

    if (to.meta.requiresAdmin && !authStore.isAdmin) {
      return next('/dashboard')
    }

    if (to.meta.requiresModule && !authStore[to.meta.requiresModule]) {
      return next('/dashboard')
    }

    next()
  })

  return Router
})
