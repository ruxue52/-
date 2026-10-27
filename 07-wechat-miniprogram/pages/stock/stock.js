// 晓镜书阁 · 库存查询页
const { request, extractIsbn } = require('../../utils/request.js')
const app = getApp()

Page({
  data: {
    keyword: '',
    book: null,
    stockList: [],
    searched: false
  },

  onShow() {
    app.requireLogin()
  },

  onKeyword(e) { this.setData({ keyword: e.detail.value }) },

  scan() {
    wx.scanCode({
      scanType: ['barCode', 'qrCode'],
      success: (res) => {
        this.setData({ keyword: extractIsbn(res.result) })
        this.search()
      },
      fail: () => {}
    })
  },

  search() {
    const kw = (this.data.keyword || '').trim()
    if (!kw) { wx.showToast({ title: '请输入关键词', icon: 'none' }); return }
    this.setData({ searched: true })
    // GET /api/mobile/stock/quick?keyword=
    request('/mobile/stock/quick', { params: { keyword: kw } })
      .then((quick) => {
        if (quick && quick.book) {
          this.setData({ book: quick.book, stockList: quick.stockList || quick.stocks || [] })
        } else if (quick && (quick.bookName || quick.isbn)) {
          this.setData({ book: quick, stockList: quick.stockList || [] })
        } else {
          this.setData({ book: null, stockList: [] })
        }
      })
      .catch((e) => {
        wx.showToast({ title: e.message, icon: 'none' })
        this.setData({ book: null, stockList: [] })
      })
  }
})