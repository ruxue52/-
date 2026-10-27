// 读者端 · 书房首页：分类入口 + 新书上架
const api = require('../../utils/request')

Page({
  data: {
    keyword: '',
    categories: [],
    books: [],
    paging: { page: 1, size: 6 },
    loading: false
  },

  onLoad() {
    this.loadCategories()
    this.loadBooks()
  },

  loadCategories() {
    api.request('/reader/categories')
      .then((list) => this.setData({ categories: list || [] }))
      .catch((e) => console.warn('分类加载失败', e.message))
  },

  loadBooks() {
    this.setData({ loading: true })
    api.request('/reader/books', { params: { page: 1, size: 6, sort: 'newest' } })
      .then((data) => this.setData({ books: decorate(api.recordsOf(data)) }))
      .catch((e) => wx.showToast({ title: e.message, icon: 'none' }))
      .then(() => this.setData({ loading: false }))
  },

  onKeyword(e) {
    this.setData({ keyword: e.detail.value })
  },

  onSearch() {
    const kw = (this.data.keyword || '').trim()
    wx.navigateTo({ url: '/pages/books/books' + (kw ? '?keyword=' + encodeURIComponent(kw) : '') })
  },

  goCategory(e) {
    const { id, name } = e.currentTarget.dataset
    wx.navigateTo({
      url: '/pages/books/books?categoryId=' + id + '&categoryName=' + encodeURIComponent(name || '')
    })
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id })
  },

  goScan() {
    wx.switchTab({ url: '/pages/scan/scan' })
  },

  goMember() {
    wx.switchTab({ url: '/pages/member/member' })
  }
})

/** 列表展示字段：售价文本、封面占位字与配色 */
function decorate(records) {
  const skins = ['', 'gold', 'ink']
  return (records || []).map((b) => ({
    ...b,
    priceText: api.salePriceOf(b),
    priceOld: b.price ? api.fmtMoney(b.price) : '',
    coverText: api.coverText(b.bookName),
    coverSkin: skins[(b.id || 0) % skins.length]
  }))
}
