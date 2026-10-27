import { defineStore } from 'pinia'
import { authApi } from '@/api'

const TOKEN_KEY = 'shg_token'
const USER_KEY = 'shg_user'
const PERM_KEY = 'shg_perms'

function readJson(key, fallback) {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : fallback
  } catch (e) {
    return fallback
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    userInfo: readJson(USER_KEY, null),
    permissions: readJson(PERM_KEY, [])
  }),

  getters: {
    isLogin: (state) => !!state.token,
    username: (state) => (state.userInfo ? state.userInfo.username : ''),
    realName: (state) =>
      (state.userInfo && (state.userInfo.realName || state.userInfo.username)) || '未登录',
    roles: (state) => (state.userInfo && state.userInfo.roles) || [],
    isAdmin: (state) => {
      const roles = (state.userInfo && state.userInfo.roles) || []
      return roles.map((r) => String(r).toUpperCase()).includes('ADMIN')
    },
    hasPerm: (state) => {
      return (code) => {
        if (!code) return true
        // 管理员或通配权限视为拥有全部权限
        const roles = (state.userInfo && state.userInfo.roles) || []
        if (roles.map((r) => String(r).toUpperCase()).includes('ADMIN')) return true
        if (state.permissions.includes('*')) return true
        if (!state.permissions.length) return true // 后端未下发权限时不隐藏菜单
        return state.permissions.some(
          (p) => p === code || p === code.split(':')[0] + ':*' || code.startsWith(p + ':')
        )
      }
    }
  },

  actions: {
    async login(payload) {
      const data = await authApi.login(payload)
      this.token = data.token || ''
      this.userInfo = data.userInfo || null
      this.permissions = data.permissions || []
      localStorage.setItem(TOKEN_KEY, this.token)
      localStorage.setItem(USER_KEY, JSON.stringify(this.userInfo))
      localStorage.setItem(PERM_KEY, JSON.stringify(this.permissions))
      return data
    },

    async fetchMe() {
      const data = await authApi.me()
      if (data) {
        this.userInfo = data
        if (data.permissions) this.permissions = data.permissions
        localStorage.setItem(USER_KEY, JSON.stringify(this.userInfo))
        localStorage.setItem(PERM_KEY, JSON.stringify(this.permissions))
      }
      return data
    },

    async logout() {
      try {
        await authApi.logout()
      } catch (e) {
        // 忽略退出接口异常
      }
      this.clear()
    },

    clear() {
      this.token = ''
      this.userInfo = null
      this.permissions = []
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      localStorage.removeItem(PERM_KEY)
    }
  }
})