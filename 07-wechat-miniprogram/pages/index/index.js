// 晓镜书阁 · 首页
const { request, fmtMoney } = require('../../utils/request.js')
const app = getApp()

Page({
  data: {
    overview: null,
    statList: [],
    userText: '您好'
  },

  onShow() {
    if (!app.requireLogin()) return
    const u = app.globalData.userInfo
    this.setData({ userText: u && (u.realName || u.username) ? '您好，' + (u.realName || u.username) : '您好' })
    this.loadOverview()
    this.loadStat()
  },

  go(e) {
    const url = e.currentTarget.dataset.url
    const isTab = e.currentTarget.dataset.tab === '1'
    if (isTab) wx.switchTab({ url })
    else wx.navigateTo({ url })
  },

  loadOverview() {
    // GET /api/mobile/overview
    request('/mobile/overview')
      .then((data) => this.setData({ overview: data || null }))
      .catch(() => this.setData({ overview: null }))
  },

  loadStat() {
    // GET /api/mobile/stat/summary
    request('/mobile/stat/summary')
      .then((data) => {
        const LABELS = {
          todaySaleAmount: '今日销售额',
          todaySaleCount: '今日销售单数',
          monthSaleAmount: '本月销售额',
          stockAmount: '库存总额',
          warningCount: '库存预警数',
          bookCount: '图书品种数',
          totalSku: 'SKU 数'
        }
        const list = []
        Object.keys(data || {}).forEach((k) => {
          let v = data[k]
          if (typeof v === 'number' && /amount/i.test(k)) v = '¥' + fmtMoney(v)
          list.push({ label: LABELS[k] || k, value: v })
        })
        this.setData({ statList: list })
      })
      .catch(() => this.setData({ statList: [] }))
  },

  logout() {
    wx.showModal({
      title: '提示',
      content: '确认退出登录？',
      success: (res) => {
        if (res.confirm) {
          request('/auth/logout', { method: 'POST' }).catch(() => {})
          app.clearAuth()
          wx.reLaunch({ url: '/pages/login/login' })
        }
      }
    })
  }
})