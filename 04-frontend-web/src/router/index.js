import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const Layout = () => import('@/layout/index.vue')

export const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/home/index.vue'),
    meta: { title: '首页', public: true }
  },
  {
    path: '/dashboard',
    component: Layout,
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'Odometer' }
      }
    ]
  },
  {
    path: '/base',
    component: Layout,
    meta: { title: '基础数据' },
    children: [
      { path: 'books', name: 'Books', component: () => import('@/views/base/books.vue'), meta: { title: '图书管理' } },
      { path: 'categories', name: 'Categories', component: () => import('@/views/base/categories.vue'), meta: { title: '分类管理' } },
      { path: 'publishers', name: 'Publishers', component: () => import('@/views/base/publishers.vue'), meta: { title: '出版社' } },
      { path: 'suppliers', name: 'Suppliers', component: () => import('@/views/base/suppliers.vue'), meta: { title: '供应商' } },
      { path: 'customers', name: 'Customers', component: () => import('@/views/base/customers.vue'), meta: { title: '客户/会员' } },
      { path: 'warehouses', name: 'Warehouses', component: () => import('@/views/base/warehouses.vue'), meta: { title: '仓库管理' } }
    ]
  },
  {
    path: '/purchase',
    component: Layout,
    meta: { title: '进货管理' },
    children: [
      { path: 'orders', name: 'PurchaseOrders', component: () => import('@/views/purchase/orders.vue'), meta: { title: '进货单' } },
      { path: 'create', name: 'PurchaseCreate', component: () => import('@/views/purchase/create.vue'), meta: { title: '进货开单' } },
      { path: 'detail/:id', name: 'PurchaseDetail', component: () => import('@/views/purchase/detail.vue'), meta: { title: '进货单详情', hidden: true } },
      { path: 'returns', name: 'PurchaseReturns', component: () => import('@/views/purchase/returns.vue'), meta: { title: '采购退货' } }
    ]
  },
  {
    path: '/sale',
    component: Layout,
    meta: { title: '出货管理' },
    children: [
      { path: 'pos', name: 'SalePos', component: () => import('@/views/sale/pos.vue'), meta: { title: 'POS 收银' } },
      { path: 'orders', name: 'SaleOrders', component: () => import('@/views/sale/orders.vue'), meta: { title: '出货单' } },
      { path: 'detail/:id', name: 'SaleDetail', component: () => import('@/views/sale/detail.vue'), meta: { title: '出货单详情', hidden: true } },
      { path: 'returns', name: 'SaleReturns', component: () => import('@/views/sale/returns.vue'), meta: { title: '销售退货' } }
    ]
  },
  {
    path: '/stock',
    component: Layout,
    meta: { title: '库存管理' },
    children: [
      { path: 'list', name: 'StockList', component: () => import('@/views/stock/list.vue'), meta: { title: '库存查询' } },
      { path: 'warnings', name: 'StockWarnings', component: () => import('@/views/stock/warnings.vue'), meta: { title: '库存预警' } },
      { path: 'checks', name: 'StockChecks', component: () => import('@/views/stock/checks.vue'), meta: { title: '库存盘点' } },
      { path: 'check/:id', name: 'StockCheckDetail', component: () => import('@/views/stock/checkDetail.vue'), meta: { title: '盘点单详情', hidden: true } },
      { path: 'transfers', name: 'StockTransfers', component: () => import('@/views/stock/transfers.vue'), meta: { title: '库存调拨' } },
      { path: 'flows', name: 'StockFlows', component: () => import('@/views/stock/flows.vue'), meta: { title: '库存流水' } }
    ]
  },
  {
    path: '/stat',
    component: Layout,
    children: [
      { path: '', name: 'Stat', component: () => import('@/views/stat/index.vue'), meta: { title: '查询统计' } }
    ]
  },
  {
    path: '/report',
    component: Layout,
    children: [
      { path: '', name: 'Report', component: () => import('@/views/report/index.vue'), meta: { title: '报表中心' } }
    ]
  },
  {
    path: '/order',
    component: Layout,
    meta: { title: '单据管理' },
    children: [
      { path: 'revisions', name: 'OrderRevisions', component: () => import('@/views/order/revisions.vue'), meta: { title: '修订审批' } },
      { path: 'archive', name: 'OrderArchive', component: () => import('@/views/order/archive.vue'), meta: { title: '单据归档' } }
    ]
  },
  {
    path: '/system',
    component: Layout,
    meta: { title: '系统管理' },
    children: [
      { path: 'users', name: 'SystemUsers', component: () => import('@/views/system/users.vue'), meta: { title: '用户管理' } },
      { path: 'roles', name: 'SystemRoles', component: () => import('@/views/system/roles.vue'), meta: { title: '角色管理' } },
      { path: 'logs', name: 'SystemLogs', component: () => import('@/views/system/logs.vue'), meta: { title: '操作日志' } }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', public: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  document.title = (to.meta && to.meta.title ? to.meta.title + ' · ' : '') + '晓镜书阁'

  if (to.meta && to.meta.public) {
    if (to.path === '/login' && userStore.isLogin) {
      next({ path: '/dashboard' })
      return
    }
    next()
    return
  }

  if (!userStore.isLogin) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  next()
})

export default router