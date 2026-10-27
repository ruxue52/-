# MySQL → 达梦 DM8 迁移说明

> 本文说明如何将 `02-database/schema.sql`（MySQL 8）迁移到达梦 DM8，涵盖类型映射、函数差异、分页差异、自增差异、注释/索引差异与注意事项，配合 `dm-schema.sql` 使用。

---

## 1. 类型映射表

| MySQL 类型 | 达梦 DM8 类型 | 说明 |
|---|---|---|
| `BIGINT` | `BIGINT` | 直接对应 |
| `INT` | `INT` | 直接对应 |
| `TINYINT` | `TINYINT` | DM 支持；若遇兼容问题可降为 `SMALLINT` |
| `VARCHAR(n)` | `VARCHAR(n)` | DM 默认按字节计长；如需按字符可写 `VARCHAR(n CHAR)` |
| `VARCHAR(2000)` 等 | `VARCHAR(2000)` | DM `VARCHAR` 上限 32767 字节 |
| `DECIMAL(14,2)` | `DECIMAL(14,2)` | 数值类型一致 |
| `DECIMAL(14,3)` | `DECIMAL(14,3)` | 数量 |
| `DECIMAL(14,4)` | `DECIMAL(14,4)` | 成本 |
| `DECIMAL(5,2)` | `DECIMAL(5,2)` | 折扣率/倍率 |
| `DATETIME` | `TIMESTAMP` | **主要差异**，见 §3 |
| `DATE` | `DATE` | — |
| `TEXT`（未使用） | `CLOB`/`TEXT` | 本项目未使用长文本类型 |

---

## 2. 自增差异

| 项 | MySQL | 达梦 DM8 |
|---|---|---|
| 语法 | `BIGINT NOT NULL AUTO_INCREMENT` | `BIGINT IDENTITY(1,1) NOT NULL` |
| 起始/步长 | `AUTO_INCREMENT=1`（表级） | `IDENTITY(起始,步长)` |
| 手动插值 | 直接 `INSERT(id,...)` | 需 `SET IDENTITY_INSERT <表> ON; ... OFF;` |

**数据迁移要点**：迁移含 `id` 的存量数据时，先关闭自增：`SET IDENTITY_INSERT psi_stock ON;`，插完再 `OFF`，并按最大 `id` 重设自增种子。

---

## 3. 时间与默认值差异

| 项 | MySQL | 达梦 DM8 | 迁移处理 |
|---|---|---|---|
| 类型 | `DATETIME` | `TIMESTAMP` | 建表改写 |
| 默认当前时间 | `DEFAULT CURRENT_TIMESTAMP` | `DEFAULT SYSDATE`（或 `CURRENT_TIMESTAMP`） | 改写默认值 |
| 自动更新 | `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | **不支持** | 用 `BEFORE UPDATE` **触发器**（见 `dm-schema.sql` 末尾）或应用层显式赋值 |
| 函数 | `NOW()` | `SYSDATE` / `NOW()`（兼容模式下可用） | 视场景替换 |

**建议**：优先在**应用层**（MyBatis-Plus `@TableField(fill = FieldFill.INSERT_UPDATE)`）维护 `update_time`，触发器作为兜底，避免触发器与 ORM 双写冲突。

---

## 4. 函数差异（SQL 适配层重点）

| 用途 | MySQL | 达梦 DM8 | 备注 |
|---|---|---|---|
| 空值替换 | `IFNULL(a,b)` | `NVL(a,b)` / `IFNULL`（兼容模式） | 统计 SQL 高频 |
| 日期格式化 | `DATE_FORMAT(t,'%Y-%m')` | `TO_CHAR(t,'YYYY-MM')` | 月度统计 |
| 字符串转日期 | `STR_TO_DATE` | `TO_DATE` | — |
| 日期加减 | `DATE_SUB(CURDATE(), INTERVAL n DAY)` | `SYSDATE - n` 或 `ADD_DAYS(SYSDATE,-n)` | 趋势统计 |
| 当前日期 | `CURDATE()` | `TRUNC(SYSDATE)` / `CURDATE()` | — |
| 日期差 | `DATEDIFF(a,b)` | `DATEDIFF` / `a-b` | 库龄分析 |
| 分组排序 | `FIELD(x,'a','b')` | `DECODE` / `CASE WHEN` | 库龄区间排序，建议改 `CASE` |
| 字符串拼接 | `CONCAT(a,b)` | `a || b` 或 `CONCAT` | — |
| 取整/四舍五入 | `ROUND(x,n)` | `ROUND(x,n)` | 一致 |
| 类型转换 | `CAST(x AS ...)` | `CAST(x AS ...)` | 一致 |
| 分组拼接 | `GROUP_CONCAT` | `WM_CONCAT` / `LISTAGG` | 如需 |
| 限制行数 | `LIMIT n` | `LIMIT n`（DM 支持） | 见 §5 |

**实施建议**：将统计/报表 SQL 中的函数调用抽到**方言适配层**（如 MyBatis 的 `<if test="dbType=='dm'">` 或独立 Mapper），避免在业务代码硬编码。

---

## 5. 分页差异

| 数据库 | 语法 |
|---|---|
| MySQL | `LIMIT #{offset}, #{size}` 或 `LIMIT #{size} OFFSET #{offset}` |
| 达梦 DM8 | `LIMIT #{size} OFFSET #{offset}` 或 `LIMIT #{offset}, #{size}`（视版本） |
| openGauss | `LIMIT #{size} OFFSET #{offset}` |

**处理**：使用 **MyBatis-Plus 分页插件**，配置 `DbType.DM`，由插件自动改写分页语句，业务代码无需关心差异。

```java
// MybatisPlusConfig
PaginationInnerInterceptor page = new PaginationInnerInterceptor(DbType.DM);
```

---

## 6. 注释与索引差异

| 项 | MySQL | 达梦 DM8 |
|---|---|---|
| 表注释 | `) COMMENT='...'` | `COMMENT ON TABLE t IS '...';` |
| 列注释 | `col TYPE COMMENT '...'` | `COMMENT ON COLUMN t.col IS '...';` |
| 唯一约束 | `UNIQUE KEY uk_x (col)` | `CONSTRAINT uk_x UNIQUE (col)` |
| 普通索引 | `KEY idx_x (col)` | `CREATE INDEX idx_x ON t(col);` |
| 组合主键 | `PRIMARY KEY (a,b)` | `CONSTRAINT pk_t PRIMARY KEY (a,b)` |
| 表/引擎选项 | `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4` | 由库级配置指定，表级无需声明 |

---

## 7. 其他注意事项

1. **字符集**：建库使用 UTF-8；如中文长度异常，将 `VARCHAR(n)` 调整为 `VARCHAR(n CHAR)`。
2. **保留字**：避免使用 DM 保留字作列名（本项目列名已规避）。
3. **大小写**：DM 标识符默认大小写敏感，统一使用**小写**命名。
4. **`UPDATE ... JOIN`**：DM 语法支持有限，多表更新建议拆分为子查询或多次单表更新。
5. **批量插入**：`INSERT INTO t (a,b) VALUES (...),(...);` DM 部分版本支持，如不支持改为多条 `INSERT` 或 `INSERT ALL`。
6. **自增回填**：迁移后校验各表 `id` 最大值并重设 `IDENTITY` 种子，防止主键冲突。
7. **事务隔离**：DM 默认隔离级别与 MySQL 略有差异，核对并发场景（库存扣减）行为一致。
8. **`SET FOREIGN_KEY_CHECKS`**：DM 无此语句，迁移脚本无需该设置（本项目物理脚本本就不建外键）。
9. **驱动**：使用 `DmJdbcDriver18.jar`，URL 形如 `jdbc:dm://host:5236/BOOKSTORE`。
10. **兼容模式**：DM 可选兼容模式（Oracle/MySQL），如启用 MySQL 兼容模式，部分函数（`IFNULL/DATE_FORMAT`）可直接使用，可简化适配。

---

## 8. 迁移检查表

| # | 检查项 | 期望 |
|---|---|---|
| 1 | 30 张表创建成功 | 无报错 |
| 2 | 主键/唯一约束创建 | 与 MySQL 一致 |
| 3 | 索引创建 | 高频查询索引齐全 |
| 4 | 默认值生效 | `create_time` 自动填充 |
| 5 | `update_time` 维护 | 触发器或应用层生效 |
| 6 | 存量数据导入 | 行数一致、抽样正确 |
| 7 | 自增种子 | 最大值之上续增 |
| 8 | 统计/报表 SQL | 结果与 MySQL 一致 |
| 9 | 分页 | 结果正确无重复遗漏 |
| 10 | 中文 | 无乱码 |

> 应用侧适配详见 [10-信创适配方案](../01-docs/10-信创适配方案.md)。