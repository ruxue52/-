// 晓镜书阁 · 读者端请求封装（wx.request + 会员 token 拦截）
// 后端接口：/api/reader/**（见 01-docs/09-接口设计.md 读者端章节）

function app() {
  return getApp() || {}
}

function baseUrl() {
  return (app().globalData && app().globalData.baseUrl) || 'http://localhost:8080/api'
}

function buildQuery(params) {
  if (!params) return ''
  const arr = []
  Object.keys(params).forEach((k) => {
    const v = params[k]
    if (v !== undefined && v !== null && v !== '') {
      arr.push(encodeURIComponent(k) + '=' + encodeURIComponent(v))
    }
  })
  return arr.length ? '?' + arr.join('&') : ''
}

/**
 * 统一请求
 * @param {String} path 如 '/reader/books'
 * @param {Object} opt  { method, data, params }
 * @returns Promise，resolve 为后端 data 字段
 */
function request(path, opt) {
  opt = opt || {}
  const url = baseUrl() + path + buildQuery(opt.params)
  const header = { 'Content-Type': 'application/json' }
  const token = (app().getToken && app().getToken()) || wx.getStorageSync('shg_member_token') || ''
  if (token) header['Authorization'] = 'Bearer ' + token

  return new Promise((resolve, reject) => {
    wx.request({
      url: url,
      method: opt.method || 'GET',
      data: opt.data || {},
      header: header,
      timeout: 15000,
      success: (res) => {
        if (res.statusCode === 401) {
          if (app().clearAuth) app().clearAuth()
          wx.showToast({ title: '请先登录会员账号', icon: 'none' })
          setTimeout(() => wx.navigateTo({ url: '/pages/login/login' }), 800)
          reject(new Error('未登录或登录已过期'))
          return
        }
        const body = res.data || {}
        if (body.code !== 0) {
          reject(new Error(body.message || '请求失败'))
          return
        }
        resolve(body.data)
      },
      fail: () => reject(new Error('无法连接后端：' + baseUrl()))
    })
  })
}

function recordsOf(data) {
  if (!data) return []
  if (Array.isArray(data)) return data
  return data.records || []
}

function fmtMoney(v) {
  if (v === undefined || v === null || v === '') return '0.00'
  return Number(v).toFixed(2)
}

function fmtDate(v) {
  if (!v) return '-'
  return String(v).replace('T', ' ').substring(0, 16)
}

/** 图书售价：优先售价，缺失时回退定价 */
function salePriceOf(book) {
  if (!book) return '0.00'
  const v = book.salePrice === undefined || book.salePrice === null ? book.price : book.salePrice
  return fmtMoney(v)
}

/** 解析扫码结果中的 ISBN，兼容本系统二维码 SHG:BOOK:<isbn> 等格式 */
function extractIsbn(code) {
  if (!code) return ''
  code = String(code).trim()
  if (code.indexOf('SHG:BOOK:') === 0) return code.substring('SHG:BOOK:'.length)
  if (code.indexOf('SHG:') === 0) {
    const p = code.split(':')
    return p[p.length - 1]
  }
  return code
}

/** 无封面时的占位字（书名首字） */
function coverText(name) {
  return name ? String(name).trim().charAt(0) : '书'
}

module.exports = { request, recordsOf, fmtMoney, fmtDate, salePriceOf, extractIsbn, coverText, baseUrl }
