# 02-database · 数据库说明（初始化与信创迁移指南）

> 本目录存放「晓镜书阁 · 书店进销存管理系统」的数据库脚本与信创适配资料。

## 1. 目录结构

```
02-database/
├── schema.sql                  MySQL 8 建表脚本（30 张表，权威结构）
├── data.sql                    MySQL 8 初始化数据（角色/用户/权限/40 图书/样例单据等）
├── README.md                   本文件
└── dm/                         达梦 DM8 信创适配
    ├── dm-schema.sql           达梦 DM8 建表脚本（含触发器）
    └── dm-adaptation-notes.md  MySQL → 达梦迁移说明
```

## 2. 数据库概览

- **数据库名**：`bookstore`（达梦/openGauss 可自定义，如 `BOOKSTORE`）
- **字符集**：`utf8mb4`（达梦/openGauss 使用 UTF-8）
- **表数量**：30 张，按域划分：
  - `sys_*` 系统管理域（8）：角色、用户、权限、关联、日志、字典
  - `base_*` 基础数据域（7）：分类、出版社、供应商、会员等级、客户、仓库、图书
  - `psi_*` 进销存域（15）：库存、进货、出货、盘点、调拨、退货、库存流水
- **金额/数量/成本精度**：`DECIMAL(14,2)` / `DECIMAL(14,3)` / `DECIMAL(14,4)`

详细字段设计见 [../01-docs/08-数据库设计.md](../01-docs/08-数据库设计.md)；ER 图见 [../01-docs/03-UML图集.md](../01-docs/03-UML图集.md)。

## 3. MySQL 8 初始化（主库）

```sql
-- 1) 创建数据库
CREATE DATABASE bookstore DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 2) 执行结构脚本
--    在 bookstore 库中执行 schema.sql
source /path/to/02-database/schema.sql;

-- 3) 执行初始化数据
source /path/to/02-database/data.sql;
```

命令行方式：

```bash
mysql -uroot -p -e "CREATE DATABASE bookstore DEFAULT CHARACTER SET utf8mb4;"
mysql -uroot -p bookstore < 02-database/schema.sql
mysql -uroot -p bookstore < 02-database/data.sql
```

**初始化数据要点**：

| 数据 | 说明 |
|---|---|
| 角色 | `ADMIN/MANAGER/PURCHASER/SALES/STOCK` |
| 用户 | admin/manager/buyer01/cashier01/stock01/sales02，初始密码均为 `123456` |
| 权限 | 26 条菜单级权限，`ADMIN` 拥有全部权限 |
| 图书 | 40 本（含 ISBN、定价、进价、售价、货架位） |
| 期初库存 | 中心仓 40 种 + 门店仓 6 种 |
| 样例单据 | 进货单（PI）、出货单（SO）、盘点单（CK）、退货单（RT） |
| 二维码 | `SHG:BOOK:<isbn>` / `SHG:ORDER:<orderNo>` 记录 |

## 4. 达梦 DM8 信创初始化

> 前置：安装达梦 DM8（默认端口 5236），准备 `DmJdbcDriver18.jar`。

```sql
-- 1) 在 DM 中创建用户/模式（示例）
--    使用达梦管理工具或 disql 执行
CREATE USER BOOKSTORE IDENTIFIED BY "Bookstore@123";
GRANT DBA TO BOOKSTORE;

-- 2) 以 BOOKSTORE 用户登录后执行建表脚本
--    执行 02-database/dm/dm-schema.sql
```

数据初始化：可将 `data.sql` 按达梦语法导入（注意自增用 `SET IDENTITY_INSERT`），或使用达梦 DTS 工具从 MySQL 迁移。

命令参考：

```bash
# 使用达梦 disql
disql BOOKSTORE/Bookstore@123@localhost:5236
SQL> start /path/to/dm/dm-schema.sql
```

## 5. openGauss 初始化（信创备选）

- 基于 `dm/dm-schema.sql` 微调：`IDENTITY` → `SERIAL`/`IDENTITY`，`SYSDATE` → `NOW()`，`NVL` 可保留（openGauss 支持 `COALESCE`）。
- 使用 `gsql` 执行建表脚本，导入数据。
- 应用侧分页使用 `LIMIT n OFFSET m`。

## 6. 应用连接配置（后端）

```yaml
# application.yml 片段
spring:
  datasource:
    # MySQL
    url: jdbc:mysql://127.0.0.1:3306/bookstore?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    driver-class-name: com.mysql.cj.jdbc.Driver
    # 达梦：url: jdbc:dm://127.0.0.1:5236/BOOKSTORE
    #       driver-class-name: dm.jdbc.driver.DmDriver
    username: root
    password: ******
```

MyBatis-Plus 分页方言：MySQL 用 `DbType.MYSQL`，达梦用 `DbType.DM`，openGauss 用 `DbType.POSTGRE_SQL`。

## 7. 单号与编码规则（与业务一致）

| 单据 | 前缀 | 示例 |
|---|---|---|
| 进货单 | `PI` | `PI20260901001` |
| 出货单 | `SO` | `SO20260905001` |
| 盘点单 | `CK` | `CK20260930001` |
| 调拨单 | `TR` | `TR20260920001` |
| 退货单 | `RT` | `RT20260918001` |

二维码内容：图书 `SHG:BOOK:<isbn>`、单据 `SHG:ORDER:<orderNo>`、库存位 `SHG:STOCK:<warehouseId>:<isbn>`、盘点 `SHG:CHECK:<checkNo>:<isbn>`。

## 8. 常见问题

| 问题 | 处理 |
|---|---|
| 中文乱码 | 确认库字符集 UTF-8；达梦可改用 `VARCHAR(n CHAR)` |
| 自增主键冲突（迁移后） | 重设达梦 `IDENTITY` 种子 |
| `update_time` 不更新 | 启用 `dm-schema.sql` 触发器或应用层填充 |
| 统计 SQL 报错（达梦） | 替换 `IFNULL→NVL`、`DATE_FORMAT→TO_CHAR` 等，见 `dm/dm-adaptation-notes.md` |
| 分页结果异常 | 使用 MyBatis-Plus 分页插件并设置正确 `DbType` |

## 9. 相关文档

- [08-数据库设计](../01-docs/08-数据库设计.md)
- [10-信创适配方案](../01-docs/10-信创适配方案.md)
- [dm/dm-adaptation-notes.md](dm/dm-adaptation-notes.md)