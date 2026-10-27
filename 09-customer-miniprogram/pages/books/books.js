// 读者端 · 图书检索（关键词 + 分类 + 排序 + 分页）
const api = require('../../utils/request')

const PAGE_SIZE = 10

Page({
  data: {
    keyword: '',
    categories: [],
    subCats: [],
    categoryId: null,
    categoryName: '',
    sort: 'default',
    list: [],
    page: 1,
    size: PAGE_SIZE,
    total: 0,
    hasMore: false,
    loading: false
  },

  onLoad(options) {
    const patch = {}
    if (options.keyword) patch.keyword = decodeURIComponent(options.keyword)
    if (options.categoryId) {
      patch.categoryId = Number(options.categoryId)
      patch.categoryName = options.categoryName ? decodeURIComponent(options.categoryName) : ''
    }
    this.setData(patch)
    this.loadCategories()
  },

  onReachBottom() {
    this.load(this.data.page + 1)
  },

  loadCategories() {
    api.request('/reader/categories')
      .then((list) => {
        this.setData({ categories: list || [] })
        this.applySubCats()
        this.load(1)
      })
      .catch((e) => {
        wx.showToast({ title: e.message, icon: 'none' })
        this.load(1)
      })
  },

  applySubCats() {
    const cur = (this.data.categories || []).find((c) => c.id === this.data.categoryId)
    this.setData({ subCats: cur ? cur.children || [] : [] })
  },

  load(page) {
    if (this.data.loading) return
    this.setData({ loading: true })
    api.request('/reader/books', {
      params: {
        page,
        size: this.data.size,
        keyword: this.data.keyword,
        categoryId: this.data.categoryId,
        sort: this.data.sort === 'newest' ? 'newest' : ''
      }
    })
      .then((data) => {
        const records = decorate(api.recordsOf(data))
        const list = page === 1 ? records : this.data.list.concat(records)
        this.setData({
          list,
          page,
          total: data ? data.total : 0,
          hasMore: list.length < (data ? data.total : 0)
        })
        if (page === 1) wx.pageScrollTo({ scrollTop: 0, duration: 0 })
      })
      .catch((e) => wx.showToast({ title: e.message, icon: 'none' }))
      .then(() => this.setData({ loading: false }))
  },

  onKeyword(e) {
    this.setData({ keyword: e.detail.value })
  },

  onSearch() {
    this.load(1)
  },

  onClearKeyword() {
    this.setData({ keyword: '' })
    this.load(1)
  },

  pickCategory(e) {
    const id = e.currentTarget.dataset.id
    const name = e.currentTarget.dataset.name || ''
    const same = this.data.categoryId === id
    this.setData({
      categoryId: same ? null : id,
      categoryName: same ? '' : name
    })
    this.applySubCats()
    this.load(1)
  },

  pickSubCategory(e) {
    const id = e.currentTarget.dataset.id
    const name = e.currentTarget.dataset.name || ''
    const same = this.data.categoryId === id
    this.setData({ categoryId: same ? null : id, categoryName: same ? '' : name })
    this.load(1)
  },

  pickSort(e) {
    const sort = e.currentTarget.dataset.sort
    if (sort === this.data.sort) return
    this.setData({ sort })
    this.load(1)
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id })
  }
})

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
