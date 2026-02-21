import { defineStore } from 'pinia'
import { LocalStorage } from 'quasar'
import { api } from 'src/boot/axios'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: LocalStorage.getItem('gonerp_token') || null,
    user: LocalStorage.getItem('gonerp_user') ? JSON.parse(LocalStorage.getItem('gonerp_user')) : null
  }),

  getters: {
    isAuthenticated: (state) => !!state.token,
    isSuperAdmin: (state) => state.user?.role === 'SUPER_ADMIN',
    isAdmin: (state) => state.user?.role === 'ADMIN' || state.user?.role === 'SUPER_ADMIN',
    currentUser: (state) => state.user,
    organizationName: (state) => state.user?.organizationName,
    hasTaskManager: (state) => state.user?.moduleTaskManager ?? true,
    hasImageManager: (state) => state.user?.moduleImageManager ?? true,
    hasDesigns: (state) => state.user?.moduleDesigns ?? true
  },

  actions: {
    async login(userName, password) {
      const response = await api.post('/auth/login', { userName, password })
      const { token, tokenType, ...user } = response.data.data
      this.token = token
      this.user = user
      LocalStorage.set('gonerp_token', token)
      LocalStorage.set('gonerp_user', JSON.stringify(user))
      return response.data
    },

    logout() {
      this.token = null
      this.user = null
      LocalStorage.remove('gonerp_token')
      LocalStorage.remove('gonerp_user')
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
