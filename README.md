# 晓镜书阁 · 书店进销存管理系统（Bookstore PSI System）

> 面向书店行业的**进销存（采购-销售-库存）一体化管理系统**
> 课程/实练项目：包含**行业分析 + 系统分析与设计（文档）** 与 **系统实现（多端）** 两大部分。

---

## 一、项目简介

本项目以「书店」行业为背景，完成从**行业分析 → 需求分析 → 系统设计 → 编码实现 → 部署适配**的完整软件工程过程，并交付：

- **一套分析设计文档**：行业分析、32+ 用例、UML 全套图（用例/类/活动/时序/状态/组件/部署/ER）、22 项查询统计、15 张报表、技术选型报告、软件架构设计、数据库设计、信创适配方案。
- **一套可运行的 B/S 进销存系统**：Spring Boot 3 + Vue3 + MySQL，覆盖 用户/权限、基础数据、进货、出货、库存、盘点、调拨、退货、查询统计、报表、二维码扫码。
- **多端覆盖**：B/S Web 端（完整）、PC 端（Electron，信创友好）、**PC 端（Microsoft Access 桌面客户端）**、移动 APP（uni-app，Android / iOS / 鸿蒙）、微信小程序（员工端）。
- **读者端（会员端）**：微信小程序（`09-customer-miniprogram`），面向顾客提供图书浏览/检索、扫码查书与门店在架、会员积分余额与消费记录。
- **信创适配**：MySQL ↔ 达梦 DM8/openGauss 兼容层，麒麟/统信 UOS/鸿蒙 适配方案，Electron 信创前端。

---

## 二、目录结构

```
企业系统/
├── README.md                    本文件
├── 01-docs/                     分析与设计文档（Markdown + Mermaid UML）
│   ├── 00-总览.md
│   ├── 01-行业分析.md
│   ├── 02-需求分析与用例.md          （32+ 用例）
│   ├── 03-UML图集.md               （类图/活动图/时序图/状态图/组件/部署/ER）
│   ├── 04-查询统计设计.md           （22 项统计）
│   ├── 05-报表设计.md              （15 张报表）
│   ├── 06-技术选型报告.md
│   ├── 07-软件架构设计.md
│   ├── 08-数据库设计.md
│   ├── 09-接口设计.md              （REST API 权威契约）
│   ├── 10-信创适配方案.md
│   └── 11-Access客户端设计.md      （Access 桌面客户端设计）
├── 02-database/                 数据库
│   ├── schema.sql               建表脚本（30 张表）
│   ├── data.sql                 初始化数据
│   ├── dm/                      达梦 DM8 适配脚本与说明
│   └── README.md
├── 03-backend/                  Spring Boot 3 后端（B/S 服务端）
├── 04-frontend-web/             Vue3 + Element Plus B/S 前端
├── 05-pc-electron/              PC 端（Electron，信创）
├── 06-mobile-app/               移动 APP（uni-app：Android/iOS/鸿蒙）
├── 07-wechat-miniprogram/       微信小程序（员工端）
├── 08-access-client/            PC 端（Microsoft Access 桌面客户端：窗体/报表 + VBA）
└── 09-customer-miniprogram/     微信小程序（读者端/会员端：面向顾客）
```

---

## 三、技术选型（摘要）

| 层次 | 选型 |
|---|---|
| 后端 | Java 21 + Spring Boot 3.2 + MyBatis-Plus + JWT + Hutool(二维码) + POI(报表导出) |
| 前端(B/S) | Vue 3 + Vite + Element Plus + Pinia + Vue Router + Axios + ECharts |
| 数据库 | MySQL 8（主），达梦 DM8 / openGauss（信创） |
| PC 端 | Electron（跨平台，信创 OS 可运行）；Microsoft Access 桌面客户端（Windows，窗体/报表 + VBA 调 REST） |
| 移动端 | uni-app（一套代码 → Android / iOS / 鸿蒙） |
| 小程序 | 微信原生小程序 |
| 二维码 | Hutool QrCode + 扫码（APP/小程序/扫码枪） |

详见 [06-技术选型报告](01-docs/06-技术选型报告.md)。

---

## 四、快速启动（B/S）

**前置**：JDK 17+、Maven 3.8+、Node 18+、MySQL 8。

### 1. 初始化数据库
```sql
-- 在 MySQL 中执行
CREATE DATABASE bookstore DEFAULT CHARACTER SET utf8mb4;
-- 再依次执行：02-database/schema.sql、02-database/data.sql
```

### 2. 启动后端
```bash
cd 03-backend
mvn clean package -DskipTests
java -jar target/*.jar          # 默认端口 8080，接口前缀 /api
```

### 3. 启动前端
```bash
cd 04-frontend-web
npm install
npm run dev                     # 默认 http://localhost:5173
```

### 4. 登录
| 账号 | 密码 | 角色 |
|---|---|---|
| admin | 123456 | 系统管理员 |
| manager | 123456 | 店长 |
| buyer01 | 123456 | 采购员 |
| cashier01 | 123456 | 收银员 |
| stock01 | 123456 | 库管员 |

### 5. 其它端
- PC 端（Electron）：见 `05-pc-electron/README.md`
- PC 端（Microsoft Access 桌面客户端）：见 `08-access-client/README.md`
- 移动 APP：见 `06-mobile-app/README.md`
- 微信小程序（员工端）：见 `07-wechat-miniprogram/README.md`
- 微信小程序（读者端/会员端）：见 `09-customer-miniprogram/README.md`

---

## 五、核心业务能力

- **进货**：进货开单 → 提交 → 入库（自动更新库存与移动加权平均成本）→ 采购退货
- **出货**：销售开单 / POS 收银 → 出库（校验库存、累计会员积分与毛利）→ 销售退货
- **库存**：多仓库存查询、低库存预警、库存盘点（差异调整）、库存调拨、库存流水台账
- **查询统计**：22 项统计（销售/采购/库存/毛利/会员/盘点）
- **报表**：15 张报表，支持 Excel/CSV 导出
- **二维码**：图书、单据二维码生成与扫码解析（PC 扫码枪 / APP / 小程序）

---

## 六、文档索引

从 [00-总览](01-docs/00-总览.md) 开始阅读；接口契约见 [09-接口设计](01-docs/09-接口设计.md)。