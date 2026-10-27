// 晓镜书阁 · 移动盘点页
// 接口：GET /api/stock/check/orders、GET /api/stock/check/orders/{id}
//       POST /api/mobile/check/scan、PUT /api/stock/check/orders/{id}/items
//       POST /api/stock/check/orders/{id}/finish
const { request, recordsOf, extractIsbn } = require('../../utils/request.js')
const app = getApp()

Page({
  data: {
    orders: [],
    checkId: null,
    detail: {},
    items: [],
    manualCode: '',
    diffCount: 0
  },

  onShow() {
    if (!app.requireLogin()) return
    if (!this.data.checkId) this.loadOrders()
  },

  loadOrders() {
    // GET /api/stock/check/orders
    request('/stock/check/orders', { params: { page: 1, size: 50, sort: 'createTime,desc' } })
      .then((data) => this.setData({ orders: recordsOf(data) }))
      .catch((e) => {
        wx.showToast({ title: e.message, icon: 'none' })
        this.setData({ orders: [] })
      })
  },

  selectOrder(e) {
    this.setData({ checkId: e.currentTarget.dataset.id })
    this.loadDetail()
  },

  backToList() {
    this.setData({ checkId: null, detail: {}, items: [], diffCount: 0 })
    this.loadOrders()
  },

  loadDetail() {
    // GET /api/stock/check/orders/{id}
    request('/stock/check/orders/' + this.data.checkId)
      .then((d) => {
        d = d || {}
        const items = (d.items || []).map((it) => ({
          itemId: it.id,
          isbn: it.isbn,
          bookName: it.bookName,
          bookQty: Number(it.bookQty || 0),
          realQty: Number(it.realQty || 0)
        }))
        this.setData({ detail: d, items })
        this.refreshDiff()
      })
      .catch((e) => wx.showToast({ title: e.message, icon: 'none' }))
  },

  refreshDiff() {
    const diffCount = this.data.items.filter((it) => Number(it.realQty) - Number(it.bookQty) !== 0).length
    this.setData({ diffCount })
  },

  onManual(e) { this.setData({ manualCode: e.detail.value }) },

  onQty(e) {
    const idx = e.currentTarget.dataset.index
    const val = Number(e.detail.value || 0)
    const key = 'items[' + idx + '].realQty'
    this.setData({ [key]: val })
    this.refreshDiff()
  },

  scan() {
    wx.scanCode({
      scanType: ['barCode', 'qrCode'],
      success: (res) => this.doScan(res.result),
      fail: () => {}
    })
  },

  scanManual() {
    const code = (this.data.manualCode || '').trim()
    if (!code) { wx.showToast({ title: '请输入 ISBN', icon: 'none' }); return }
    this.doScan(code)
    this.setData({ manualCode: '' })
  },

  doScan(code) {
    // POST /api/mobile/check/scan  { checkId, code, realQty }
    request('/mobile/check/scan', {
      method: 'POST',
      data: { checkId: Number(this.data.checkId), code, realQty: 1 }
    })
      .then(() => {
        wx.showToast({ title: '已录入', icon: 'success' })
        this.loadDetail()
      })
      .catch((e) => wx.showToast({ title: e.message, icon: 'none' }))
  },

  saveItems() {
    const payload = this.data.items.map((it) => ({ itemId: it.itemId, realQty: Number(it.realQty) }))
    // PUT /api/stock/check/orders/{id}/items
    request('/stock/check/orders/' + this.data.checkId + '/items', { method: 'PUT', data: { items: payload } })
      .then(() => {
        wx.showToast({ title: '已保存', icon: 'success' })
        this.loadDetail()
      })
      .catch((e) => wx.showToast({ title: e.message, icon: 'none' }))
  },

  finish() {
    wx.showModal({
      title: '完成盘点',
      content: '确认完成？系统将按差异调整库存并写入流水。',
      success: (res) => {
        if (!res.confirm) return
        // POST /api/stock/check/orders/{id}/finish
        request('/stock/check/orders/' + this.data.checkId + '/finish', { method: 'POST' })
          .then(() => {
            wx.showToast({ title: '盘点完成', icon: 'success' })
            this.backToList()
          })
          .catch((e) => wx.showToast({ title: e.message, icon: 'none' }))
      }
    })
  }
})