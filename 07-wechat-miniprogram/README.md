# 晓镜书阁 · 微信小程序（原生）

书店进销存管理系统的微信小程序端，使用**微信原生小程序**（无框架），通过 `wx.scanCode` 扫码、`wx.request` 调后端接口。

## 一、页面与接口

| 页面 | 目录 | 说明 | 主要接口 |
|---|---|---|---|
| 登录 | `pages/login/login` | 登录并保存 token | `POST /api/auth/login` |
| 首页 | `pages/index/index` | 首页指标 + 功能宫格 | `GET /api/mobile/overview`、`GET /api/mobile/stat/summary` |
| **扫码** | `pages/scan/scan` | `wx.scanCode` → 解析并展示 | `POST /api/scan/resolve`、`POST /api/scan/log`、`GET /api/mobile/stock/quick` |
| **库存查询** | `pages/stock/stock` | 扫/输 ISBN 查库存与货架位 | `GET /api/mobile/stock/quick` |
| **扫码盘点** | `pages/check/check` | 选盘点单 → 扫码录入 → 提交 | `GET /api/stock/check/orders`、`GET /api/stock/check/orders/{id}`、`POST /api/mobile/check/scan`、`PUT /api/stock/check/orders/{id}/items`、`POST /api/stock/check/orders/{id}/finish` |
| 单据查询 | `pages/order/order` | 出货单 / 进货单 列表与详情 | `GET /api/sale/orders`、`GET /api/purchase/orders`（含 `/{id}`） |

> 接口契约以 `01-docs/09-接口设计.md` 为准。

## 二、目录结构

```
07-wechat-miniprogram/
├── app.js / app.json / app.wxss
├── project.config.json     # 项目配置（urlCheck=false）
├── sitemap.json
├── utils/request.js        # wx.request 封装 + token 拦截
├── pages/
│   ├── login/  index/  scan/  stock/  check/  order/   （每页 .js/.wxml/.wxss/.json）
└── README.md
```

## 三、运行/导入步骤

1. 下载安装 **微信开发者工具**（稳定版）。
2. `导入项目` → 选择目录 `07-wechat-miniprogram`。
3. **AppID**：可选择「测试号」，或替换 `project.config.json` 中的 `"appid"` 为你的小程序 AppID。
4. ⚠️ **关键：勾选「不校验合法域名」**
   开发者工具右上角 `详情 → 本地设置 → 勾选「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」`。
   否则 `http://localhost:8080` 会被微信拦截（小程序默认只允许 https 且需在后台配置域名）。
5. 后端需已启动于 `http://localhost:8080`，然后在小程序内登录（`admin / 123456`）。

## 四、后端基地址配置

在 `app.js` 的 `globalData.baseUrl` 修改：

```js
baseUrl: 'http://localhost:8080/api'
```

- 开发者工具内 `localhost` 可直连本机后端。
- 真机预览/体验版：需填写电脑**局域网 IP**（如 `http://192.168.1.20:8080/api`），并在开发者工具中暂停域名校验（体验版需真机调试工具栏开启「调试」同样跳过校验）。

## 五、域名与上线说明

- **开发阶段**：依赖「不校验合法域名」，仅用于本地联调。
- **正式发布**：需在 **微信公众平台 → 开发 → 开发设置 → 服务器域名** 中，把后端域名加入 `request 合法域名`；生产环境必须使用 **HTTPS** 域名且已备案，`http`/IP 形式无法通过审核。
- 若沿用本方案内网部署，可考虑通过反向代理（Nginx）为后端配置 HTTPS 域名后，再在后台登记。

## 六、备注

- 本工程不修改后端/文档代码，仅消费 `/api` 接口。
- 扫码能力依赖 `wx.scanCode`（相机权限由微信统一管理，首次使用会请求授权）。
- 未接入真实后端时，页面可正常打开，接口调用会提示「无法连接后端」。