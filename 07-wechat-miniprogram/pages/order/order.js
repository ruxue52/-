// 晓镜书阁 · 单据查询页
// 接口：GET /api/sale/orders、GET /api/purchase/orders（含 /{id} 详情）
const { request, recordsOf, fmtMoney, fmtTime } = require('../../utils/request.js')
const app = getApp()

Page({
  data: {
    tab: 'sale',
    list: [],
    detail: null
  },

  onShow() {
    if (!app.requireLogin()) return
    this.load()
  },

  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab })
    this.load()
  },

  load() {
    const path = this.data.tab === 'sale' ? '/sale/orders' : '/purchase/orders'
    request(path, { params: { page: 1, size: 30, sort: 'createTime,desc' } })
      .then((data) => {
        const list = recordsOf(data).map((o) => ({
          id: o.id,
          orderNo: o.orderNo,
          warehouseId: o.warehouseId,
          warehouseName: o.warehouseName,
          status: o.status,
          payAmount: fmtMoney(o.payAmount),
          customerName: o.customerName,
          supplierName: o.supplierName,
          createTime: fmtTime(o.createTime)
        }))
        this.setData({ list })
      })
      .catch((e) => {
        wx.showToast({ title: e.message, icon: 'none' })
        this.setData({ list: [] })
      })
  },

  openDetail(e) {
    const id = e.currentTarget.dataset.id
    const path = this.data.tab === 'sale' ? '/sale/orders/' : '/purchase/orders/'
    request(path + id)
      .then((d) => {
        d = d || {}
        d.createTime = fmtTime(d.createTime)
        d.totalAmount = fmtMoney(d.totalAmount)
        d.payAmount = fmtMoney(d.payAmount)
        ;(d.items || []).forEach((it) => { it.amount = fmtMoney(it.amount) })
        this.setData({ detail: d })
      })
      .catch((e) => wx.showToast({ title: e.message, icon: 'none' }))
  },

  closeDetail() { this.setData({ detail: null }) },
  noop() {}
})