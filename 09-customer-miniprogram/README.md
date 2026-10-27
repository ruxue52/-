# 晓镜书阁 · 读者端小程序（会员端）

面向**顾客/会员**的微信原生小程序：浏览与检索图书、扫码查书与门店在架、会员积分/余额/消费记录。
与员工端小程序（`07-wechat-miniprogram`）**完全分离**：员工端调 `/api/auth`、`/api/mobile/**`，读者端调 `/api/reader/**`。

## 一、页面与接口

| 页面 | 目录 | 说明 | 主要接口 |
|---|---|---|---|
| 书房（首页） | `pages/index/index` | 分类入口 + 新书上架 + 搜索 | `GET /api/reader/categories`、`GET /api/reader/books` |
| **查书** | `pages/books/books` | 关键词/分类/排序 + 上拉分页 | `GET /api/reader/books` |
| 图书详情 | `pages/detail/detail` | 图书信息 + 各门店在架数量 | `GET /api/reader/books/{id}`、`GET /api/reader/books/by-isbn/{isbn}` |
| **扫书** | `pages/scan/scan` | `wx.scanCode` 扫 ISBN / 手输 ISBN | `GET /api/reader/books/by-isbn/{isbn}` |
| 会员登录 | `pages/login/login` | 手机号 + 验证码登录（保存 token） | `POST /api/reader/auth/send-code`、`POST /api/reader/auth/login` |
| **我的会员** | `pages/member/member` | 积分/余额/累计消费 + 等级权益 | `GET /api/reader/member/profile`、`GET /api/reader/levels` |
| 消费记录 | `pages/orders/orders` | 消费列表 + 展开明细 | `GET /api/reader/member/orders`、`GET /api/reader/member/orders/{id}` |

> 接口契约见 `01-docs/09-接口设计.md` 第 12 章「读者端（会员端）」。

## 二、目录结构

```
09-customer-miniprogram/
├── app.js                  # 全局：baseUrl、会员 token/资料存取、requireLogin
├── app.json / app.wxss     # 全局配置与样式（宣纸底 + 松墨绿 + 赭金）
├── project.config.json     # 项目配置（urlCheck=false）
├── sitemap.json
├── utils/request.js        # wx.request 封装 + 会员 token 拦截 + 展示格式化
├── pages/
│   ├── index/  books/  detail/  scan/  login/  member/  orders/   （每页 .js/.wxml/.wxss/.json）
└── README.md
```

## 三、运行/导入步骤

1. 启动后端（`03-backend`，默认 `http://localhost:8080`，接口前缀 `/api`）。
2. 下载安装 **微信开发者工具**（稳定版）→ `导入项目` → 选择目录 `09-customer-miniprogram`。
3. **AppID**：工程默认填本系统小程序 AppID（`wx680ce7cae16fdb65`，与员工端一致）；若要独立发布，替换 `project.config.json` 中的 `"appid"` 即可。
4. ⚠️ **勾选「不校验合法域名」**：右上角 `详情 → 本地设置 → 勾选「不校验合法域名…」`，否则 `http://localhost:8080` 会被微信拦截。
5. 编译后即可浏览图书与扫码查书（免登录）；点「我的 → 手机号登录」使用下方演示会员账号。

## 四、演示会员账号

登录为「手机号 + 短信验证码」，验证码由后端在响应中回显（演示环境未接短信网关，页面会直接提示验证码）。

| 手机号 | 会员 | 等级 | 说明 |
|---|---|---|---|
| 13900000001 | 张伟 | 普通会员 | — |
| **13900000002** | 李娜 | 银卡会员（95% 结算价、1.5 倍积分） | **有 2 笔消费记录，便于演示** |
| 13900000003 | 王芳 | 金卡会员（90% 结算价、2 倍积分） | — |
| **13900000004** | 刘强 | 钻石会员（85% 结算价、3 倍积分） | **有 1 笔消费记录** |

> 非会员手机号登录会提示「该手机号尚未注册会员，请先到门店办理会员卡」；仅 `base_customer` 中在册且登记了手机号的会员可登录。

## 五、后端基地址配置

`app.js` 的 `globalData.baseUrl`：

```js
baseUrl: 'http://localhost:8080/api'
```

- 开发者工具内 `localhost` 可直连本机后端；若遇到连接被拒（Node/浏览器把 `localhost` 解析到 IPv6），改为 `http://127.0.0.1:8080/api`。
- 真机预览/体验版：改为电脑**局域网 IP**（如 `http://192.168.1.20:8080/api`），并在开发者工具中暂停域名校验。
- 正式发布需在微信公众平台配置 `request 合法域名` 且使用已备案的 **HTTPS** 域名。

## 六、本次为支撑读者端所做的后端改动

- `LoginUser` / `JwtUtil` 增加 `userType`（`STAFF` / `CUSTOMER`）；`AuthInterceptor` 限制**会员 token 仅可访问 `/api/reader/**`**，访问其它接口返回 403。
- 新增 `ReaderAuthController`、`ReaderBookController`、`ReaderMemberController` 与 `ReaderMemberService`、`ReaderCodeService`。
- 读者端接口**不返回**进价/成本等内部字段（`purchasePrice` / `avgCost` / `costPrice`）。
- 原有员工端接口未做任何行为变更（回归已验证：登录、图书、会员、进货、出货、库存等接口照常）。

## 七、备注

- 扫码能力依赖 `wx.scanCode`（相机权限由微信统一管理，首次使用会请求授权）；兼容本系统二维码 `SHG:BOOK:<isbn>` 与书籍自身条码。
- 图书封面缺失时，列表与详情使用「书名首字 + 渐变书脊」占位，不依赖外部图片。
- 数据库初始化见 `02-database/schema.sql`、`02-database/data.sql`。
