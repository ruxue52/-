-- =====================================================================
--  晓镜书阁 · Access 客户端 · 直连查询集（access-queries.sql）
--  用途：将下列 SQL 保存为 Access 查询（QueryDef），供窗体/报表/图表直连取数。
--  说明：这些查询针对「链接表」（ODBC 直连 MySQL，见表名同 schema.sql），
--        使用 Access SQL 方言：日期 #yyyy-mm-dd#、通配符 *、Nz()、IIf()、
--        TOP 代替 LIMIT、多表 JOIN 需加括号。
--  与 MySQL 方言差异见文件末尾「方言对照」。
-- =====================================================================

-- ---------------------------------------------------------------------
-- Q1 库存结存查询（对应 R07 库存结存表）
-- ---------------------------------------------------------------------
SELECT w.warehouse_name, c.category_name, b.book_name, b.isbn, b.shelf_no,
       s.quantity, s.safety_stock, s.avg_cost,
       (s.quantity * s.avg_cost) AS stock_amount
FROM ((psi_stock AS s
   INNER JOIN base_book AS b ON b.id = s.book_id)
   LEFT JOIN base_category AS c ON c.id = b.category_id)
   LEFT JOIN base_warehouse AS w ON w.id = s.warehouse_id
ORDER BY w.warehouse_name, c.category_name, b.book_name;

-- ---------------------------------------------------------------------
-- Q2 低库存预警查询（对应 R08；quantity < safety_stock）
-- ---------------------------------------------------------------------
SELECT w.warehouse_name, b.book_name, b.isbn,
       s.quantity, s.safety_stock,
       (s.safety_stock - s.quantity) AS gap_qty
FROM (psi_stock AS s
   INNER JOIN base_book AS b ON b.id = s.book_id)
   LEFT JOIN base_warehouse AS w ON w.id = s.warehouse_id
WHERE s.quantity < s.safety_stock
ORDER BY (s.safety_stock - s.quantity) DESC;

-- ---------------------------------------------------------------------
-- Q3 销售日报（对应 R01；date 参数用 #...#，此处示例 2026-10-12）
--     建议在 Access 中改为参数查询：WHERE DateValue(o.create_time) = [请输入日期]
-- ---------------------------------------------------------------------
SELECT o.order_no, o.out_time, i.book_name, i.isbn, i.quantity, i.price,
       i.amount, i.cost_price, o.payment_type, o.operator_name
FROM psi_stock_out_order AS o
   INNER JOIN psi_stock_out_item AS i ON i.order_id = o.id
WHERE o.status = 'OUTSTOCK'
  AND DateValue(o.create_time) = #2026-10-12#
ORDER BY o.out_time;

-- ---------------------------------------------------------------------
-- Q4 销售日报汇总（单数/数量/销售额/成本/毛利）
-- ---------------------------------------------------------------------
SELECT COUNT(DISTINCT o.id) AS orders,
       Sum(i.quantity) AS qty,
       Sum(i.amount) AS amount,
       Sum(i.quantity * i.cost_price) AS cost,
       Sum((i.price - i.cost_price) * i.quantity) AS profit
FROM psi_stock_out_order AS o
   INNER JOIN psi_stock_out_item AS i ON i.order_id = o.id
WHERE o.status = 'OUTSTOCK'
  AND DateValue(o.create_time) = #2026-10-12#;

-- ---------------------------------------------------------------------
-- Q5 商品销售排行榜 TOP10（对应 R03；Access 用 TOP 代替 LIMIT）
-- ---------------------------------------------------------------------
SELECT TOP 10 i.book_name, i.isbn,
       Sum(i.quantity) AS qty, Sum(i.amount) AS amount,
       Sum((i.price - i.cost_price) * i.quantity) AS profit
FROM psi_stock_out_item AS i
   INNER JOIN psi_stock_out_order AS o ON o.id = i.order_id
WHERE o.status = 'OUTSTOCK'
  AND o.create_time Between #2026-09-01# And #2026-09-30 23:59:59#
GROUP BY i.book_name, i.isbn
ORDER BY Sum(i.amount) DESC;

-- ---------------------------------------------------------------------
-- Q6 库存流水台账（按图书/仓库/期间，对应 R14）
--     LIKE 通配符用 * 而非 %；此处用可选参数示例
-- ---------------------------------------------------------------------
SELECT f.create_time, f.biz_type, f.biz_no, w.warehouse_name,
       b.book_name, b.isbn, f.direction, f.quantity,
       f.before_qty, f.after_qty, f.unit_cost, f.operator_name, f.source
FROM (psi_stock_flow AS f
   INNER JOIN base_book AS b ON b.id = f.book_id)
   LEFT JOIN base_warehouse AS w ON w.id = f.warehouse_id
WHERE f.create_time Between #2026-10-01# And #2026-10-31 23:59:59#
ORDER BY f.create_time;

-- ---------------------------------------------------------------------
-- Q7 图书检索（按 ISBN 精确 / 书名模糊；Access 通配符 *）
--     扫码枪扫描 ISBN 后可直接查此查询
-- ---------------------------------------------------------------------
SELECT b.id, b.isbn, b.book_name, b.author, b.shelf_no,
       b.price, b.sale_price, p.publisher_name, c.category_name,
       Nz(s.quantity, 0) AS quantity, Nz(s.safety_stock, 0) AS safety_stock,
       s.avg_cost, w.warehouse_name
FROM (((base_book AS b
   LEFT JOIN base_publisher AS p ON p.id = b.publisher_id)
   LEFT JOIN base_category  AS c ON c.id = b.category_id)
   LEFT JOIN psi_stock AS s ON s.book_id = b.id)
   LEFT JOIN base_warehouse AS w ON w.id = s.warehouse_id
WHERE b.isbn = [请输入 ISBN]
   Or b.book_name Like '*' & [请输入关键字] & '*';

-- ---------------------------------------------------------------------
-- Q8 盘点差异明细（对应 R10）
-- ---------------------------------------------------------------------
SELECT o.check_no, w.warehouse_name, o.check_type,
       i.book_name, i.isbn, i.book_qty, i.real_qty, i.diff_qty, i.diff_amount
FROM (psi_stock_check_order AS o
   INNER JOIN psi_stock_check_item AS i ON i.order_id = o.id)
   LEFT JOIN base_warehouse AS w ON w.id = o.warehouse_id
WHERE o.status = 'FINISHED'
ORDER BY o.check_no, i.diff_qty DESC;

-- ---------------------------------------------------------------------
-- Q9 进销存汇总（按月/仓库，对应 R15；用 IIf() 代替 CASE WHEN）
-- ---------------------------------------------------------------------
SELECT w.warehouse_name,
       Sum(IIf(f.biz_type='INIT', f.quantity * f.direction, 0)) AS opening,
       Sum(IIf(f.biz_type='IN',   f.quantity, 0))              AS purchase_in,
       Sum(IIf(f.biz_type='OUT',  f.quantity, 0))              AS sale_out,
       Sum(IIf(f.biz_type IN ('TRANSFER_IN','TRANSFER_OUT'), f.quantity * f.direction, 0)) AS transfer_net,
       Sum(IIf(f.biz_type='CHECK', f.quantity * f.direction, 0)) AS check_adj,
       Sum(IIf(f.biz_type IN ('RET_PUR','RET_SAL'), f.quantity * f.direction, 0)) AS return_net,
       Sum(f.quantity * f.direction) AS closing
FROM psi_stock_flow AS f
   LEFT JOIN base_warehouse AS w ON w.id = f.warehouse_id
WHERE Format(f.create_time, 'yyyy-mm') = '2026-10'
GROUP BY w.warehouse_name;

-- ---------------------------------------------------------------------
-- Q10 会员积分明细（对应 R13）
-- ---------------------------------------------------------------------
SELECT c.customer_code, c.customer_name, c.phone,
       l.level_name, l.discount, c.points, c.balance
FROM base_customer AS c
   LEFT JOIN base_member_level AS l ON l.id = c.level_id
WHERE c.customer_type = 'MEMBER'
ORDER BY c.points DESC;

-- =====================================================================
--  方言对照：Access SQL  vs  MySQL
-- =====================================================================
--  日期常量        #2026-10-12#            '2026-10-12' / DATE(...)
--  通配符          * 和 ?  (配合 LIKE)     % 和 _  (配合 LIKE)
--  取前 N 行       SELECT TOP 10 ...       SELECT ... LIMIT 10
--  空值替换        Nz(x, 0)                IFNULL(x, 0)
--  条件表达式      IIf(cond, a, b)         CASE WHEN ... THEN ... END
--  字符串拼接      (a & b) 或 a+b          CONCAT(a, b)
--  日期格式化      Format(d, 'yyyy-mm')    DATE_FORMAT(d, '%Y-%m')
--  取日期部分      DateValue(d) / Year()   DATE(d) / YEAR()
--  除零保护        IIf(x=0, 0, a/x)        NULLIF(x,0)
--  多表 JOIN 语法  ((a JOIN b) JOIN c)     a JOIN b JOIN c （括号可选）
--  分页            TOP N + 子查询           LIMIT offset, size
--  标识符引用      [field name] 或 `…`      `field`
-- =====================================================================