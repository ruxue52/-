# 08 · 晓镜书阁 Access 桌面客户端（PC 端交付物）

> 面向 Windows 桌面（Microsoft Access），以「**库存盘点、在线查询、扫码办事**」为核心场景，
> 与现有 Spring Boot 后端（`/api`，端口 8080）联通，作为 PC 端交付物之一。
> 接口契约：[`01-docs/09-接口设计.md`](../01-docs/09-接口设计.md)；表结构：[`02-database/schema.sql`](../02-database/schema.sql)。

---

## 一、交付物：开箱即用的 .accdb + 可复现构建脚本 + 源码/规格

本目录**已内置一个可直接打开的二进制数据库 `ShuHaiGe-PSI.accdb`**（标题/界面为中文），
并附带一个**幂等的构建脚本** `tools\build-accdb.ps1`，在有 Microsoft Access 的 Windows 上
可一键重新生成该 `.accdb`。同时仍保留源码与规格，便于二次开发与人工复现：

- **开箱即用 `.accdb`**：`ShuHaiGe-PSI.accdb` —— 已含 **10 个 VBA 模块、9 个窗体、4 张原生报表**，
  启动窗体为 `frmLogin`；双击即可打开（首次需按下方「打开与信任设置」放行宏）。
- **可复现构建脚本**：`tools\build-accdb.ps1` —— 用 Access COM 自动化，从 `vba/*.bas`
  真正生成 `.accdb`（导入模块 → 建本地临时表 → 程序化建窗体/报表 → 设启动窗体 → 重开枚举校验）；
  日志见 `tools\build-accdb.log`。用法：
  ```powershell
  powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\build-accdb.ps1
  ```
  （脚本会自行写入 HKCU 信任设置 `AccessVBOM=1`、`VBAWarnings=1` 并结束遗留 Access 进程，可重复运行。）
- **可导入的 VBA 源码**（`vba/`）—— 每个 `.bas` 均含 `Option Compare Database` / `Option Explicit`，可直接 `导入文件`；
- **窗体/报表规格**（`forms/`、`reports/`）—— 逐控件、逐事件说明，照做即可复现；
- **搭建指南**（本文件 + `assets/create-accdb-steps.md`）—— 手工分步组装出可运行客户端（备用路径）。

> 目标：有 Access 环境的用户**直接打开 `.accdb`** 即可用；需要定制时，改 `vba/*.bas` 后重跑构建脚本即可。

### 打开与信任设置（首次）

1. 双击 `ShuHaiGe-PSI.accdb`（或 Access → 打开）。
2. 若提示「已阻止宏运行」，点 **启用内容**；或：文件 → 选项 → 信任中心 → 信任中心设置 →
   **宏设置 = 启用所有宏**、并勾选 **信任对 VBA 工程对象模型的访问**。
   （构建脚本已自动写入当前用户的等效注册表项。）
3. 打开后自动进入 `frmLogin`；用 `admin / 123456` 登录（后端需运行在 `http://localhost:8080/api`）。

### 事件绑定说明（重要）

本 `.accdb` 的按钮事件采用 **Access「表达式」方式** 绑定到 `vba/` 标准模块里的**公有过程**
（不注入任何窗体代码模块），例如：
- `frmLogin.cmdLogin` → `=modLogin.DoLogin([txtUsername],[txtPassword])`
- `frmReport.cmdPreview` → `=modReportBuild.PreviewReport(modReport.ReportCodeOf([cboReport]),'')`
- `frmReport.cmdPrint`   → `=modReportBuild.PrintReport(modReport.ReportCodeOf([cboReport]),'')`
- `frmMain.cmdLogout`    → `=modLogin.DoLogout`（零参数函数**不能写空括号**，见下方限制）
- `frmMain` 的 7 个导航按钮 → `=modUtil.NavOpenForm("frmStockQuery")` 等；表达式里不能直接写
  `DoCmd`，故由 `modUtil.NavOpenForm` 在 VBA 内部代为调用 `DoCmd.OpenForm`

> 限制：Access 表达式仅能调用**有返回值的函数**；并且**零参数函数必须省略空括号**——写成
> `=modLogin.DoLogout()` 会被 Access 判为「无效语法」而绑定失败（`=modUtil.FmtNum(0)` 这类
> 带参调用则正常）。表达式内也**不能直接调用 `DoCmd`**，需按上面的方式在一个 VBA 函数里代调。
>
> 因此**扫码枪录入（`frmMain.txtGlobalScan` 的 `KeyDown`）仍无法用表达式实现**：它需要把
> `KeyCode` 置 0（写回参数），而表达式事件做不到，须在各窗体自己的代码模块里实现
> （`Private Sub txtGlobalScan_KeyDown(KeyCode As Integer, ...)`）—— 参见 `forms/*.md`
> 的「事件过程」小节。

---

## 二、目录结构

```
08-access-client/
├── README.md                     本文件（总览 + 分步搭建指南）
├── vba/                          VBA 源码（10 个模块，可直接导入）
│   ├── modApi.bas                通用 REST 客户端（WinHTTP）+ 轻量 JSON 解析器
│   ├── modUtil.bas               格式化/JSON 拼装/URL 编码/提示/控件辅助
│   ├── modLogin.bas              登录、当前用户、权限判断
│   ├── modStock.bas              库存查询/预警/概览/流水/移动端快速查
│   ├── modCheck.bas              盘点：建单/明细/扫码录入/完成/作废
│   ├── modPurchase.bas           进货：列表/详情/入库
│   ├── modSale.bas               出货：列表/POS 检索/出库
│   ├── modScan.bas               扫码解析/记录/二维码下载
│   ├── modReport.bas             报表取数/导出/目录
│   └── modReportBuild.bas        报表动态落表 + 绑定报表
├── forms/                        9 个窗体规格（Markdown）
│   ├── frmLogin.md  frmMain.md  frmStockQuery.md  frmScanQuery.md  frmCheck.md
│   └── frmPurchaseIn.md  frmSaleOut.md  frmOrderQuery.md  frmReport.md
├── reports/reports-spec.md       15 张报表（R01~R15）规格总表
├── sql/
│   ├── linked-tables.md          ODBC 链接 MySQL 步骤与 DSN 配置
│   └── access-queries.sql        10 条 Access 直连查询 + 方言对照表
└── assets/
    ├── create-accdb-steps.md     从零组装 .accdb 的分步清单
    └── import-odbc.reg           用户 DSN 注册表模板
```

---

## 三、总体架构：双通道

```
                        ┌──────────────────────────────────────┐
                        │      晓镜书阁 Access 客户端 (.accdb)     │
                        │                                      │
   通道 A（读/报表）      │   ┌──────────────────────────────┐    │
  ┌────────────────────┐│   │  VBA 模块（modApi/Login/...） │    │
  │ MySQL ODBC 8.x DSN ││   └──────────────┬───────────────┘    │
  │  链接表(Linked)     │◄┼──────────────────│────────────────────┼──► REST API
  │  psi_stock / base_* ││   通道 B（写/业务）│  WinHTTP            │   http://localhost:8080/api
  └─────────┬──────────┘│                    ▼                   │   Authorization: Bearer <JWT>
            ▼            │          ┌──────────────────┐        │
        ┌────────┐       │          │ frmLogin 登录     │        │
        │ MySQL8 │◄──────┼──────────┤ frmCheck 盘点     │        │
        │bookstore│      │          │ frmScanQuery 扫码 │        │
        └───┬────┘       │          │ frmSaleOut POS    │        │
            │            │          │ frmReport 报表     │        │
            ▼            │          └──────────────────┘        │
    Spring Boot 3 后端 ───┘  事务/移动加权成本/库存流水/积分/状态机  │
    （唯一业务写入口）                                             │
                        └──────────────────────────────────────┘
```

**通道分工（关键设计）**

| 通道 | 方式 | 用途 | 为什么 |
|---|---|---|---|
| **A** 链接表(ODBC) | Access 通过 MySQL ODBC 链接后端库的表 | 基础数据浏览、库存结存、报表直连、离线批量查询 | 读快、轻量、可利用 Access 原生表格/排序/筛选 |
| **B** REST API(VBA) | `modApi` 用 WinHTTP 调 `/api/*` | 登录、入库/出库/盘点/退货等**业务写操作** | 复用后端事务、移动加权成本、流水、积分、状态机，保证与 Web 端**数据一致** |

> **写入一律走通道 B**：直接在 Access 改库会绕过成本计算与流水，导致与 Web 端不一致。

---

## 四、环境准备

| 项 | 要求 |
|---|---|
| 操作系统 | Windows 10/11（**Access 仅限 Windows**，见「信创」）。 |
| Office / Access | Access 2016 / 2019 / 2021 / Microsoft 365（记住位数：32 或 64）。 |
| MySQL ODBC 驱动 | **MySQL ODBC 8.0 Unicode Driver**，须与 Office **同位数**。 |
| 后端 | Spring Boot 3 已启动：`http://localhost:8080`，前缀 `/api`，登录 `admin/123456`。 |
| 数据库 | MySQL 8，库 `bookstore`（执行过 `schema.sql`、`data.sql`）。 |

验证后端：浏览器打开 `http://localhost:8080/api/stock/summary`，返回 JSON（未登录 401 亦属后端在线）。

---

## 五、分步搭建指南（速览）

> 详细清单见 [`assets/create-accdb-steps.md`](assets/create-accdb-steps.md)。

1. **新建 .accdb**：空白数据库 `晓镜书阁客户端.accdb`；启用宏（信任中心）。
2. **导入 VBA**：`Alt+F11` → 文件 → 导入文件 → 导入 `vba/` 全部 10 个 `.bas` → 编译通过。
3. **建临时表**：按 `assets/create-accdb-steps.md` 第 3 步执行 DDL（`tmp*`、`tblSession`）。
4. **建窗体**：按 `forms/` 每份规格摆控件 + 粘贴事件过程，共 9 个窗体（含 3 个子窗体）。
5. **建报表**（可选）：按 `reports/reports-spec.md` 建 `rptR01~rptR15`，记录源 `tmp_R01~tmp_R15`。
6. **配 DSN（通道 A）**：按 `sql/linked-tables.md` 建 DSN `SHG-MySQL`，运行 `LinkAllTables`。
7. **设启动窗体**：`文件 → 选项 → 当前数据库 → 显示窗体 = frmLogin`。
8. **冒烟测试**：登录 → 查库存 → 扫码 → 盘点 → 出报表。
9. **打包**：另存为 **.accde**；终端可只装 Access **Runtime**。

> 关键常量在 `vba/modApi.bas` 顶部：`Public Const API_BASE As String = "http://localhost:8080/api"`，部署换机时改此处。

---

## 六、关键场景操作步骤

### 6.1 登录（frmLogin）
1. 打开数据库 → 自动弹出 `frmLogin`。
2. 输入 `admin / 123456` → 回车（或「登录」）。
3. 成功：`POST /api/auth/login` 返回 `token`，写入全局 `gToken/gRealName/gRoles/gPermissions`，打开 `frmMain`。
4. 失败：提示「账号或密码错误 / 账号被停用」（对应 `code=2001/2002`）。

### 6.2 库存在线查询（frmStockQuery）
1. 主控台点「库存在线查询」。
2. 选仓库/分类、填关键字、可勾「仅低库存」→ 查询。
3. 调 `GET /api/stock/list`，结果落 `tmpStockList` 表，子窗体显示并可排序/筛选。
4. 「导出」按钮 `DoCmd.TransferSpreadsheet` 生成 Excel。

### 6.3 扫码办事（frmScanQuery）
1. **扫码枪=键盘设备**：把光标放进「扫码」文本框，扫描 ISBN。
2. 扫码内容自动以**回车**结束 → `KeyDown(vbKeyReturn)` 触发查询。
3. 调 `GET /api/mobile/stock/quick?keyword=<isbn>`，展示书名/作者/价格/货架位/库存/状态。
4. 同时下载 `GET /api/qrcode/book/{isbn}` 的 PNG 显示在 `imgQR`，可另存。
5. 扫码内容兼容 `SHG:BOOK:<isbn>` 与纯条码；`SHG:STOCK:<wh>:<isbn>` 亦可解析。

### 6.4 库存盘点（frmCheck）
1. 主控台点「库存盘点」。
2. 选盘点单（或「新建盘点单」→ 选仓库/类型/备注 → `POST /api/stock/check/orders`）。
3. 焦点到「扫码盘点」框，扫描 ISBN → 自动录入实盘（累加）或覆写。
   - 累加：`POST /api/mobile/check/scan`（服务端按盘点标记累加）；
   - 手工改数：直接在明细子窗体改「实盘」列。
4. 点「提交实盘数」→ `PUT /api/stock/check/orders/{id}/items`。
5. 点「完成盘点」→ `POST .../finish`：服务端按差异调整库存并写流水，状态→`FINISHED`。
6. 可「作废」→ `POST .../cancel`。

### 6.5 进货入库（frmPurchaseIn）
查询进货单 → 选中 → 「确认入库」→ `POST /api/purchase/orders/{id}/instock`（写库存+流水，状态→`INSTOCK`）。

### 6.6 出货 / POS（frmSaleOut）
扫码/检索加入购物车（`GET /api/sale/pos/search`）→ 结算 → `POST /api/sale/orders` + `POST .../outstock`。
库存不足后端返回 `code=1002` 并回滚，客户端提示。

### 6.7 单据查询（frmOrderQuery）
按类型/状态/日期查询进销/盘点单，选中可查看与保存**单据二维码**（`GET /api/qrcode/order/{orderNo}`）。

### 6.8 报表中心（frmReport）
选报表（R01~R15）→ 填参数 → 预览/打印/导出。
预览：`modReportBuild.BuildReportTable` 取数落 `tmp_<code>` → 打开 `rpt<code>`；
导出：`GET /api/report/{code}/export?format=xlsx|csv`。

---

## 七、与 Web 端的一致性保证

| 维度 | 一致性做法 |
|---|---|
| 认证 | 同一 `POST /auth/login`，同一 JWT，同一 `Authorization: Bearer` 头。 |
| 业务写 | 入库/出库/盘点/退货全部调后端接口，复用同一事务与成本/流水逻辑。 |
| 字段 | API 层面用驼峰（`bookName`），与 Web 完全相同；SQL 直连用下划线（`book_name`）。 |
| 状态机 | `DRAFT/SUBMITTED/INSTOCK/OUTSTOCK/CANCELLED/FINISHED` 与后端一致。 |
| 报表 | 同一 `/report/{code}` 契约，列与顺序与 Web 导出一致。 |
| 扫码 | 同一 `/scan/resolve`、`/mobile/check/scan`、`/mobile/stock/quick`，与 APP 同源。 |

---

## 八、信创 / 兼容性说明（分工）

- **Access 仅能在 Windows 上运行**，且依赖 Microsoft Office / Access Runtime，**不适配麒麟、统信 UOS、鸿蒙等国产 OS**。
- 因此本项目 PC 端采取**双轨交付**：
  - **Access 客户端（本目录）**：面向既有 Windows + Office 环境的门店，快速落地、报表能力强。
  - **Electron 客户端（`05-pc-electron/`）**：面向信创 OS，跨平台、可国产化适配。
- 二者**共用同一后端 REST API**，业务与数据完全一致，可按现场环境二选一部署。
- 若需国产化替代 Access，可用 **WPS 表格 + 宏**、**达梦/openGauss + 自研客户端**或直接采用 Electron 端。

---

## 九、常见问题

| 现象 | 处理 |
|---|---|
| 找不到 DSN / 驱动列表为空 | Office 与 ODBC 驱动**位数不匹配**；装同位数驱动。 |
| 中文乱码 | 用 Unicode 驱动 + `utf8mb4`；VBA 内置 `Utf8Bytes/Utf8String`。 |
| `用户定义类型未定义`（DAO） | 引用 → 勾选 Access 数据库引擎对象库。 |
| 请求失败 | 后端未启动 / `API_BASE` 不符（改 `modApi.bas` 顶部常量）。 |
| 报表打开空白 | 未先执行 `BuildReportTable`，或报表记录源未指向 `tmp_<code>`。 |
| 想直连库做写操作 | 不推荐；写入请走 REST API（见「双通道」说明）。 |

---

## 十、相关文档

- 接口契约：[`01-docs/09-接口设计.md`](../01-docs/09-接口设计.md)
- 数据库结构：[`02-database/schema.sql`](../02-database/schema.sql)
- 报表设计：[`01-docs/05-报表设计.md`](../01-docs/05-报表设计.md)
- Access 设计文档：[`01-docs/11-Access客户端设计.md`](../01-docs/11-Access客户端设计.md)
- 搭建清单：[`assets/create-accdb-steps.md`](assets/create-accdb-steps.md)