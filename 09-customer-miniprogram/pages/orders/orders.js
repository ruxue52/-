// 读者端 · 我的消费记录（列表 + 展开明细）
const api = require('../../utils/request')

const PAGE_SIZE = 10

const PAYMENT_TYPES = {
  CASH: '现金',
  WECHAT: '微信',
  ALIPAY: '支付宝',
  CARD: '银行卡',
  CREDIT: '赊账',
  TRANSFER: '转账'
}

Page({
  data: {
    list: [],
    page: 1,
    size: PAGE_SIZE,
    total: 0,
    hasMore: false,
    loading: false
  },

  onLoad() {
    getApp().requireLogin()
  },

  onShow() {
    if (getApp().isLogin() && !this.inited) {
      this.inited = true
      this.load(1)
    }
  },

  onReachBottom() {
    if (this.data.hasMore) this.load(this.data.page + 1)
  },

  load(page) {
    if (this.data.loading) return
    this.setData({ loading: true })
    api.request('/reader/member/orders', { params: { page, size: this.data.size } })
      .then((data) => {
        const records = (api.recordsOf(data) || []).map((o) => ({
          ...o,
          timeText: api.fmtDate(o.outTime || o.createTime),
          qtyText: api.fmtMoney(o.totalQty),
          payText: api.fmtMoney(o.payAmount),
          amountText: api.fmtMoney(o.totalAmount),
          paymentText: PAYMENT_TYPES[o.paymentType] || o.paymentType || '-',
          items: null,
          open: false
        }))
        const list = page === 1 ? records : this.data.list.concat(records)
        this.setData({
          list,
          page,
          total: data ? data.total : 0,
          hasMore: list.length < (data ? data.total : 0)
        })
      })
      .catch((e) => wx.showToast({ title: e.message, icon: 'none' }))
      .then(() => this.setData({ loading: false }))
  },

  toggle(e) {
    const id = e.currentTarget.dataset.id
    const idx = this.data.list.findIndex((o) => o.id === id)
    if (idx < 0) return
    const order = this.data.list[idx]

    if (order.items) {
      this.setData({ ['list[' + idx + '].open']: !order.open })
      return
    }
    api.request('/reader/member/orders/' + id)
      .then((data) => {
        const items = (data.items || []).map((it) => ({
          ...it,
          priceText: api.fmtMoney(it.price),
          amountText: api.fmtMoney(it.amount),
          qtyText: api.fmtMoney(it.quantity)
        }))
        this.setData({
          ['list[' + idx + '].items']: items,
          ['list[' + idx + '].open']: true
        })
      })
      .catch((err) => wx.showToast({ title: err.message, icon: 'none' }))
  },

  goBooks() {
    wx.switchTab({ url: '/pages/index/index' })
  }
})
