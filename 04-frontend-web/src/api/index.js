import request from './request'

/* ============ 认证与用户 ============ */
export const authApi = {
  login: (data) => request.post('/auth/login', data),
  me: () => request.get('/auth/me'),
  logout: () => request.post('/auth/logout')
}

/* ============ 基础数据（统一 CRUD） ============ */
function crud(base) {
  return {
    list: (params) => request.get(base, { params }),
    detail: (id) => request.get(`${base}/${id}`),
    create: (data) => request.post(base, data),
    update: (id, data) => request.put(`${base}/${id}`, data),
    remove: (id) => request.delete(`${base}/${id}`)
  }
}

export const bookApi = {
  ...crud('/books'),
  byIsbn: (isbn) => request.get(`/books/by-isbn/${isbn}`)
}
export const categoryApi = crud('/categories')
export const publisherApi = crud('/publishers')
export const supplierApi = crud('/suppliers')
export const customerApi = crud('/customers')
export const memberLevelApi = crud('/member-levels')
export const warehouseApi = crud('/warehouses')

/* ============ 进货管理 ============ */
export const purchaseApi = {
  list: (params) => request.get('/purchase/orders', { params }),
  detail: (id) => request.get(`/purchase/orders/${id}`),
  create: (data) => request.post('/purchase/orders', data),
  update: (id, data) => request.put(`/purchase/orders/${id}`, data),
  remove: (id) => request.delete(`/purchase/orders/${id}`),
  submit: (id) => request.post(`/purchase/orders/${id}/submit`),
  instock: (id) => request.post(`/purchase/orders/${id}/instock`),
  cancel: (id) => request.post(`/purchase/orders/${id}/cancel`),
  logs: (id) => request.get(`/purchase/orders/${id}/logs`)
}

/* ============ 出货管理 ============ */
export const saleApi = {
  list: (params) => request.get('/sale/orders', { params }),
  detail: (id) => request.get(`/sale/orders/${id}`),
  create: (data) => request.post('/sale/orders', data),
  update: (id, data) => request.put(`/sale/orders/${id}`, data),
  remove: (id) => request.delete(`/sale/orders/${id}`),
  submit: (id) => request.post(`/sale/orders/${id}/submit`),
  outstock: (id) => request.post(`/sale/orders/${id}/outstock`),
  cancel: (id) => request.post(`/sale/orders/${id}/cancel`),
  posSearch: (keyword) => request.get('/sale/pos/search', { params: { keyword } })
}

/* ============ 库存管理 ============ */
export const stockApi = {
  list: (params) => request.get('/stock/list', { params }),
  byBook: (bookId) => request.get(`/stock/by-book/${bookId}`),
  warnings: (params) => request.get('/stock/warnings', { params }),
  summary: () => request.get('/stock/summary'),
  flows: (params) => request.get('/stock/flows', { params }),
  // 盘点
  checkList: (params) => request.get('/stock/check/orders', { params }),
  checkDetail: (id) => request.get(`/stock/check/orders/${id}`),
  checkCreate: (data) => request.post('/stock/check/orders', data),
  checkSaveItems: (id, data) => request.put(`/stock/check/orders/${id}/items`, data),
  checkFinish: (id) => request.post(`/stock/check/orders/${id}/finish`),
  checkCancel: (id) => request.post(`/stock/check/orders/${id}/cancel`),
  // 调拨
  transferCreate: (data) => request.post('/stock/transfer/orders', data),
  transferUpdate: (id, data) => request.put(`/stock/transfer/orders/${id}`, data),
  transferExec: (id) => request.post(`/stock/transfer/orders/${id}/transfer`),
  transferList: (params) => request.get('/stock/transfer/orders', { params }),
  transferDetail: (id) => request.get(`/stock/transfer/orders/${id}`),
  // 退货
  returnCreate: (data) => request.post('/returns', data),
  returnUpdate: (id, data) => request.put(`/returns/${id}`, data),
  returnFinish: (id) => request.post(`/returns/${id}/finish`),
  returnList: (params) => request.get('/returns', { params }),
  returnDetail: (id) => request.get(`/returns/${id}`)
}

/* ============ 单据确认锁定与修订审批 ============ */
export const orderDocApi = {
  // orderType: IN / OUT / CHECK / TRANSFER / RETURN
  logs: (orderType, id) => request.get(`/orders/${orderType}/${id}/logs`),
  revisions: (orderType, id) => request.get(`/orders/${orderType}/${id}/revisions`),
  confirm: (orderType, id) => request.post(`/orders/${orderType}/${id}/confirm`),
  applyRevision: (orderType, id, reason) =>
    request.post(`/orders/${orderType}/${id}/revision`, { reason }),
  pendingRevisions: (params) => request.get('/orders/revisions/pending', { params }),
  approve: (revisionId, remark) => request.post(`/orders/revisions/${revisionId}/approve`, { remark }),
  reject: (revisionId, remark) => request.post(`/orders/revisions/${revisionId}/reject`, { remark }),
  cancel: (revisionId) => request.post(`/orders/revisions/${revisionId}/cancel`),
  // 归档
  archive: (orderType, id) => request.post(`/orders/${orderType}/${id}/archive`),
  archiveSummary: (params) => request.get('/orders/archive/summary', { params }),
  archiveBatch: (orderType, keepDays) =>
    request.post('/orders/archive/batch', { orderType, keepDays }),
  archivedList: (orderType, params) => request.get(`/orders/${orderType}/archived`, { params })
}

/* ============ 查询统计 ============ */
export const statApi = {
  overview: () => request.get('/stat/overview'),
  salesTrend: (params) => request.get('/stat/sales/trend', { params }),
  salesMonthly: (params) => request.get('/stat/sales/monthly', { params }),
  salesTopBooks: (params) => request.get('/stat/sales/top-books', { params }),
  salesByCategory: (params) => request.get('/stat/sales/by-category', { params }),
  salesByPublisher: (params) => request.get('/stat/sales/by-publisher', { params }),
  salesByWarehouse: (params) => request.get('/stat/sales/by-warehouse', { params }),
  salesByPayment: (params) => request.get('/stat/sales/by-payment', { params }),
  salesByOperator: (params) => request.get('/stat/sales/by-operator', { params }),
  salesHour: (params) => request.get('/stat/sales/hour', { params }),
  purchaseTrend: (params) => request.get('/stat/purchase/trend', { params }),
  purchaseBySupplier: (params) => request.get('/stat/purchase/by-supplier', { params }),
  purchaseTopBooks: (params) => request.get('/stat/purchase/top-books', { params }),
  stockValueByCategory: () => request.get('/stat/stock/value-by-category'),
  stockTurnover: (params) => request.get('/stat/stock/turnover', { params }),
  stockAge: () => request.get('/stat/stock/age'),
  profitByBook: (params) => request.get('/stat/profit/by-book', { params }),
  profitMonthly: (params) => request.get('/stat/profit/monthly', { params }),
  customerConsumeRank: (params) => request.get('/stat/customer/consume-rank', { params }),
  customerActive: (params) => request.get('/stat/customer/active', { params }),
  memberLevelDistribution: () => request.get('/stat/member/level-distribution'),
  checkDiffSummary: (params) => request.get('/stat/check/diff-summary', { params })
}

/* ============ 统计报表 ============ */
export const reportApi = {
  get: (code, params) => request.get(`/report/${code}`, { params }),
  exportUrl: (code, params) => {
    const qs = new URLSearchParams(
      Object.entries(params || {}).filter(([, v]) => v !== undefined && v !== null && v !== '')
    ).toString()
    return `/api/report/${code}/export?format=xlsx${qs ? '&' + qs : ''}`
  }
}

/* ============ 系统管理 ============ */
export const systemApi = {
  userList: (params) => request.get('/users', { params }),
  userCreate: (data) => request.post('/users', data),
  userUpdate: (id, data) => request.put(`/users/${id}`, data),
  userRemove: (id) => request.delete(`/users/${id}`),
  userStatus: (id, status) => request.put(`/users/${id}/status`, { status }),
  userResetPwd: (id, password) => request.put(`/users/${id}/password`, { password }),
  roleList: (params) => request.get('/roles', { params }),
  rolePermUpdate: (id, permissionIds) =>
    request.put(`/roles/${id}/permissions`, { permissionIds }),
  permTree: () => request.get('/permissions/tree'),
  logList: (params) => request.get('/logs', { params }),
  dictItems: (code) => request.get(`/dicts/${code}/items`)
}

/* ============ 二维码 / 扫码 ============ */
export const qrcodeApi = {
  book: (isbn) => `/api/qrcode/book/${isbn}`,
  order: (orderNo) => `/api/qrcode/order/${orderNo}`,
  content: (code) => request.get(`/qrcode/content/${code}`),
  resolve: (data) => request.post('/scan/resolve', data),
  log: (data) => request.post('/scan/log', data)
}