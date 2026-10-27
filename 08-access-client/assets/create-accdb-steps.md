# create-accdb-steps.md · 从零组装 .accdb 的分步清单

> 按此清单可**逐步复现**一个可运行的「晓镜书阁 Access 桌面客户端」。
> 全程约 30–60 分钟。全程使用 Access 原生功能，无需安装任何第三方控件。

## 第 0 步 · 环境检查

- [ ] Windows 10/11，已装 Access 2016/2019/2021/365（记录位数：32 或 64）
- [ ] 已装与 Office **同位数**的 `MySQL ODBC 8.0 Unicode Driver`
- [ ] 后端已启动：浏览器访问 `http://localhost:8080/api/stock/summary` 能返回 JSON（未登录会返回 401，属正常）
- [ ] 已安装 `08-access-client` 目录（本交付物）

## 第 1 步 · 新建数据库

1. Access → 空白数据库 → 文件名 `晓镜书阁客户端.accdb` → 创建。
2. 文件 → 选项 → 信任中心 → 信任中心设置 → **宏设置 = 启用所有宏**（开发期；交付前改回「禁用并通知」并签名）。
3. 文件 → 选项 → 当前数据库：显示导航窗格、**显示对象 = 全部**。

## 第 2 步 · 导入 VBA 模块

1. 按 `Alt+F11` 打开 VBA 编辑器。
2. 依次 `文件 → 导入文件`，导入 `vba/` 下全部 10 个 `.bas`：
   `modApi.bas`、`modUtil.bas`、`modLogin.bas`、`modStock.bas`、`modCheck.bas`、
   `modPurchase.bas`、`modSale.bas`、`modScan.bas`、`modReport.bas`、`modReportBuild.bas`。
3. 编译：`调试 → 编译 VBAProject`，应无错误。
   （若报「找不到工程/库」，确认 DAO 引用存在：工具 → 引用 → 勾选 **Microsoft Office xx.0 Access database engine Object Library**。）

> 导入时若模块名冲突（如已有 `modUtil`），先删除同名模块再导入。

## 第 3 步 · 建本地辅助临时表

在「创建 → 查询设计 → SQL 视图」中执行下列 DDL（建 `*_tmp` 本地表；这些表**不是**链接表）：

```sql
CREATE TABLE tblSession     (skey TEXT(64), val TEXT(255));
CREATE TABLE tmpStockList   (book_id LONG, book_name TEXT(255), isbn TEXT(32),
    warehouse_id LONG, warehouse_name TEXT(128), category_id LONG,
    category_name TEXT(64), shelf_no TEXT(64), quantity DOUBLE,
    safety_stock DOUBLE, avg_cost DOUBLE, stock_amount DOUBLE);
CREATE TABLE tmpCheckOrders (id LONG, check_no TEXT(64), warehouse_id LONG,
    warehouse_name TEXT(128), check_type TEXT(16), status TEXT(16),
    total_items LONG, diff_items LONG, profit_qty DOUBLE, loss_qty DOUBLE,
    create_time DATETIME);
CREATE TABLE tmpCheckItems  (item_id LONG, book_id LONG, book_name TEXT(255),
    isbn TEXT(32), book_qty DOUBLE, real_qty DOUBLE, diff_qty DOUBLE, diff_amount DOUBLE);
CREATE TABLE tmpPurchaseOrders (id LONG, order_no TEXT(64), supplier_name TEXT(128),
    warehouse_name TEXT(128), total_qty DOUBLE, pay_amount DOUBLE, status TEXT(16),
    operator_name TEXT(64), create_time DATETIME);
CREATE TABLE tmpPurchaseItems (book_id LONG, book_name TEXT(255), isbn TEXT(32),
    quantity DOUBLE, price DOUBLE, amount DOUBLE);
CREATE TABLE tmpSaleOrders  (id LONG, order_no TEXT(64), customer_name TEXT(128),
    warehouse_name TEXT(128), total_qty DOUBLE, pay_amount DOUBLE,
    payment_type TEXT(32), status TEXT(16), create_time DATETIME);
CREATE TABLE tmpPosCart     (book_id LONG, book_name TEXT(255), isbn TEXT(32),
    quantity DOUBLE, price DOUBLE, amount DOUBLE);
CREATE TABLE tmpDocQuery    (id LONG, doc_no TEXT(64), doc_type TEXT(16),
    partner_name TEXT(128), warehouse_name TEXT(128), total_qty DOUBLE,
    amount DOUBLE, status TEXT(16), create_time DATETIME);
```

`tmp_<code>`（R01~R15 报表临时表）**无需手工建**，由 `modReportBuild.BuildReportTable` 运行时自动创建。

## 第 4 步 · 按规格建窗体

参照 `forms/` 目录逐份规格创建，至少 9 个：

| 顺序 | 窗体名 | 规格文件 |
|---|---|---|
| 1 | `frmLogin` | `forms/frmLogin.md` |
| 2 | `frmMain` | `forms/frmMain.md` |
| 3 | `frmStockQuery`（+ 子窗体 `sfmStockList`） | `forms/frmStockQuery.md` |
| 4 | `frmScanQuery` | `forms/frmScanQuery.md` |
| 5 | `frmCheck`（+ 子窗体 `sfmCheckItems`） | `forms/frmCheck.md` |
| 6 | `frmPurchaseIn`（+ 子窗体 `sfmPurchaseItems`） | `forms/frmPurchaseIn.md` |
| 7 | `frmSaleOut`（+ 子窗体 `sfmPosCart`） | `forms/frmSaleOut.md` |
| 8 | `frmOrderQuery` | `forms/frmOrderQuery.md` |
| 9 | `frmReport` | `forms/frmReport.md` |

每个窗体：先摆控件（用规格里的控件表），再到 VBA 编辑器粘贴该窗体的「事件过程」代码。

## 第 5 步 · 建报表（可选但推荐）

参照 `reports/reports-spec.md` 创建 `rptR01 ~ rptR15`，记录源设为 `tmp_R01 ~ tmp_R15`，控件 `ControlSource` 等于各报表 `key` 字段。

## 第 6 步 · 链接表（通道 A，可选）

参照 `sql/linked-tables.md`：配置 DSN `SHG-MySQL`，运行 `LinkAllTables`，再按 `sql/access-queries.sql` 建查询。

## 第 7 步 · 设置启动项

文件 → 选项 → 当前数据库：
- **显示窗体**：`frmLogin`
- 取消勾选「显示导航窗格」与「使用 Access 特殊键」（成品化）
- 应用标题：`晓镜书阁 · 书店进销存（Access 客户端）`

## 第 8 步 · 冒烟测试

1. 打开数据库 → 弹出 `frmLogin` → 输入 `admin / 123456` → 进入 `frmMain`。
2. 主控台 KPI 应显示库存概览（非 0 或非报错）。
3. `frmScanQuery`：手动输入一个已存在 ISBN → 应显示书名/库存/二维码。
4. `frmCheck`：选一个盘点单 → 扫码录入 → 完成盘点。
5. `frmReport`：选 R07 → 预览 → 应打开报表窗口。

## 第 9 步 · 打包与部署

- **.accde**：文件 → 另存为 → 将数据库另存为 → **Make ACCDE**（隐藏 VBA 源码）。
  > 含 DAO/无 VBA 引用的库均可生成 .accde；若报错通常是缺少编译或存在编译错误。
- **运行时**：终端机可只安装 **Access Runtime**（免费），直接运行 .accde，无需完整 Office。
- **分发**：`晓镜书阁客户端.accde` + `sql/` 说明；客户端内置 DSN 提示，首启若失败会提示检查后端。

## 常见错误速查

| 报错 | 处理 |
|---|---|
| `用户定义类型未定义` | 缺 DAO 引用：工具 → 引用勾选 Access 数据库引擎对象库 |
| `找不到 DSN` | DSN 位数不匹配 / 未创建；改 32 位驱动或换 Office 位数 |
| `网络请求失败` | 后端未启动或 `API_BASE` 不对；在 `modApi` 顶部改地址 |
| 中文乱码 | DSN 用 Unicode 驱动 + utf8mb4；VBA 用 `Utf8Bytes/Utf8String`（已内置） |
| 报表打开空白 | 未先 `BuildReportTable` 或报表记录源未指向 `tmp_<code>` |