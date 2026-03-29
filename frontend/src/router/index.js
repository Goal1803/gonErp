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
        path: 'profile',
        name: 'profile',
        component: () => import('pages/ProfilePage.vue'),
        meta: { title: 'My Profile' }
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
        name: 'designsHome',
        component: () => import('pages/DesignsHomePage.vue'),
        meta: { title: 'Designs', requiresModule: 'hasDesigns' }
      },
      {
        path: 'designs/list',
        name: 'designs',
        component: () => import('pages/DesignsPage.vue'),
        meta: { title: 'Designs', requiresModule: 'hasDesigns' }
      },
      {
        path: 'designs/boards',
        name: 'designBoards',
        component: () => import('pages/DesignBoardsPage.vue'),
        meta: { title: 'Design Boards', requiresModule: 'hasDesigns' }
      },
      {
        path: 'designs/boards/:boardId',
        name: 'designBoard',
        component: () => import('pages/BoardPage.vue'),
        meta: { title: 'Design Board', requiresModule: 'hasDesigns' }
      },
      {
        path: 'tasks/:boardId/dashboard',
        name: 'boardDashboard',
        component: () => import('pages/DesignDashboardPage.vue'),
        meta: { title: 'Board Dashboard', requiresModule: 'hasTaskManager' }
      },
      {
        path: 'designs/dashboard',
        name: 'designsDashboard',
        component: () => import('pages/DesignDashboardPage.vue'),
        meta: { title: 'Designs Dashboard', requiresModule: 'hasDesigns' }
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
      },
      // ─── Finance Module ──────────────────────────────────────────────
      {
        path: 'finance',
        name: 'finance',
        component: () => import('pages/FinancePage.vue'),
        meta: { title: 'Finance', requiresModule: 'hasFinance' }
      },
      {
        path: 'finance/config',
        name: 'financeConfig',
        component: () => import('pages/FinanceConfigPage.vue'),
        meta: { title: 'Finance Config', requiresModule: 'hasFinance', requiresAdmin: true }
      },
      {
        path: 'finance/gma',
        name: 'gmaMonthlyDashboard',
        component: () => import('pages/GmaMonthlyDashboardPage.vue'),
        meta: { title: 'Monthly Accounting', requiresModule: 'hasFinance' }
      },
      {
        path: 'finance/accounts',
        name: 'financeAccounts',
        component: () => import('pages/GmaAccountsPage.vue'),
        meta: { title: 'Bank Accounts', requiresModule: 'hasFinance' }
      },
      {
        path: 'finance/currencies',
        name: 'financeCurrencies',
        component: () => import('pages/FinanceCurrencyPage.vue'),
        meta: { title: 'Currencies', requiresModule: 'hasFinance' }
      },
      {
        path: 'finance/gma/report/:id',
        name: 'gmaMonthlyReport',
        component: () => import('pages/GmaMonthlyReportPage.vue'),
        meta: { title: 'Monthly Report', requiresModule: 'hasFinance' }
      },
      {
        path: 'finance/gma/rules',
        name: 'gmaRules',
        component: () => import('pages/GmaRulesPage.vue'),
        meta: { title: 'Categorization Rules', requiresModule: 'hasFinance' }
      },
      {
        path: 'finance/gma/amazon',
        name: 'gmaAmazonReconciliation',
        component: () => import('pages/GmaAmazonReconciliationPage.vue'),
        meta: { title: 'Amazon Reconciliation', requiresModule: 'hasFinance' }
      },
      {
        path: 'finance/gma/share',
        name: 'gmaShare',
        component: () => import('pages/GmaSharePage.vue'),
        meta: { title: 'Share Links', requiresModule: 'hasFinance' }
      },
      // ─── E-commerce Module ──────────────────────────────────────────
      { path: 'ecommerce', name: 'ecommerce', component: () => import('pages/EcommercePage.vue'), meta: { title: 'E-commerce', requiresModule: 'hasEcommerce' } },
      { path: 'ecommerce/stores', name: 'ecomStores', component: () => import('pages/EcomStoresPage.vue'), meta: { title: 'Stores', requiresModule: 'hasEcommerce' } },
      { path: 'ecommerce/stores/:id', name: 'ecomStoreDetail', component: () => import('pages/EcomStoreDetailPage.vue'), meta: { title: 'Store Detail', requiresModule: 'hasEcommerce' } },
      { path: 'ecommerce/orders', name: 'ecomOrders', component: () => import('pages/EcomOrdersPage.vue'), meta: { title: 'Orders', requiresModule: 'hasEcommerce' } },
      { path: 'ecommerce/orders/:id', name: 'ecomOrderDetail', component: () => import('pages/EcomOrderDetailPage.vue'), meta: { title: 'Order Detail', requiresModule: 'hasEcommerce' } },
      { path: 'ecommerce/suppliers', name: 'ecomSuppliers', component: () => import('pages/EcomSuppliersPage.vue'), meta: { title: 'Suppliers', requiresModule: 'hasEcommerce' } },
      // ─── Working Time Management ─────────────────────────────────────
      {
        path: 'worktime',
        name: 'worktime',
        component: () => import('pages/WorkTimePage.vue'),
        meta: { title: 'Working Time', requiresModule: 'hasWorkTime' }
      },
      {
        path: 'worktime/clock',
        name: 'worktimeClock',
        component: () => import('pages/WorkTimeClockPage.vue'),
        meta: { title: 'Time Clock', requiresModule: 'hasWorkTime' }
      },
      {
        path: 'worktime/day-off',
        name: 'worktimeDayOff',
        component: () => import('pages/WorkTimeDayOffPage.vue'),
        meta: { title: 'Day Off', requiresModule: 'hasWorkTime' }
      },
      {
        path: 'worktime/day-off/admin',
        name: 'worktimeDayOffAdmin',
        component: () => import('pages/WorkTimeDayOffAdminPage.vue'),
        meta: { title: 'Day Off Admin', requiresModule: 'hasWorkTime', requiresAdmin: true }
      },
      {
        path: 'worktime/calendar',
        name: 'worktimeCalendar',
        component: () => import('pages/WorkTimeCalendarPage.vue'),
        meta: { title: 'Team Calendar', requiresModule: 'hasWorkTime' }
      },
      {
        path: 'worktime/reports',
        name: 'worktimeReports',
        component: () => import('pages/WorkTimeReportsPage.vue'),
        meta: { title: 'Reports', requiresModule: 'hasWorkTime' }
      },
      {
        path: 'worktime/team',
        name: 'worktimeTeam',
        component: () => import('pages/WorkTimeTeamPage.vue'),
        meta: { title: 'Team', requiresModule: 'hasWorkTime' }
      },
      {
        path: 'worktime/settings',
        name: 'worktimeSettings',
        component: () => import('pages/WorkTimeSettingsPage.vue'),
        meta: { title: 'Work Time Settings', requiresModule: 'hasWorkTime', requiresAdmin: true }
      }
    ]
  },
  // ─── Public Steuerberater Page (no auth, outside MainLayout) ──────
  {
    path: '/finance/steuerberater/:token',
    name: 'steuerberater',
    component: () => import('pages/GmaSteuerberaterPage.vue'),
    meta: { title: 'Shared Report' }
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
