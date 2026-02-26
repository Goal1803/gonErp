import { defineStore } from 'pinia'
import { LocalStorage } from 'quasar'
import { api } from 'src/boot/axios'

function getTokenExpiry(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.exp ? payload.exp * 1000 : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: LocalStorage.getItem('gonerp_token') || null,
    user: LocalStorage.getItem('gonerp_user') ? JSON.parse(LocalStorage.getItem('gonerp_user')) : null,
    _expiryTimer: null
  }),

  getters: {
    isAuthenticated: (state) => !!state.token,
    isSuperAdmin: (state) => state.user?.role === 'SUPER_ADMIN',
    isAdmin: (state) => state.user?.role === 'ADMIN' || state.user?.role === 'SUPER_ADMIN',
    currentUser: (state) => state.user,
    organizationName: (state) => state.user?.organizationName,
    hasTaskManager: (state) => state.user?.moduleTaskManager ?? true,
    hasImageManager: (state) => state.user?.moduleImageManager ?? true,
    hasDesigns: (state) => state.user?.moduleDesigns ?? true,
    hasWorkTime: (state) => state.user?.moduleWorkTime ?? false
  },

  actions: {
    async login(userName, password) {
      const response = await api.post('/auth/login', { userName, password })
      const { token, tokenType, ...user } = response.data.data
      this.token = token
      this.user = user
      LocalStorage.set('gonerp_token', token)
      LocalStorage.set('gonerp_user', JSON.stringify(user))
      this._scheduleAutoLogout()
      return response.data
    },

    logout() {
      if (this._expiryTimer) {
        clearTimeout(this._expiryTimer)
        this._expiryTimer = null
      }
      this.token = null
      this.user = null
      LocalStorage.remove('gonerp_token')
      LocalStorage.remove('gonerp_user')
    },

    _scheduleAutoLogout() {
      if (this._expiryTimer) clearTimeout(this._expiryTimer)
      if (!this.token) return

      const expiry = getTokenExpiry(this.token)
      if (!expiry) return

      const msUntilExpiry = expiry - Date.now()
      if (msUntilExpiry <= 0) {
        this.logout()
        this.router.push('/login')
        return
      }

      this._expiryTimer = setTimeout(() => {
        this.logout()
        this.router.push('/login')
      }, msUntilExpiry)
    },

    initAutoLogout() {
      if (this.token) this._scheduleAutoLogout()
    },

    async fetchCurrentUser() {
      try {
        const response = await api.get('/auth/me')
        this.user = response.data.data
        LocalStorage.set('gonerp_user', JSON.stringify(this.user))
      } catch {
        this.logout()
      }
    }
  }
})
