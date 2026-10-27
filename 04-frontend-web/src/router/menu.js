/**
 * 侧边菜单定义（按 permission 编码过滤显隐）
 * perm 为 null 表示无需权限即可见
 */
export const menuList = [
  {
    path: '/dashboard',
    title: '工作台',
    icon: 'Odometer',
    perm: null
  },
  {
    path: '/base',
    title: '基础数据',
    icon: 'Reading',
    perm: 'base',
    children: [
      { path: '/base/books', title: '图书管理', icon: 'Notebook', perm: 'base:book' },
      { path: '/base/categories', title: '分类管理', icon: 'Files', perm: 'base:category' },
      { path: '/base/publishers', title: '出版社', icon: 'OfficeBuilding', perm: 'base:publisher' },
      { path: '/base/suppliers', title: '供应商', icon: 'Van', perm: 'base:supplier' },
      { path: '/base/customers', title: '客户/会员', icon: 'User', perm: 'base:customer' },
      { path: '/base/warehouses', title: '仓库管理', icon: 'House', perm: 'base:warehouse' }
    ]
  },
  {
    path: '/purchase',
    title: '进货管理',
    icon: 'ShoppingCart',
    perm: 'purchase',
    children: [
      { path: '/purchase/orders', title: '进货单', icon: 'Document', perm: 'purchase:order' },
      { path: '/purchase/create', title: '进货开单', icon: 'EditPen', perm: 'purchase:order' },
      { path: '/purchase/returns', title: '采购退货', icon: 'RefreshLeft', perm: 'purchase:return' }
    ]
  },
  {
    path: '/sale',
    title: '出货管理',
    icon: 'Sell',
    perm: 'sale',
    children: [
      { path: '/sale/pos', title: 'POS 收银', icon: 'CreditCard', perm: 'sale:order' },
      { path: '/sale/orders', title: '出货单', icon: 'List', perm: 'sale:order' },
      { path: '/sale/returns', title: '销售退货', icon: 'RefreshRight', perm: 'sale:return' }
    ]
  },
  {
    path: '/stock',
    title: '库存管理',
    icon: 'Box',
    perm: 'stock',
    children: [
      { path: '/stock/list', title: '库存查询', icon: 'Search', perm: 'stock:list' },
      { path: '/stock/warnings', title: '库存预警', icon: 'Warning', perm: 'stock:warning' },
      { path: '/stock/checks', title: '库存盘点', icon: 'DocumentChecked', perm: 'stock:check' },
      { path: '/stock/transfers', title: '库存调拨', icon: 'Sort', perm: 'stock:transfer' },
      { path: '/stock/flows', title: '库存流水', icon: 'Tickets', perm: 'stock:flow' }
    ]
  },
  {
    path: '/stat',
    title: '查询统计',
    icon: 'DataAnalysis',
    perm: 'stat'
  },
  {
    path: '/report',
    title: '报表中心',
    icon: 'Printer',
    perm: 'report:view'
  },
  {
    path: '/order',
    title: '单据管理',
    icon: 'Lock',
    // 父级不设权限码：hasPerm 有「父级码隐含子码」规则，若这里写 'order'
    // 会让持有 order:confirm 的岗位同时获得 order:revision:approve，破坏审批隔离。
    // 子项为空时整组菜单会自动隐藏（见 layout 的 visibleMenus）。
    perm: null,
    children: [
      { path: '/order/revisions', title: '修订审批', icon: 'EditPen', perm: 'order:revision:approve' },
      { path: '/order/archive', title: '单据归档', icon: 'Files', perm: 'order:archive' }
    ]
  },
  {
    path: '/system',
    title: '系统管理',
    icon: 'Setting',
    perm: 'system',
    children: [
      { path: '/system/users', title: '用户管理', icon: 'UserFilled', perm: 'system:user' },
      { path: '/system/roles', title: '角色管理', icon: 'Avatar', perm: 'system:role' },
      { path: '/system/logs', title: '操作日志', icon: 'Memo', perm: 'system:log' }
    ]
  }
]

export default menuList