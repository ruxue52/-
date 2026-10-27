// 读者端 · 会员中心（积分 / 余额 / 累计消费 / 等级权益 / 消费记录入口）
const api = require('../../utils/request')

Page({
  data: {
    logged: false,
    loading: false,
    member: null,
    avatarText: '',
    levels: []
  },

  onShow() {
    const logged = getApp().isLogin()
    this.setData({ logged })
    if (logged) {
      this.loadProfile()
    } else {
      this.setData({ member: null, avatarText: '' })
    }
    this.loadLevels()
  },

  loadProfile() {
    this.setData({ loading: true })
    api.request('/reader/member/profile')
      .then((m) => {
        getApp().setMember(m)
        this.setData({
          member: {
            ...m,
            balanceText: api.fmtMoney(m.balance),
            totalAmountText: api.fmtMoney(m.totalAmount)
          },
          avatarText: api.coverText(m.customerName)
        })
      })
      .catch((e) => wx.showToast({ title: e.message, icon: 'none' }))
      .then(() => this.setData({ loading: false }))
  },

  loadLevels() {
    api.request('/reader/levels')
      .then((list) => this.setData({ levels: list || [] }))
      .catch(() => this.setData({ levels: [] }))
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' })
  },

  goOrders() {
    if (!this.data.logged) {
      this.goLogin()
      return
    }
    wx.navigateTo({ url: '/pages/orders/orders' })
  },

  goScan() {
    wx.switchTab({ url: '/pages/scan/scan' })
  },

  goBooks() {
    wx.navigateTo({ url: '/pages/books/books' })
  },

  logout() {
    wx.showModal({
      title: '提示',
      content: '确定退出当前会员账号？',
      success: (res) => {
        if (!res.confirm) return
        getApp().clearAuth()
        this.setData({ logged: false, member: null, avatarText: '' })
        wx.showToast({ title: '已退出登录', icon: 'none' })
      }
    })
  }
})
