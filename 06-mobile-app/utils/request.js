// 晓镜书阁 · 移动端 —— 请求封装（uni.request + token 拦截）
// 接口契约：01-docs/09-接口设计.md，基础路径 /api

// 后端基地址：H5 预览走 vite 代理（/api），App 端直连后端
// 真机调试时请改为电脑局域网 IP（手机与电脑需同一网络）
export const BASE_URL = process.env.UNI_PLATFORM === 'h5'
  ? '/api'
  : 'http://localhost:8080/api'

const TOKEN_KEY = 'shg_token'
const USER_KEY = 'shg_user'

export function getToken() {
  return uni.getStorageSync(TOKEN_KEY) || ''
}
export function getUser() {
  return uni.getStorageSync(USER_KEY) || null
}
export function setAuth(token, userInfo) {
  uni.setStorageSync(TOKEN_KEY, token || '')
  uni.setStorageSync(USER_KEY, userInfo || null)
}
export function clearAuth() {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
}

export function requireLogin() {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/login' })
    return false
  }
  return true
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
 * @param {String} path  如 '/auth/login'
 * @param {Object} opt   { method, data, params }
 * @returns Promise，resolve 为 data 字段
 */
export function request(path, opt = {}) {
  const url = BASE_URL + path + buildQuery(opt.params)
  const header = { 'Content-Type': 'application/json' }
  const token = getToken()
  if (token) header['Authorization'] = 'Bearer ' + token

  return new Promise((resolve, reject) => {
    uni.request({
      url: url,
      method: opt.method || 'GET',
      data: opt.data || {},
      header: header,
      timeout: 15000,
      success: (res) => {
        if (res.statusCode === 401) {
          clearAuth()
          uni.showToast({ title: '登录已过期', icon: 'none' })
          setTimeout(() => uni.reLaunch({ url: '/pages/login/login' }), 800)
          reject(new Error('未登录或登录已过期'))
          return
        }
        if (res.statusCode === 403) {
          reject(new Error('无访问权限'))
          return
        }
        const body = res.data || {}
        if (body.code !== 0) {
          reject(new Error(body.message || '请求失败'))
          return
        }
        resolve(body.data)
      },
      fail: () => {
        reject(new Error('无法连接后端：' + BASE_URL))
      }
    })
  })
}

// 分页响应兼容：取 records 数组
export function recordsOf(data) {
  if (!data) return []
  if (Array.isArray(data)) return data
  return data.records || []
}

export function fmtMoney(v) {
  if (v === undefined || v === null || v === '') return '0.00'
  return Number(v).toFixed(2)
}

export function fmtTime(v) {
  return v ? String(v).replace('T', ' ').substring(0, 19) : '-'
}

// 从扫码内容中提取 ISBN（兼容 SHG:BOOK / SHG:STOCK / SHG:CHECK 规范）
export function extractIsbn(code) {
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

export default { request, BASE_URL, getToken, getUser, setAuth, clearAuth, requireLogin, recordsOf, fmtMoney, fmtTime, extractIsbn }