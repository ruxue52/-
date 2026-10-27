/**
 * 枚举字典与格式化工具
 */

// 单据状态
export const ORDER_STATUS = {
  DRAFT: { label: '草稿', type: 'info' },
  SUBMITTED: { label: '已提交', type: 'warning' },
  INSTOCK: { label: '已入库', type: 'success' },
  OUTSTOCK: { label: '已出库', type: 'success' },
  FINISHED: { label: '已完成', type: 'success' },
  PENDING: { label: '待处理', type: 'warning' },
  TRANSFERRED: { label: '已调拨', type: 'success' },
  CANCELLED: { label: '已作废', type: 'danger' }
}

// 支付方式
export const PAYMENT_TYPE = {
  CASH: '现金',
  WECHAT: '微信',
  ALIPAY: '支付宝',
  CARD: '银行卡',
  CREDIT: '赊账'
}

// 结算方式
export const SETTLE_TYPE = {
  CASH: '现结',
  CREDIT: '账期',
  PERIOD: '月结'
}

// 客户类型
export const CUSTOMER_TYPE = {
  RETAIL: { label: '零售', type: '' },
  WHOLESALE: { label: '批发', type: 'warning' },
  MEMBER: { label: '会员', type: 'success' }
}

// 单据来源
export const SOURCE_TYPE = {
  PC: '电脑端',
  APP: 'APP',
  MINI: '小程序',
  SCAN: '扫码'
}

// 盘点类型
export const CHECK_TYPE = {
  FULL: '全盘',
  PART: '抽盘',
  CYCLE: '循环盘'
}

// 退货类型
export const RETURN_TYPE = {
  PURCHASE: '采购退货',
  SALES: '销售退货'
}

// 库存流水业务类型
export const BIZ_TYPE = {
  IN: '采购入库',
  OUT: '销售出库',
  CHECK: '盘点调整',
  TRANSFER_IN: '调拨入',
  TRANSFER_OUT: '调拨出',
  RET_PUR: '采购退货',
  RET_SAL: '销售退货',
  INIT: '期初'
}

// 用户/通用状态
export const ENABLE_STATUS = {
  1: { label: '启用', type: 'success' },
  0: { label: '停用', type: 'danger' }
}

export function statusLabel(map, key) {
  const item = map[key]
  if (!item) return key || '-'
  return typeof item === 'string' ? item : item.label
}

export function statusType(map, key) {
  const item = map[key]
  if (!item || typeof item === 'string') return 'info'
  return item.type
}

export function formatMoney(val) {
  if (val === null || val === undefined || val === '') return '0.00'
  const n = Number(val)
  if (Number.isNaN(n)) return String(val)
  return n.toFixed(2)
}

export function formatQty(val) {
  if (val === null || val === undefined || val === '') return '0'
  const n = Number(val)
  if (Number.isNaN(n)) return String(val)
  return Number.isInteger(n) ? String(n) : String(n)
}

export function formatDate(val) {
  if (!val) return '-'
  return String(val).replace('T', ' ').slice(0, 19)
}

export function formatDay(val) {
  if (!val) return '-'
  return String(val).slice(0, 10)
}

/** 通用选项转换 */
export function toOptions(map) {
  return Object.entries(map).map(([value, v]) => ({
    value,
    label: typeof v === 'string' ? v : v.label
  }))
}