# 晓镜书阁 · PC 端（Electron，信创友好）

书店进销存管理系统的桌面客户端。基于 **Electron**，使用原生 HTML/CSS/JS 渲染层，无重型构建链，开箱即用。

## 一、功能页面

| 页面 | 文件 | 说明 | 主要接口 |
|---|---|---|---|
| 登录 | `renderer/login.html` | 登录并保存 token | `POST /api/auth/login` |
| 主界面 | `renderer/index.html` | 左侧导航 + KPI 工作台 | `GET /api/stat/overview` |
| **库存盘点** | `renderer/check.html` | 选盘点单 → 扫码/输入录入实盘 → 完成盘点 | `GET /api/stock/check/orders`、`GET /api/stock/check/orders/{id}`、`POST /api/mobile/check/scan`、`PUT /api/stock/check/orders/{id}/items`、`POST /api/stock/check/orders/{id}/finish` |
| **扫码查询** | `renderer/scan.html` | ISBN 扫码 → 图书信息 + 实时库存 + 货架位 + 二维码 | `POST /api/scan/resolve`、`GET /api/mobile/stock/quick`、`GET /api/qrcode/book/{isbn}` |
| 单据/统计 | `renderer/query.html` | 出货单 / 进货单 / 移动端统计 | `GET /api/sale/orders`、`GET /api/purchase/orders`、`GET /api/mobile/stat/summary` |

> 接口契约以 `01-docs/09-接口设计.md` 为准。

## 二、目录结构

```
05-pc-electron/
├── package.json        # electron / electron-builder 依赖与脚本
├── main.js             # 主进程：创建窗口、菜单
├── preload.js          # 预加载：安全暴露 apiBase 配置
├── renderer/
│   ├── login.html / index.html / check.html / scan.html / query.html
│   ├── css/style.css
│   └── js/api.js       # 请求封装（Bearer token、401 跳登录）
│       js/login.js | index.js | check.js | scan.js | query.js
└── README.md
```

## 三、运行方式

**前置**：Node 18+；后端已启动于 `http://localhost:8080`。

```bash
cd 05-pc-electron
npm install      # 首次安装 Electron（需联网，约 100MB+）
npm start        # 启动应用，默认加载登录页
npm run dev      # 启动并自动打开 DevTools
```

登录账号：`admin / 123456`。

### 后端地址配置
默认 `http://localhost:8080/api`。可在启动前用环境变量覆盖（信创内网常用）：

```bash
# Windows PowerShell
$env:SHG_API_BASE="http://10.0.0.20:8080/api"; npm start
# Linux / 麒麟 / UOS
SHG_API_BASE="http://10.0.0.20:8080/api" npm start
```

## 四、打包发布

```bash
npm run dist          # 按当前平台打包（输出到 release/）
npm run dist:linux    # Linux：AppImage / deb / rpm
npm run dist:win      # Windows：nsis / portable
```

产物位于 `release/` 目录。

## 五、信创（国产化）说明

本 PC 端专为国产操作系统适配设计，要点如下：

1. **国产 OS 适配**：Electron 基于 Chromium 与 Node.js，可直接运行于
   **麒麟软件（Kylin V10）**、**统信 UOS**、深度 Deepin 等主流国产 Linux 桌面。
2. **打包格式**：
   - 麒麟 / UOS 推荐使用 **deb / rpm** 直接安装；
   - 通用分发可用 **AppImage**（免安装、双击运行，最省事）。
   - 在目标国产 OS 上执行 `npm run dist:linux` 生成对应包。
3. **架构**：国产 OS 常见 `x86_64` 与 `aarch64`（飞腾/鲲鹏）。打包 aarch64 需在对应架构机器（或交叉编译环境）执行 `npm run dist:linux`，或使用 `--arm64` 参数。
4. **国产内核/组件版本**：麒麟 V10 / UOS 通常携带 GTK3、NSS 等依赖；若 Electron 启动缺库，可安装对应运行库（如 `libnss3`、`libgtk-3-0`、`libasound2`）。
   Electron 亦可选取其 **国产内核版本**（社区/厂商维护的 Chromium 内核定制版）以满足更高合规要求。
5. **无需系统级运行时**：不依赖 Web 服务器，安装包自带运行时，便于在离线内网环境部署。
6. **安全**：`contextIsolation: true` + `nodeIntegration: false`，渲染层通过 `preload.js` 仅获取只读配置，符合桌面端安全基线。

## 六、备注

- 本工程不修改任何后端/文档代码；仅消费 `/api` 接口。
- 未安装依赖时页面无法真正联网，但配置文件与页面代码语法自洽，可直接 `npm install` 后运行。