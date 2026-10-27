// 晓镜书阁 · 扫码页
const { request, extractIsbn, fmtMoney } = require('../../utils/request.js')
const app = getApp()

Page({
  data: {
    manual: '',
    result: null,
    isbn: '',
    entityRows: [],
    stockList: []
  },

  onShow() {
    app.requireLogin()
  },

  onManual(e) { this.setData({ manual: e.detail.value }) },

  startScan() {
    wx.scanCode({
      scanType: ['barCode', 'qrCode'],
      success: (res) => this.resolve(res.result),
      fail: () => wx.showToast({ title: '扫码取消', icon: 'none' })
    })
  },

  resolveManual() {
    const code = (this.data.manual || '').trim()
    if (!code) { wx.showToast({ title: '请输入内容', icon: 'none' }); return }
    this.resolve(code)
  },

  resolve(code) {
    code = (code || '').trim()
    if (!code) return
    this.rawCode = code
    this.setData({ stockList: [], result: null })
    // POST /api/scan/resolve
    request('/scan/resolve', { method: 'POST', data: { code } })
      .then((result) => {
        const isbn = (result.entity && result.entity.isbn) || extractIsbn(code)
        this.setData({ result, isbn, entityRows: this.buildRows(result.entity, code) })
        // 记录扫码日志
        request('/scan/log', {
          method: 'POST',
          data: { code, bizType: result.bizType, bizId: (result.entity || {}).isbn || '', device: 'MINI', result: 'SUCCESS' }
        }).catch(() => {})
      })
      .catch((e) => {
        wx.showToast({ title: e.message, icon: 'none' })
        this.setData({ result: null, entityRows: [], isbn: '' })
      })
  },

  buildRows(entity, code) {
    entity = entity || {}
    const rows = []
    const push = (k, v) => { if (v !== undefined && v !== null && v !== '') rows.push({ k, v }) }
    if (entity.isbn) push('ISBN', entity.isbn)
    if (entity.bookName) push('书名', entity.bookName)
    if (entity.author) push('作者', entity.author)
    if (entity.publisherName) push('出版社', entity.publisherName)
    if (entity.categoryName) push('分类', entity.categoryName)
    if (entity.price !== undefined) push('定价', '¥' + fmtMoney(entity.price))
    if (entity.shelfNo) push('货架位', entity.shelfNo)
    if (entity.orderNo) push('单号', entity.orderNo)
    if (entity.status) push('状态', entity.status)
    if (rows.length === 0) push('内容', code || '-')
    return rows
  },

  queryStock() {
    const isbn = this.data.isbn
    if (!isbn) return
    // GET /api/mobile/stock/quick?keyword=
    request('/mobile/stock/quick', { params: { keyword: isbn } })
      .then((quick) => {
        let list = []
        if (quick && (quick.stockList || quick.stocks)) list = quick.stockList || quick.stocks
        this.setData({ stockList: list })
      })
      .catch((e) => wx.showToast({ title: e.message, icon: 'none' }))
  }
})