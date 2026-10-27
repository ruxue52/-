# 晓镜书阁 · Access 报表规格（R01 ~ R15）

> 与 [09-接口设计 §9](../../01-docs/09-接口设计.md) 的 15 张报表一一对应，
> 数据来源优先为 `GET /api/report/{code}`（列/行/汇总由后端给出），
> 也给出「链接表直连」的可选方案（Access 报表直接绑定本地查询）。
>
> **统一机制**：`modReportBuild.BuildReportTable(code, query)` 取回
> `{title, columns:[{key,label}], rows:[], summary:{}}` 后，按 `columns[].key`
> 动态创建临时表 `tmp_<code>`（含附加列 `RptTitle`），再打开报表对象 `rpt<code>`
> （记录源 = `tmp_<code>`）。**报表控件的 ControlSource 必须等于下表 `key` 列。**

## 0. 报表索引与绑定

| code | 报表对象 | 记录源 | 版式 | 页 | 打印 |
|---|---|---|---|---|---|
| R01 | `rptR01` | `tmp_R01` | 明细表 | A4 纵 | 小票/凭证可选 |
| R02 | `rptR02` | `tmp_R02` | 分组汇总 | A4 纵 | — |
| R03 | `rptR03` | `tmp_R03` | 明细（排行榜） | A4 纵 | — |
| R04 | `rptR04` | `tmp_R04` | 分组汇总 | A4 纵 | — |
| R05 | `rptR05` | `tmp_R05` | 明细表 | A4 横 | — |
| R06 | `rptR06` | `tmp_R06` | 分组汇总 | A4 纵 | — |
| R07 | `rptR07` | `tmp_R07` | 明细+分组 | A4 横 | A4 |
| R08 | `rptR08` | `tmp_R08` | 明细（高亮） | A4 纵 | A4 |
| R09 | `rptR09` | `tmp_R09` | 分组汇总 | A4 纵 | — |
| R10 | `rptR10` | `tmp_R10` | 明细+分组 | A4 横 | — |
| R11 | `rptR11` | `tmp_R11` | 分组汇总 | A4 纵 | — |
| R12 | `rptR12` | `tmp_R12` | 明细表 | A4 纵 | — |
| R13 | `rptR13` | `tmp_R13` | 明细表 | A4 纵 | — |
| R14 | `rptR14` | `tmp_R14` | 明细表 | A4 横 | — |
| R15 | `rptR15` | `tmp_R15` | 分组汇总 | A4 横 | — |

**通用页面设置**：页边距 上/下 1.5cm、左/右 1.5cm；页眉含「晓镜书阁 + 报表名 + `=gRptTitle` 或 `[RptTitle]`」、打印日期 `=Now()`；页脚含页码 `="第 " & [Page] & " / " & [Pages] & " 页"`。字体统一「微软雅黑 9pt」，表头加粗。

---

## R01 · 销售日报表

- **接口**：`GET /report/R01?date=yyyy-MM-dd`
- **链接表直连 SQL**（Access 方言）：
  ```sql
  SELECT o.order_no, o.out_time, i.book_name, i.isbn, i.quantity, i.price, i.amount,
         i.cost_price, o.payment_type, o.operator_name
  FROM psi_stock_out_order AS o INNER JOIN psi_stock_out_item AS i ON i.order_id = o.id
  WHERE o.status='OUTSTOCK' AND DateValue(o.create_time) = #2026-10-12#
  ORDER BY o.out_time;
  ```
- **列（key → 标签）**：`orderNo`单号、`outTime`出库时间、`bookName`书名、`isbn`ISBN、`quantity`数量、`price`单价、`amount`金额、`costPrice`成本单价、`paymentType`支付方式、`operatorName`收银员
- **分组/汇总**：按 `orderNo` 分组（组头显示单号/支付/收银员）；报表页脚汇总 单据数 `=Count([orderNo])`、总数量 `=Sum([quantity])`、销售总额 `=Sum([amount])`、总成本 `=Sum([quantity]*[costPrice])`、毛利额 `=Sum([amount])-Sum([quantity]*[costPrice])`
- **打印**：A4 纵；可切换小票版（`frmSaleOut` 结算后调用）

## R02 · 销售月报表

- **接口**：`GET /report/R02?month=2026-09`
- **列**：`date`日期、`orders`单数、`qty`销量、`amount`销售额、`cost`销售成本、`profit`毛利、`profitRate`毛利率(%)
- **分组/汇总**：无分组；报表页脚合计 单数/销量/销售额/成本/毛利，综合毛利率 `=IIf(Sum([amount])=0,0,Sum([profit])/Sum([amount])*100)`
- **打印**：A4 纵

## R03 · 商品销售排行榜

- **接口**：`GET /report/R03?startDate=&endDate=&limit=10`
- **列**：`rank`排名、`bookName`书名、`isbn`ISBN、`categoryName`分类、`qty`销量、`amount`销售额、`profit`毛利
- **分组/汇总**：无；页脚 TopN 合计销量/销售额
- **打印**：A4 纵

## R04 · 分类销售汇总表

- **接口**：`GET /report/R04?startDate=&endDate=`
- **列**：`categoryName`分类名称、`qty`销售数量、`amount`销售额、`ratio`占比(%)、`profit`毛利
- **分组/汇总**：按 `categoryName` 排序（可不分组）；页脚合计，占比按 `=Sum([amount])` 计算
- **打印**：A4 纵

## R05 · 采购到货明细表

- **接口**：`GET /report/R05?startDate=&endDate=&supplierId=`
- **列**：`orderNo`单号、`inTime`到货时间、`supplierName`供应商、`warehouseName`仓库、`bookName`书名、`isbn`ISBN、`quantity`数量、`price`单价、`amount`金额、`settleType`结算方式、`paidAmount`已付金额
- **分组/汇总**：按 `supplierName` 分组；页脚 单据数/采购数量/采购金额/已付金额
- **打印**：A4 横（列多）

## R06 · 供应商采购汇总表

- **接口**：`GET /report/R06?startDate=&endDate=`
- **列**：`supplierName`供应商、`orders`单数、`amount`采购金额、`paid`已付金额、`unpaid`未付金额、`ratio`占比(%)
- **分组/汇总**：无分组；页脚合计
- **打印**：A4 纵

## R07 · 库存结存表

- **接口**：`GET /report/R07?warehouseId=&categoryId=`
- **列**：`warehouseName`仓库、`categoryName`分类、`bookName`书名、`isbn`ISBN、`shelfNo`货架位、`quantity`数量、`safetyStock`安全库存、`avgCost`移动加权成本、`stockAmount`库存金额
- **分组/汇总**：按仓库、分类两级分组；组尾小计，页脚 SKU 数 `=Count(*)`、总数量、总金额
- **打印**：A4 横

## R08 · 库存预警表

- **接口**：`GET /report/R08?warehouseId=`
- **列**：`warehouseName`仓库、`bookName`书名、`isbn`ISBN、`quantity`当前数量、`safetyStock`安全库存、`gapQty`缺口数量、`suggestQty`建议补货量
- **条件格式**：`gapQty>0` 行字体红色（FormatCondition）
- **分组/汇总**：页脚 预警 SKU 数、缺口总量
- **打印**：A4 纵

## R09 · 库龄分析表

- **接口**：`GET /report/R09?warehouseId=`
- **列**：`range`库龄区间、`skuCount`SKU 数、`quantity`库存数量、`amount`库存金额、`ratio`占比(%)
- **分组/汇总**：页脚合计
- **打印**：A4 纵

## R10 · 盘点差异表

- **接口**：`GET /report/R10?checkNo=&startDate=&endDate=`
- **列**：`checkNo`盘点单号、`warehouseName`仓库、`checkType`盘点类型、`bookName`书名、`isbn`ISBN、`bookQty`账面数量、`realQty`实盘数量、`diffQty`差异数量、`diffAmount`差异金额
- **分组/汇总**：按 `checkNo` 分组；页脚 盘点品种数/差异品种数/盘盈数量/盘亏数量/差异金额合计
- **打印**：A4 横

## R11 · 商品毛利分析表

- **接口**：`GET /report/R11?startDate=&endDate=`
- **列**：`bookName`书名、`isbn`ISBN、`qty`销量、`amount`销售额、`cost`成本、`profit`毛利、`profitRate`毛利率(%)
- **分组/汇总**：页脚合计销售额/成本/毛利 + 综合毛利率
- **打印**：A4 纵（限店长及以上）

## R12 · 客户消费明细表

- **接口**：`GET /report/R12?customerId=&startDate=&endDate=`
- **列**：`orderNo`单号、`createTime`消费时间、`bookName`书名、`quantity`数量、`amount`金额、`paymentType`支付方式、`points`累计积分
- **分组/汇总**：页脚 消费单数/消费金额/获得积分
- **打印**：A4 纵

## R13 · 会员积分报表

- **接口**：`GET /report/R13?levelId=`
- **列**：`customerCode`客户编码、`customerName`姓名、`phone`手机、`levelName`会员等级、`discount`折扣率、`points`积分、`balance`余额
- **分组/汇总**：按等级分组；页脚 会员数/积分合计/余额合计
- **打印**：A4 纵

## R14 · 库存流水台账

- **接口**：`GET /report/R14?bookId=&warehouseId=&startDate=&endDate=`
- **列**：`createTime`时间、`bizType`业务类型、`bizNo`业务单号、`warehouseName`仓库、`bookName`书名、`isbn`ISBN、`direction`方向、`quantity`变动数量、`beforeQty`变动前、`afterQty`变动后、`unitCost`单位成本、`operatorName`操作人、`source`来源
- **分组/汇总**：按 `bizType` 分组；页脚 入库合计/出库合计/期末数量
- **打印**：A4 横

## R15 · 进销存汇总表

- **接口**：`GET /report/R15?month=&warehouseId=`
- **列**：`warehouseName`仓库、`opening`期初数量、`purchaseIn`进货入库、`saleOut`销售出库、`transferNet`调拨净额、`checkAdj`盘点调整、`returnNet`退货净额、`closing`期末数量
- **分组/汇总**：页脚各列合计；期末校验 `closing = opening+purchaseIn-saleOut+transferNet+checkAdj+returnNet`
- **打印**：A4 横

---

## 附 A · 报表控件命名规范

- 显示控件统一用 Text Box（`Can Grow = Yes`），控件名建议 `col_<key>`，`ControlSource` 填写该报表的字段名（=`key`），标题放组头/页头 Label。
- 汇总用 `Report Footer` 中 Text Box，表达式如 `=Sum([quantity])`。

## 附 B · 链接表直连（可选方案）

若采取「链接表直连」而非 /report 接口取数，则报表记录源可为本地 **查询（QueryDef）**，例如：

```
qryR07_库存结存：
SELECT w.warehouse_name, c.category_name, b.book_name, b.isbn, b.shelf_no,
       s.quantity, s.safety_stock, s.avg_cost,
       (s.quantity * s.avg_cost) AS stock_amount
FROM ((psi_stock AS s
  INNER JOIN base_book AS b ON b.id = s.book_id)
  LEFT JOIN base_category AS c ON c.id = b.category_id)
  LEFT JOIN base_warehouse AS w ON w.id = s.warehouse_id
ORDER BY w.warehouse_name, c.category_name, b.book_name;
```

> Access 直连注意：多表 JOIN 需加括号；`IFNULL/NULLIF` 用 `Nz()`/`IIf()` 替代；日期用 `#yyyy-mm-dd#`；通配符用 `*`（配合 `Like`）。详见 `sql/access-queries.sql` 与 `sql/linked-tables.md`。

## 附 C · 导出

报表导出建议两种途径：① `frmReport` 的「导出 Excel/CSV」按钮调用 `ExportReport`（后端生成，列序与 Web 一致）；② Access 原生 `DoCmd.OutputTo acOutputReport, "rptR07", acFormatPDF, …` 导出 PDF。二者按需选用。