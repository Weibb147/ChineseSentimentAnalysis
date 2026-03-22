// src/stores/auth.ts
import { defineStore } from 'pinia'
import { parseJwt } from '@/utils/jwtUtils'

type UserInfo = {
  id?: number | string
  username?: string
  role?: string
  imageUrl?: string
  [key: string]: any
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: '',
    userInfo: {} as UserInfo,
    showLoginDrawer: false
  }),

  actions: {
    setToken(newToken: string) {
      this.token = newToken
      // 解析并设置用户信息
      if (newToken) {
        const parsedInfo = parseJwt(newToken)
        if (Object.keys(parsedInfo).length > 0) {
          this.setUserInfo(parsedInfo)
        }
      }
    },

    setUserInfo(newInfo: UserInfo) {
      this.userInfo = newInfo
    },

    removeAuth() {
      this.token = ''
      this.userInfo = {} as UserInfo
    },

    setShowLoginDrawer(val: boolean) {
      this.showLoginDrawer = val
    }
  },

  getters: {
    isAuthenticated: (state) => {
      return !!state.token
    }
  },

  persist: true
})
