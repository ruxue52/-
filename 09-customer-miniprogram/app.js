// 晓镜书阁 · 读者端（会员端）小程序入口
App({
  globalData: {
    // 后端基地址：开发者工具中需勾选「不校验合法域名」才能连本地/非 https 地址
    // 真机调试时改为局域网地址，如 http://192.168.1.10:8080/api
    baseUrl: 'http://localhost:8080/api',
    memberInfo: null
  },

  onLaunch() {
    const token = wx.getStorageSync('shg_member_token')
    const member = wx.getStorageSync('shg_member')
    if (token) this.globalData.token = token
    if (member) this.globalData.memberInfo = member
    console.log('晓镜书阁 · 读者端小程序启动')
  },

  getToken() {
    return this.globalData.token || wx.getStorageSync('shg_member_token') || ''
  },
  getMember() {
    return this.globalData.memberInfo || wx.getStorageSync('shg_member') || null
  },
  setAuth(token, member) {
    this.globalData.token = token
    this.globalData.memberInfo = member || null
    wx.setStorageSync('shg_member_token', token || '')
    wx.setStorageSync('shg_member', member || null)
  },
  setMember(member) {
    this.globalData.memberInfo = member || null
    wx.setStorageSync('shg_member', member || null)
  },
  clearAuth() {
    this.globalData.token = ''
    this.globalData.memberInfo = null
    wx.removeStorageSync('shg_member_token')
    wx.removeStorageSync('shg_member')
  },
  isLogin() {
    return !!this.getToken()
  },
  /** 需要登录的场景：未登录则跳登录页并返回 false */
  requireLogin() {
    if (!this.isLogin()) {
      wx.navigateTo({ url: '/pages/login/login' })
      return false
    }
    return true
  }
})
