// 晓镜书阁 · 微信小程序入口
App({
  globalData: {
    // 后端基地址：开发者工具中需勾选「不校验合法域名」才能连本地/非 https 地址
    baseUrl: 'http://localhost:8080/api',
    userInfo: null
  },

  onLaunch() {
    // 读取本地缓存的登录信息
    const token = wx.getStorageSync('shg_token')
    const user = wx.getStorageSync('shg_user')
    if (token) this.globalData.token = token
    if (user) this.globalData.userInfo = user
    console.log('晓镜书阁 · 微信小程序启动')
  },

  // 统一登录态
  getToken() {
    return this.globalData.token || wx.getStorageSync('shg_token') || ''
  },
  setAuth(token, userInfo) {
    this.globalData.token = token
    this.globalData.userInfo = userInfo
    wx.setStorageSync('shg_token', token || '')
    wx.setStorageSync('shg_user', userInfo || null)
  },
  clearAuth() {
    this.globalData.token = ''
    this.globalData.userInfo = null
    wx.removeStorageSync('shg_token')
    wx.removeStorageSync('shg_user')
  },
  requireLogin() {
    if (!this.getToken()) {
      wx.reLaunch({ url: '/pages/login/login' })
      return false
    }
    return true
  }
})