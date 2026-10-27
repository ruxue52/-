// 晓镜书阁 · 微信小程序请求封装（wx.request + token 拦截）
// 接口契约：01-docs/09-接口设计.md，基础路径 /api

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
 * 统一请求方法
 * @param {String} path 如 '/auth/login'
 * @param {Object} opt  { method, data, params }
 * @returns Promise，resolve 为 data 字段
 */
function request(path, opt) {
  opt = opt || {}
  const url = baseUrl() + path + buildQuery(opt.params)
  const header = { 'Content-Type': 'application/json' }
  const token = (app().getToken && app().getToken()) || wx.getStorageSync('shg_token') || ''
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
          wx.showToast({ title: '登录已过期', icon: 'none' })
          setTimeout(() => wx.reLaunch({ url: '/pages/login/login' }), 800)
          reject(new Error('未登录或登录已过期'))
          return
        }
        if (res.statusCode === 403) { reject(new Error('无访问权限')); return }
        const body = res.data || {}
        if (body.code !== 0) { reject(new Error(body.message || '请求失败')); return }
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

function fmtTime(v) {
  return v ? String(v).replace('T', ' ').substring(0, 19) : '-'
}

function extractIsbn(code) {
  if (!code) return ''
  code = String(code).trim()
  if (code.indexOf('SHG:BOOK:') === 0) return code.substring('SHG:BOOK:'.length)
  if (code.indexOf('SHG:STOCK:') === 0) {
    const p = code.split(':')
    return p.length >= 4 ? p[3] : ''
  }
  if (code.indexOf('SHG:CHECK:') === 0) {
    const p = code.split(':')
    return p.length >= 4 ? p[3] : ''
  }
  return code
}

module.exports = { request, recordsOf, fmtMoney, fmtTime, extractIsbn, baseUrl }