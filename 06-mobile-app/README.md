# 晓镜书阁 · 移动 APP（uni-app，Vue3）

书店进销存管理系统的移动端，一套代码运行到 **Android / iOS / 鸿蒙（HarmonyOS）**。基于 uni-app（Vue3 语法）+ `uni.scanCode` 扫码能力。

## 一、页面与接口

| 页面 | 文件 | 说明 | 主要接口 |
|---|---|---|---|
| 登录 | `pages/login/login.vue` | 登录并保存 token | `POST /api/auth/login` |
| 首页 | `pages/index/index.vue` | 首页指标 + 功能宫格 | `GET /api/mobile/overview`、`GET /api/mobile/stat/summary` |
| **扫码** | `pages/scan/scan.vue` | `uni.scanCode` → 按类型跳转/就地展示 | `POST /api/scan/resolve`、`POST /api/scan/log`、`GET /api/mobile/stock/quick` |
| **库存查询** | `pages/stock/stock.vue` | 扫/输 ISBN 查询实时库存与货架位 | `GET /api/mobile/stock/quick` |
| **移动盘点** | `pages/check/check.vue` | 选盘点单 → 扫码录入实盘 → 提交 | `GET /api/stock/check/orders`、`GET /api/stock/check/orders/{id}`、`POST /api/mobile/check/scan`、`PUT /api/stock/check/orders/{id}/items`、`POST /api/stock/check/orders/{id}/finish` |
| 单据查询 | `pages/order/order.vue` | 出货单 / 进货单 列表与详情 | `GET /api/sale/orders`、`GET /api/purchase/orders`（含 `/{id}` 详情） |

> 接口契约以 `01-docs/09-接口设计.md` 为准。

## 二、目录结构

```
06-mobile-app/
├── package.json          # 依赖与脚本（HBuilderX 下可忽略依赖安装）
├── pages.json            # 页面路由 + tabBar
├── manifest.json         # 应用配置（app-plus: android/ios；app-harmony: 鸿蒙）
├── App.vue               # 应用入口与全局样式
├── main.js               # createSSRApp（Vue3）
├── uni.scss              # 全局 SCSS 变量
├── utils/request.js      # uni.request 封装 + token 拦截
├── pages/
│   ├── login/login.vue
│   ├── index/index.vue
│   ├── scan/scan.vue
│   ├── stock/stock.vue
│   ├── check/check.vue
│   └── order/order.vue
└── README.md
```

## 三、运行方式（HBuilderX，推荐）

1. 下载安装 **HBuilderX**（App 开发版）。
2. `文件 → 打开目录`，选择 `06-mobile-app` 目录。
3. 顶部菜单 `运行 → 运行到手机或模拟器 → 运行到 Android App 基座 / iOS App 基座`（需连接真机或启动模拟器）。
4. 首次运行需在 HBuilderX 中登录 DCloud 账号。

> 也可用 CLI（Vue3/Vite）：`npm install && npm run dev:app`，再配合 `@dcloudio` 相关工具链，但 HBuilderX 方式最省事。

## 四、后端基地址配置

在 `utils/request.js` 中修改：

```js
export const BASE_URL = 'http://localhost:8080/api'
```

- **模拟器 / H5**：`localhost` 可直接访问本机后端。
- **真机调试**：请改成电脑的**局域网 IP**（如 `http://192.168.1.20:8080/api`），并确保手机与电脑同一网络、后端监听 `0.0.0.0`。
- 登录账号：`admin / 123456`。

## 五、打包发布

### Android / iOS
HBuilderX：`发行 → 原生 App-云打包`（或制作自定义基座后本地打包）。
- Android 权限已在 `manifest.json` 声明（CAMERA / INTERNET 等）。
- iOS 需配置 `NSCameraUsageDescription`（已写入 manifest）。

### 鸿蒙（HarmonyOS Next）
1. 使用 **HBuilderX 3.99 及以上**版本，`发行 → 鸿蒙 App`。
2. 在 `manifest.json` 的 `app-harmony` 节点补充 **appid**（DCloud 鸿蒙应用标识）与签名信息。
3. `uni.scanCode` 在鸿蒙端走系统相机扫码能力；相机权限 `ohos.permission.CAMERA` 已在 manifest 声明。
4. 打包产物为 `.hap`，通过 DevEco Studio / 华为应用市场分发。

## 六、信创 / 国产化说明

1. **鸿蒙（信创重点）**：uni-app 原生支持 HarmonyOS Next，本工程已在 `manifest.json` 增加 `app-harmony` 配置项（权限、API 版本），可直接打包鸿蒙应用。
2. **国产 Android 系统**：可运行于麒麟/统信等厂商的国产安卓平板或定制终端（标准 APK 即可），扫码、查询、盘点功能均可用。
3. **国产化终端适配**：扫码依赖摄像头；如使用工业 PDA（扫码枪式手持终端），`uni.scanCode` 仍可用，或改用手持机硬件扫码广播集成。
4. **数据安全**：请求统一走 `Authorization: Bearer <token>`，401 自动跳转登录；不落地敏感明文。

## 七、备注

- 本工程不修改后端/文档代码，仅消费 `/api` 接口。
- 未接入真实后端时，页面可正常打开，接口调用会提示「无法连接后端」。