// 读者端 · 图书详情（含各门店在架数量）
const api = require('../../utils/request')

Page({
  data: {
    loading: true,
    error: '',
    book: null,
    stock: [],
    totalQuantity: 0,
    available: false,
    member: null
  },

  onLoad(options) {
    const member = getApp().getMember()
    this.setData({ member: member && member.discount ? member : null })

    if (options.isbn) {
      this.fetch('/reader/books/by-isbn/' + encodeURIComponent(decodeURIComponent(options.isbn)))
    } else if (options.id) {
      this.fetch('/reader/books/' + options.id)
    } else {
      this.setData({ loading: false, error: '缺少图书参数' })
    }
  },

  fetch(path) {
    this.setData({ loading: true, error: '' })
    api.request(path)
      .then((data) => {
        const book = data.book || {}
        wx.setNavigationBarTitle({ title: book.bookName || '图书详情' })
        this.setData({
          book: {
            ...book,
            priceText: api.salePriceOf(book),
            priceOld: book.price ? api.fmtMoney(book.price) : '',
            coverText: api.coverText(book.bookName)
          },
          stock: (data.stock || []).map((s) => ({ ...s, qtyText: api.fmtMoney(s.quantity) })),
          totalQuantity: data.totalQuantity,
          available: !!data.available
        })
      })
      .catch((e) => this.setData({ error: e.message }))
      .then(() => this.setData({ loading: false }))
  },

  goScan() {
    wx.switchTab({ url: '/pages/scan/scan' })
  },

  goMember() {
    wx.switchTab({ url: '/pages/member/member' })
  }
})
