import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/store/user'

// 创建 axios 实例
const service = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截器：附加 JWT
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = 'Bearer ' + userStore.token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理 { code, message, data }
service.interceptors.response.use(
  (response) => {
    // 文件流（导出）直接返回原始响应
    const respType = response.config.responseType
    if (respType === 'blob' || respType === 'arraybuffer') {
      return response
    }

    const res = response.data
    // 非标准结构（如后端直返数据）直接放行
    if (res === null || typeof res !== 'object' || !('code' in res)) {
      return res
    }

    if (res.code === 0) {
      return res.data
    }

    // 业务错误
    ElMessage.error(res.message || '请求失败')
    if (res.code === 401) {
      handleUnauthorized()
    }
    return Promise.reject(new Error(res.message || 'Error'))
  },
  (error) => {
    const status = error.response && error.response.status
    const msg =
      (error.response && error.response.data && error.response.data.message) ||
      error.message ||
      '网络异常'

    if (status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      handleUnauthorized()
    } else if (status === 403) {
      ElMessage.error('无访问权限')
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

function handleUnauthorized() {
  try {
    const userStore = useUserStore()
    userStore.clear()
  } catch (e) {
    // store 尚未初始化时忽略
  }
  if (router.currentRoute.value.path !== '/login') {
    router.replace({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
  }
}

export default service

/**
 * 文件下载（导出 Excel/CSV）
 */
export function downloadFile(url, params) {
  return service.get(url, { params, responseType: 'blob' }).then((response) => {
    const blob = new Blob([response.data])
    const disposition = response.headers['content-disposition'] || ''
    let filename = 'export.xlsx'
    const match = /filename\*?=(?:UTF-8'')?["']?([^"';]+)/i.exec(disposition)
    if (match && match[1]) {
      filename = decodeURIComponent(match[1])
    }
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(link.href)
  })
}