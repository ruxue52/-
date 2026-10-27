// 读者端 · 扫码 / 手输 ISBN 查书
const api = require('../../utils/request')

const RECENT_KEY = 'shg_scan_recent'

Page({
  data: {
    isbn: '',
    recent: []
  },

  onShow() {
    this.setData({ recent: wx.getStorageSync(RECENT_KEY) || [] })
  },

  onIsbnInput(e) {
    this.setData({ isbn: e.detail.value })
  },

  doScan() {
    wx.scanCode({
      scanType: ['barCode', 'qrCode'],
      success: (res) => {
        const isbn = api.extractIsbn(res.result)
        if (!isbn) {
          wx.showToast({ title: '未识别到图书条码', icon: 'none' })
          return
        }
        this.query(isbn)
      },
      fail: () => {}
    })
  },

  onQuery() {
    const isbn = (this.data.isbn || '').trim()
    if (!isbn) {
      wx.showToast({ title: '请输入 ISBN 或条码内容', icon: 'none' })
      return
    }
    this.query(isbn)
  },

  query(isbn) {
    this.saveRecent(isbn)
    wx.navigateTo({ url: '/pages/detail/detail?isbn=' + encodeURIComponent(isbn) })
  },

  saveRecent(isbn) {
    const list = (wx.getStorageSync(RECENT_KEY) || []).filter((item) => item.isbn !== isbn)
    list.unshift({ isbn, time: api.fmtDate(new Date().toISOString()) })
    wx.setStorageSync(RECENT_KEY, list.slice(0, 8))
    this.setData({ recent: wx.getStorageSync(RECENT_KEY) })
  },

  queryRecent(e) {
    this.query(e.currentTarget.dataset.isbn)
  },

  clearRecent() {
    wx.removeStorageSync(RECENT_KEY)
    this.setData({ recent: [] })
  }
})
