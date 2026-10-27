# 链接表（ODBC 直连 MySQL）搭建说明

> 「双通道」中的 **通道 A**：Access 通过 MySQL ODBC 8.x 驱动链接后端数据库的表，
> 用于基础数据浏览、库存结存、报表直连取数等离线/批量场景。
> 「通道 B」（登录与业务操作）见 `../README.md`，走 VBA 调 REST API。

## 1. 前置条件

| 项 | 要求 |
|---|---|
| 操作系统 | Windows 10/11（Access 仅 Windows） |
| Office | Access 2016 / 2019 / 2021 / Microsoft 365（32 位或 64 位） |
| ODBC 驱动 | **MySQL ODBC 8.0 Unicode Driver**（必须装与 Office **同位数**：Office 32 位装 32 位驱动，64 位装 64 位驱动） |
| MySQL | 8.x，数据库名 `bookstore`（见 `02-database/schema.sql`），字符集 `utf8mb4` |
| 网络 | 能访问 MySQL 主机（默认 3306） |

> **位数匹配是最常见踩坑点**：64 位驱动配 32 位 Office 会看不到 DSN。查看方式：
> Access → 文件 → 账户 → 关于 Access，确认「64 位」或「32 位」。

## 2. 安装 MySQL ODBC 驱动

1. 下载 `mysql-connector-odbc-8.0.x-winx64.msi`（64 位）或 `winx86`（32 位）：
   https://dev.mysql.com/downloads/connector/odbc/
2. 安装后，在「ODBC 数据源管理器」中确认：
   - 64 位：`控制面板 → 管理工具 → ODBC 数据源(64 位)`
   - 32 位：`C:\Windows\SysWOW64\odbcad32.exe`
3. 在「驱动程序」页应看到 `MySQL ODBC 8.0 Unicode Driver`。

## 3. 配置 DSN（用户数据源）

「ODBC 数据源管理器 → 用户 DSN → 添加 → MySQL ODBC 8.0 Unicode Driver」：

| 字段 | 值 |
|---|---|
| Data Source Name | `SHG-MySQL` |
| Description | 晓镜书阁进销存 MySQL |
| TCP/IP Server | `localhost`（或后端 MySQL 主机） |
| Port | `3306` |
| User | `root`（或实际账号） |
| Password | 你的 MySQL 密码 |
| Database | `bookstore` |
| Character Set | `utf8mb4` |
| SSL | 视部署而定（本地可 Disabled） |

点击「Test」应返回 `Connection Successful`。

> 也可跳过「用户 DSN」，在 Access 链接表时选择**机器数据源/文件 DSN**。
> 若希望脚本化创建，可用 `assets/import-odbc.reg`（导入注册表）后微调。

## 4. 在 Access 中创建链接表

### 方式一：界面操作
1. 打开 .accdb → **外部数据** 选项卡 → **新数据源 → 从其他源 → ODBC 数据库**。
2. 选择「通过创建链接表来链接到数据源」→ 选择 DSN `SHG-MySQL` → 输入账号密码。
3. 在表列表中 **Shift/Ctrl 多选** 需要的表 → 确定。
4. 链接表名默认为原名（如 `psi_stock`）。若想加前缀，可 `表名` 保持原名，便于 SQL 直连复用。

### 方式二：VBA 脚本批量链接（推荐，可复现）
在 Access 中新建模块粘贴下面过程，运行 `LinkAllTables`：

```vba
Option Compare Database
Option Explicit

' 需链接的业务表（可按需增删）
Private Function TableNames() As String
    TableNames = "sys_role,sys_user,base_category,base_publisher,base_supplier," & _
        "base_member_level,base_customer,base_warehouse,base_book," & _
        "psi_stock,psi_stock_in_order,psi_stock_in_item,psi_stock_out_order," & _
        "psi_stock_out_item,psi_stock_check_order,psi_stock_check_item," & _
        "psi_stock_transfer_order,psi_stock_transfer_item,psi_return_order," & _
        "psi_return_item,psi_stock_flow,sys_qrcode,sys_scan_log"
End Function

Public Sub LinkAllTables()
    Dim db As DAO.Database, td As DAO.TableDef, arr() As String, i As Long
    Set db = CurrentDb
    arr = Split(TableNames(), ",")
    For i = LBound(arr) To UBound(arr)
        ' 已存在则跳过
        On Error Resume Next
        db.TableDefs(arr(i)).Name = db.TableDefs(arr(i)).Name
        If Err.Number = 0 Then GoTo NextOne
        Err.Clear
        On Error GoTo EH
        Set td = db.CreateTableDef(arr(i))
        ' 注意：ODBC;DSN=<DSN名>;DATABASE=bookstore;
        td.Connect = "ODBC;DSN=SHG-MySQL;DATABASE=bookstore;"
        td.SourceTableName = arr(i)
        db.TableDefs.Append td
NextOne:
    Next i
    MsgBox "链接完成。", vbInformation
    Exit Sub
EH:
    MsgBox "链接 " & arr(i) & " 失败：" & Err.Description, vbExclamation
End Sub

' 取消全部链接表
Public Sub UnlinkAllTables()
    Dim db As DAO.Database, arr() As String, i As Long
    Set db = CurrentDb
    arr = Split(TableNames(), ",")
    For i = LBound(arr) To UBound(arr)
        On Error Resume Next
        db.TableDefs.Delete arr(i)
        Err.Clear
    Next i
    MsgBox "已断开全部链接表。", vbInformation
End Sub
```

> Access 再次打开时若要求输入密码：勾选「保存密码」（会在连接串里保存加密后的密码）；
> 生产环境建议用 `PWD=...` 由 DSN 携带，或改由登录时用 `td.Connect` 动态赋值。

## 5. 表结构与字段映射（对齐 schema.sql）

链接后表名保持 MySQL 原名（下划线风格），字段名亦为下划线风格（如 `book_name`、`quantity`、`safety_stock`）。
**注意与 REST API 字段命名的差异**：接口用驼峰（`bookName`），数据库用下划线（`book_name`）。
VBA 走 API 时用驼峰；SQL 直连时用下划线。映射示例：

| 业务 | REST 字段（API） | DB 字段（链接表） |
|---|---|---|
| 书名 | `bookName` | `book_name` |
| ISBN | `isbn` | `isbn` |
| 仓库名 | `warehouseName` | `warehouse_name` |
| 在库数量 | `quantity` | `quantity` |
| 安全库存 | `safetyStock` | `safety_stock` |
| 平均成本 | `avgCost` | `avg_cost` |
| 库存金额 | `stockAmount` | `quantity*avg_cost`（计算列） |
| 单据状态 | `status` | `status` |

## 6. 常见问题

| 现象 | 原因/处理 |
|---|---|
| 驱动列表为空 / 找不到 DSN | 位数不匹配：装与 Office 同位的 ODBC 驱动 |
| 中文乱码 | DSN 未设 `utf8mb4` 或驱动选成 ANSI 版；改用 **Unicode** 驱动并设 Character Set=utf8mb4 |
| 链接表只读、无法更新 | 这是**双通道**设计的取舍：业务写入一律走 REST API，链接表仅用于查询/报表；勿在 Access 直接改库 |
| 打开慢 | 大数据量表（`psi_stock_flow`）避免整表打开，用查询加条件 |
| 密码要求反复输入 | 链接时勾选「保存密码」；或把 PWD 放入 DSN |

## 7. 为什么写入不直连

业务写入（入库/出库/盘点/退货）必须由后端完成，因为它涉及**事务、移动加权成本、库存流水、积分累计、状态机校验**。
Access 直接 `UPDATE psi_stock` 会绕过这些逻辑，导致与 Web 端数据不一致。
因此：**读用链接表（快、轻），写走 REST API（一致、可控）**。