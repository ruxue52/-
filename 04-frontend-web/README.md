# 晓镜书阁 · 书店进销存管理系统（前端 Web）

基于 **Vue 3 + Vite 5 + JavaScript + Vue Router 4 + Pinia + Axios + Element Plus + ECharts 5** 的 B/S 前端。

## 环境要求

- Node.js >= 18
- npm >= 9

## 快速开始

```bash
# 安装依赖（国内镜像更快）
npm install --registry=https://registry.npmmirror.com

# 开发模式（默认 http://localhost:5173）
npm run dev

# 生产构建（输出到 dist/）
npm run build

# 预览构建产物
npm run preview
```

开发服务器已配置代理：`/api` → `http://localhost:8080`（后端服务地址）。

## 默认账号

| 账号 | 密码 |
|---|---|
| admin | 123456 |

## 目录结构

```
src/
├── api/            # Axios 封装与全部接口定义（request.js / index.js）
├── components/     # 通用组件（CrudTable / ChartBox / ReturnOrderPage）
├── layout/         # 主布局（侧边菜单 + 顶栏 + 面包屑）
├── router/         # 路由表与菜单权限配置
├── store/          # Pinia 状态（user）
├── styles/         # 全局样式
├── utils/          # 字典与格式化工具（dict.js）
└── views/          # 业务页面
```

## 页面与路由清单

| 路由 | 页面 | 说明 |
|---|---|---|
| `/login` | 登录 | 账号密码登录 |
| `/dashboard` | 工作台 | KPI 卡片 + 趋势/分类/畅销/月度图表 |
| `/base/books` | 图书管理 | 图书 CRUD、ISBN 二维码 |
| `/base/categories` | 分类管理 | 树形分类 CRUD |
| `/base/publishers` | 出版社 | CRUD |
| `/base/suppliers` | 供应商 | CRUD |
| `/base/customers` | 客户/会员 | CRUD |
| `/base/warehouses` | 仓库管理 | CRUD |
| `/purchase/orders` | 进货单 | 列表 + 提交/入库/作废 |
| `/purchase/create` | 进货开单 | 选书开单 |
| `/purchase/detail/:id` | 进货单详情 | 明细 + 日志 + 单据二维码 |
| `/purchase/returns` | 采购退货 | 退货单 |
| `/sale/pos` | POS 收银 | 扫码/搜索 → 购物车 → 结算出库 |
| `/sale/orders` | 出货单 | 列表 + 提交/出库/作废 |
| `/sale/detail/:id` | 出货单详情 | 明细 + 日志 |
| `/sale/returns` | 销售退货 | 退货单 |
| `/stock/list` | 库存查询 | 库存明细与汇总 |
| `/stock/warnings` | 库存预警 | 低库存预警 |
| `/stock/checks` | 库存盘点 | 盘点单 + 录入/完成 |
| `/stock/check/:id` | 盘点单详情 | 实盘录入、完成盘点 |
| `/stock/transfers` | 库存调拨 | 新建/执行调拨 |
| `/stock/flows` | 库存流水 | 出入库流水查询 |
| `/stat` | 查询统计 | 22 项统计（销售/采购/库存/毛利/会员/盘点） |
| `/report` | 报表中心 | R01~R15 共 15 张报表 + 导出 Excel |
| `/system/users` | 用户管理 | 用户 CRUD、启停、重置密码 |
| `/system/roles` | 角色管理 | 角色列表 + 权限树 |
| `/system/logs` | 操作日志 | 操作日志查询 |

## 约定说明

- 所有接口集中在 `src/api/index.js`，分页统一返回 `{ total, page, size, records }`。
- 响应拦截器统一解包业务数据：`code === 0` 返回 `data`，否则弹出错误；`401` 自动跳转登录。
- 字典（单据状态、支付方式、业务类型等）集中在 `src/utils/dict.js`。
- 导出文件通过 `downloadFile`（`src/api/request.js`）下载。